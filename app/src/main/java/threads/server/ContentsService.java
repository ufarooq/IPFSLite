package threads.server;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import threads.core.Network;
import threads.core.Preferences;
import threads.core.Singleton;
import threads.core.THREADS;
import threads.core.api.Status;
import threads.core.api.Thread;
import threads.core.api.User;
import threads.ipfs.IPFS;
import threads.ipfs.api.CID;
import threads.ipfs.api.Multihash;
import threads.ipfs.api.PID;

import static androidx.core.util.Preconditions.checkNotNull;

class ContentsService {

    private static final String TAG = ContentsService.class.getSimpleName();


    static void contents(@NonNull Context context) {
        checkNotNull(context);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                Service.getInstance(context);

                if (Network.isConnected(context)) {
                    Log.e(TAG, "Run contents service");

                    downloadContents(context);
                }
            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }

        });
    }


    private static void downloadContents(@NonNull Context context) {
        checkNotNull(context);

        final ContentService contentService = ContentService.getInstance(context);
        final THREADS threads = Singleton.getInstance(context).getThreads();
        final IPFS ipfs = Singleton.getInstance(context).getIpfs();
        final PID host = Preferences.getPID(context);

        try {
            checkNotNull(ipfs, "IPFS not valid");
            for (PID user : threads.getUsersPIDs()) {

                if (user.equals(host)) {
                    continue;
                }

                if (!threads.isUserBlocked(user)) {

                    long timestamp = getMinutesAgo(10);

                    List<Content> contents = contentService.getContentDatabase().
                            contentDao().getContents(user, timestamp, false);

                    if (!contents.isEmpty()) {

                        SwarmService.ConnectInfo info = SwarmService.connect(context, user);

                        if (info.isConnected()) {
                            for (threads.server.Content entry : contents) {
                                ContentsService.download(context, entry.getPid(), entry.getCID());
                            }
                        }

                        SwarmService.disconnect(context, info);
                    }
                }
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    private static long getMinutesAgo(int minutes) {
        return System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(minutes);
    }

    private static void downloadContents(@NonNull Context context,
                                         @NonNull PID pid,
                                         @NonNull Contents files) {
        for (ContentEntry file : files) {

            String cidStr = file.getCid();
            try {
                Multihash.fromBase58(cidStr);
            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
                continue;
            }

            createSkeleton(context, pid,
                    CID.create(cidStr), file.getFilename(),
                    file.getSize(), file.getMimeType(), file.getImage());


        }

        if (Service.isAutoDownload(context)) {
            Collections.reverse(files);
            for (ContentEntry file : files) {
                JobServiceDownload.downloadContentID(context, pid, CID.create(file.getCid()));
            }
        }

    }

    private static void createSkeleton(
            @NonNull Context context,
            @NonNull PID sender,
            @NonNull CID cid,
            @Nullable String filename,
            @Nullable String filesize,
            @Nullable String mimeType,
            @Nullable String image) {

        checkNotNull(context);
        checkNotNull(sender);
        checkNotNull(cid);


        final THREADS threads = Singleton.getInstance(context).getThreads();
        try {

            User user = threads.getUserByPID(sender);
            if (user == null) {
                Preferences.error(threads, context.getString(R.string.unknown_peer_sends_data));
                return;
            }

            CID thumbnail = null;

            if (image != null) {
                thumbnail = downloadImage(context, image);
            }

            createThread(context, threads, user, cid, filename, filesize, mimeType, thumbnail);


        } catch (Throwable e) {
            Preferences.evaluateException(threads, Preferences.EXCEPTION, e);
        }


    }

    private static synchronized void createThread(
            @NonNull Context context,
            @NonNull THREADS threads,
            @NonNull User user,
            @NonNull CID cid,
            @Nullable String filename,
            @Nullable String filesize,
            @Nullable String mimeType,
            @Nullable CID thumbnail) {


        final IPFS ipfs = Singleton.getInstance(context).getIpfs();
        checkNotNull(ipfs, "IPFS not valid");
        List<Thread> entries = threads.getThreadsByCIDAndThread(cid, 0L);

        if (entries.isEmpty()) {

            Service.createThread(context, ipfs, user, cid,
                    Status.ERROR, filename, filesize, mimeType, thumbnail);

        }

    }

    @Nullable
    private static CID downloadImage(@NonNull Context context,
                                     @NonNull String image) {

        final IPFS ipfs = Singleton.getInstance(context).getIpfs();
        final int timeout = Preferences.getConnectionTimeout(context);

        if (ipfs != null) {
            try {
                try {
                    Multihash.fromBase58(image);
                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
                    return null;
                }

                CID cid = CID.create(image);
                byte[] data = ipfs.getData(cid, timeout, false);
                if (data != null) {
                    return cid;
                }

            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }
        }
        return null;
    }

    private static Contents downloadContents(@NonNull Context context,
                                             @NonNull CID cid) {
        final Gson gson = new Gson();
        final IPFS ipfs = Singleton.getInstance(context).getIpfs();
        final int timeout = Preferences.getConnectionTimeout(context);

        if (ipfs != null) {
            try {

                String content = ipfs.getText(cid, "", timeout, false);

                if (content != null) {

                    return gson.fromJson(content, Contents.class);
                }

            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }
        }
        return null;
    }

    public static boolean download(@NonNull Context context, @NonNull PID pid, @NonNull CID cid) {
        checkNotNull(context);
        checkNotNull(pid);
        checkNotNull(cid);

        final ContentService contentService = ContentService.getInstance(context);
        final THREADS threads = Singleton.getInstance(context).getThreads();

        boolean success = false;
        try {

            if (threads.existsUser(pid)) {

                if (!threads.isUserBlocked(pid)) {

                    final IPFS ipfs = Singleton.getInstance(context).getIpfs();
                    checkNotNull(ipfs, "IPFS not valid");

                    SwarmService.ConnectInfo info = SwarmService.connect(context, pid);
                    success = info.isConnected();


                    if (success) {
                        Contents contents = downloadContents(context, cid);

                        if (contents != null) {

                            contentService.finishContent(cid);

                            downloadContents(context, pid, contents);
                        }
                    }

                    SwarmService.disconnect(context, info);

                }
            } else {
                // create a new unknown user
                Service.createUnknownUser(context, pid);

            }

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
        return success;

    }
}
