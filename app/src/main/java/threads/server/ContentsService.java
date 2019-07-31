package threads.server;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import threads.core.GatewayService;
import threads.core.IdentityService;
import threads.core.Network;
import threads.core.Preferences;
import threads.core.Singleton;
import threads.core.THREADS;
import threads.core.api.Thread;
import threads.core.api.ThreadStatus;
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
        final boolean peerDiscovery = Service.isSupportPeerDiscovery(context);
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

                        boolean success = IdentityService.connectPeer(
                                context, user, BuildConfig.ApiAesKey, Service.PROTOCOL,
                                peerDiscovery, true, true);

                        if (success) {
                            for (threads.server.Content entry : contents) {
                                ContentsService.download(context, entry.getPid(), entry.getCID());
                            }
                        }
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
            if (files.size() < 50) {
                for (ContentEntry file : files) {
                    JobServiceMultihash.download(context, pid, CID.create(file.getCid()));
                }
            } else {
                for (ContentEntry file : files) {
                    JobServiceMultihash.downloadMultihash(context, pid, CID.create(file.getCid()));
                }
            }
        }

    }

    private static void createSkeleton(@NonNull Context context,
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

        final IPFS ipfs = Singleton.getInstance(context).getIpfs();
        if (ipfs != null) {

            try {

                User user = threads.getUserByPID(sender);
                if (user == null) {
                    Preferences.error(threads, context.getString(R.string.unknown_peer_sends_data));
                    return;
                }


                List<Thread> entries = threads.getThreadsByCIDAndThread(cid, 0L);

                if (entries.isEmpty()) {

                    CID thumbnail = null;

                    if (image != null) {
                        thumbnail = downloadImage(context, image);
                    }

                    Service.createThread(context, ipfs, user, cid,
                            ThreadStatus.ERROR, filename, filesize, mimeType, thumbnail);


                    Preferences.event(threads, Preferences.THREAD_SCROLL_EVENT, "");
                }

            } catch (Throwable e) {
                Preferences.evaluateException(threads, Preferences.EXCEPTION, e);
            }

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
                byte[] content = ipfs.get(cid, "", timeout, false);

                if (content.length > 0) {
                    ipfs.pin_add(cid, timeout, true);
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

                byte[] content = ipfs.get(cid, "", timeout, false);

                if (content.length > 0) {

                    String contentAsString = new String(content);

                    return gson.fromJson(contentAsString, Contents.class);
                }

            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }
        }
        return null;
    }

    public static boolean download(@NonNull Context context,
                                   @NonNull PID pid,
                                   @NonNull CID cid) {
        checkNotNull(context);
        checkNotNull(pid);
        checkNotNull(cid);

        final ContentService contentService = ContentService.getInstance(context);
        final THREADS threads = Singleton.getInstance(context).getThreads();

        boolean success = false;
        try {

            if (threads.existsUser(pid)) {

                if (!threads.isUserBlocked(pid)) {
                    final int timeout = Preferences.getConnectionTimeout(context);

                    final IPFS ipfs = Singleton.getInstance(context).getIpfs();
                    checkNotNull(ipfs, "IPFS not valid");

                    if (!ipfs.isConnected(pid)) {
                        success = ipfs.swarmConnect(pid, timeout);

                        if (!success) {

                            boolean peerDiscovery = Service.isSupportPeerDiscovery(
                                    context);
                            success = IdentityService.connectPeer(
                                    context, pid, BuildConfig.ApiAesKey, Service.PROTOCOL,
                                    peerDiscovery, true, true);

                            if (!success) {
                                Singleton.getInstance(context).getConsoleListener().info(
                                        "Can't connect to PID :" + pid);
                            }

                        } else {
                            ipfs.protectPeer(pid, GatewayService.TAG);
                        }
                    } else {
                        success = true;
                        ipfs.protectPeer(pid, GatewayService.TAG);
                    }


                    if (success) {
                        Contents contents = downloadContents(context, cid);

                        if (contents != null) {

                            contentService.finishContent(cid);

                            downloadContents(context, pid, contents);
                        }
                    }

                }
            }

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
        return success;

    }
}
