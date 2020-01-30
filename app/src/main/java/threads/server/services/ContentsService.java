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

import threads.ipfs.CID;
import threads.ipfs.IPFS;
import threads.ipfs.Multihash;
import threads.ipfs.PID;
import threads.server.R;
import threads.server.core.contents.CDS;
import threads.server.core.contents.Content;
import threads.server.core.contents.ContentEntry;
import threads.server.core.contents.Contents;
import threads.server.core.events.EVENTS;
import threads.server.core.peers.PEERS;
import threads.server.core.peers.User;
import threads.server.core.threads.THREADS;
import threads.server.core.threads.Thread;
import threads.server.jobs.JobServiceDownload;
import threads.server.utils.Network;
import threads.server.work.DownloadThumbnailWorker;

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
        final PID host = IPFS.getPID(context);
        try {
            String alias;

            if (sender.equals(host)) {
                alias = IPFS.getDeviceName();
            } else {
                User user = peers.getUserByPID(sender);

                if (user == null) {
                    events.error(context.getString(R.string.unknown_peer_sends_data));
                    return;
                } else {
                    alias = user.getAlias();
                }
            }


            long idx = createThread(context, sender, alias, cid, filename, fileSize, mimeType);

            if (idx > 0) {
                if (image != null) {
                    DownloadThumbnailWorker.download(context, image, idx);
                }
            }

        } catch (Throwable e) {
            events.exception(e);
        }


    }

    private static synchronized long createThread(
            @NonNull Context context,
            @NonNull PID sender,
            @NonNull String alias,
            @NonNull CID cid,
            @Nullable String filename,
            long fileSize,
            @Nullable String mimeType) {

        final THREADS threads = THREADS.getInstance(context);
        final IPFS ipfs = IPFS.getInstance(context);
        checkNotNull(ipfs, "IPFS not valid");
        List<Thread> entries = threads.getThreadsByCcontentAndParent(cid, 0L);

        if (entries.isEmpty()) {

            return Service.createThread(context, sender, alias, cid,
                    filename, fileSize, mimeType, null);

        }
        return -1;
    }


    private static Contents downloadContents(@NonNull Context context,
                                             @NonNull CID cid) {
        final Gson gson = new Gson();
        final IPFS ipfs = IPFS.getInstance(context);

        // TODO convert to worker
        try {

            String content = ipfs.loadText(cid);

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
