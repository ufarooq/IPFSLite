package threads.server.services;

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

import threads.core.contents.CDS;
import threads.core.contents.Content;
import threads.core.contents.ContentEntry;
import threads.core.contents.Contents;
import threads.core.events.EVENTS;
import threads.core.peers.PEERS;
import threads.core.peers.User;
import threads.core.threads.Status;
import threads.core.threads.THREADS;
import threads.core.threads.Thread;
import threads.ipfs.CID;
import threads.ipfs.IPFS;
import threads.ipfs.Multihash;
import threads.ipfs.PID;
import threads.server.Preferences;
import threads.server.R;
import threads.server.Service;
import threads.server.jobs.JobServiceDownload;
import threads.share.Network;

import static androidx.core.util.Preconditions.checkNotNull;

public class ContentsService {

    private static final String TAG = ContentsService.class.getSimpleName();


    public static void contents(@NonNull Context context) {
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

        final CDS contentService = CDS.getInstance(context);
        final PEERS threads = PEERS.getInstance(context);

        final IPFS ipfs = IPFS.getInstance(context);
        final PID host = IPFS.getPID(context);

        try {
            checkNotNull(ipfs, "IPFS not valid");
            for (PID user : threads.getUsersPIDs()) {

                if (user.equals(host)) {
                    continue;
                }

                if (!threads.isUserBlocked(user)) {

                    long timestamp = System.currentTimeMillis() -
                            TimeUnit.MINUTES.toMillis(10);


                    List<Content> contents = contentService.getContentDatabase().
                            contentDao().getContents(user, timestamp, false);

                    if (!contents.isEmpty()) {

                        SwarmService.ConnectInfo info = SwarmService.connect(context, user);

                        if (info.isConnected()) {
                            for (Content entry : contents) {
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
            long fileSize,
            @Nullable String mimeType,
            @Nullable String image) {

        checkNotNull(context);
        checkNotNull(sender);
        checkNotNull(cid);


        final PEERS peers = PEERS.getInstance(context);
        final EVENTS events = EVENTS.getInstance(context);
        try {

            User user = peers.getUserByPID(sender);
            if (user == null) {
                events.error(context.getString(R.string.unknown_peer_sends_data));
                return;
            }

            CID thumbnail = null;

            if (image != null) {
                thumbnail = downloadImage(context, image);
            }

            createThread(context, user, cid, filename, fileSize, mimeType, thumbnail);


        } catch (Throwable e) {
            events.exception(e);
        }


    }

    private static synchronized void createThread(
            @NonNull Context context,
            @NonNull User user,
            @NonNull CID cid,
            @Nullable String filename,
            long fileSize,
            @Nullable String mimeType,
            @Nullable CID thumbnail) {

        final THREADS threads = THREADS.getInstance(context);
        final IPFS ipfs = IPFS.getInstance(context);
        checkNotNull(ipfs, "IPFS not valid");
        List<Thread> entries = threads.getThreadsByCIDAndParent(cid, 0L);

        if (entries.isEmpty()) {

            Service.createThread(context, ipfs, user, cid,
                    Status.ERROR, filename, fileSize, mimeType, thumbnail);

        }

    }

    @Nullable
    private static CID downloadImage(@NonNull Context context,
                                     @NonNull String image) {

        final IPFS ipfs = IPFS.getInstance(context);
        final int timeout = Preferences.getConnectionTimeout(context);

        try {
            try {
                Multihash.fromBase58(image);
            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
                return null;
            }

            CID cid = CID.create(image);
            byte[] data = ipfs.loadData(cid, timeout);
            if (data != null) {
                return cid;
            }

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }

        return null;
    }

    private static Contents downloadContents(@NonNull Context context,
                                             @NonNull CID cid) {
        final Gson gson = new Gson();
        final IPFS ipfs = IPFS.getInstance(context);
        final int timeout = Preferences.getConnectionTimeout(context);

        try {

            String content = ipfs.loadText(cid, timeout);

            if (content != null) {

                return gson.fromJson(content, Contents.class);
            }

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }

        return null;
    }

    public static boolean download(@NonNull Context context, @NonNull PID pid, @NonNull CID cid) {
        checkNotNull(context);
        checkNotNull(pid);
        checkNotNull(cid);

        final CDS contentService = CDS.getInstance(context);
        final PEERS peers = PEERS.getInstance(context);


        boolean success = false;
        try {

            if (peers.existsUser(pid)) {

                if (!peers.isUserBlocked(pid)) {

                    SwarmService.ConnectInfo info = SwarmService.connect(context, pid);
                    success = info.isConnected();

                    Contents contents = downloadContents(context, cid);

                    if (contents != null) {

                        // Send a message that the notification is downloaded
                        Service.sendReceiveMessage(context, pid.getPid());


                        contentService.finishContent(cid);

                        downloadContents(context, pid, contents);
                    }

                    // This has to be evaluated again if it really required
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
