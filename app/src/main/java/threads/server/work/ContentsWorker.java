package threads.server.work;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.gson.Gson;

import java.util.Collections;
import java.util.List;

import threads.ipfs.CID;
import threads.ipfs.IPFS;
import threads.ipfs.Multihash;
import threads.ipfs.PID;
import threads.ipfs.Progress;
import threads.server.R;
import threads.server.core.contents.CDS;
import threads.server.core.contents.ContentEntry;
import threads.server.core.contents.Contents;
import threads.server.core.events.EVENTS;
import threads.server.core.peers.Content;
import threads.server.core.peers.PEERS;
import threads.server.core.peers.User;
import threads.server.core.threads.THREADS;
import threads.server.core.threads.Thread;
import threads.server.services.Service;
import threads.server.services.SwarmService;

import static androidx.core.util.Preconditions.checkNotNull;

public class ContentsWorker extends Worker {
    private static final String TAG = ContentsWorker.class.getSimpleName();

    public ContentsWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    public static void download(@NonNull Context context, @NonNull CID cid, @NonNull PID pid) {
        checkNotNull(context);
        checkNotNull(cid);
        checkNotNull(pid);
        Constraints.Builder builder = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED);

        Data.Builder data = new Data.Builder();
        data.putString(Content.CID, cid.getCid());
        data.putString(Content.PID, pid.getPid());

        OneTimeWorkRequest syncWorkRequest =
                new OneTimeWorkRequest.Builder(ContentsWorker.class)
                        .addTag(TAG)
                        .setConstraints(builder.build())
                        .setInputData(data.build())
                        .build();

        WorkManager.getInstance(context).enqueueUniqueWork(
                cid.getCid(), ExistingWorkPolicy.KEEP, syncWorkRequest);


    }

    private void downloadContent(@NonNull PID sender, @NonNull CID cid) {
        checkNotNull(sender);
        checkNotNull(cid);

        final THREADS threads = THREADS.getInstance(getApplicationContext());

        final EVENTS events = EVENTS.getInstance(getApplicationContext());

        try {


            List<Thread> entries = threads.getThreadsByContentAndParent(cid, 0L);

            if (!entries.isEmpty()) {
                Thread entry = entries.get(0);

                if (entry.isDeleting() || entry.isSeeding()) {
                    Service.replySender(getApplicationContext(), sender, entry);
                } else {
                    threads.setThreadLeaching(entry.getIdx(), true);
                    DownloadThreadWorker.download(getApplicationContext(),
                            entry.getIdx(), true);
                }
            }


        } catch (Throwable e) {
            events.exception(e);
        }

    }

    private void createSkeleton(
            @NonNull PID sender,
            @NonNull CID cid,
            @Nullable String filename,
            long fileSize,
            @Nullable String mimeType,
            @Nullable String image) {

        checkNotNull(sender);
        checkNotNull(cid);


        final PEERS peers = PEERS.getInstance(getApplicationContext());
        final EVENTS events = EVENTS.getInstance(getApplicationContext());
        final PID host = IPFS.getPID(getApplicationContext());
        try {
            String alias;

            if (sender.equals(host)) {
                alias = IPFS.getDeviceName();
            } else {
                User user = peers.getUserByPID(sender);

                if (user == null) {
                    events.error(getApplicationContext().getString(
                            R.string.unknown_peer_sends_data));
                    return;
                } else {
                    alias = user.getAlias();
                }
            }


            long idx = createThread(sender, alias, cid, filename, fileSize, mimeType);

            if (idx > 0) {
                if (image != null) {
                    DownloadThumbnailWorker.download(getApplicationContext(), image, idx);
                }
            }

        } catch (Throwable e) {
            events.exception(e);
        }


    }

    private long createThread(
            @NonNull PID sender,
            @NonNull String alias,
            @NonNull CID cid,
            @Nullable String filename,
            long fileSize,
            @Nullable String mimeType) {

        final THREADS threads = THREADS.getInstance(getApplicationContext());
        final IPFS ipfs = IPFS.getInstance(getApplicationContext());
        checkNotNull(ipfs, "IPFS not valid");
        List<Thread> entries = threads.getThreadsByContentAndParent(cid, 0L);

        if (entries.isEmpty()) {

            return Service.createThread(getApplicationContext(), sender, alias, cid,
                    filename, fileSize, mimeType);

        }
        return -1;
    }

    @NonNull
    @Override
    public Result doWork() {

        try {
            String cidStr = getInputData().getString(Content.CID);
            checkNotNull(cidStr);
            String pidStr = getInputData().getString(Content.PID);
            checkNotNull(pidStr);

            Multihash.fromBase58(cidStr);
            Multihash.fromBase58(pidStr);

            CID cid = CID.create(cidStr);
            PID pid = PID.create(pidStr);


            CDS contentService = CDS.getInstance(getApplicationContext());
            PEERS peers = PEERS.getInstance(getApplicationContext());


            if (peers.existsUser(pid)) {

                if (!peers.isUserBlocked(pid)) {

                    SwarmService.connect(getApplicationContext(), pid);

                    Contents contents = downloadContents(cid);

                    if (contents != null) {
                        contentService.finishContent(cid);
                        downloadContents(pid, contents);
                    }
                }
            } else {
                // create a new unknown user
                Service.createUnknownUser(getApplicationContext(), pid);

            }

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
        return Result.success();
    }

    private void downloadContents(@NonNull PID pid, @NonNull Contents files) {
        for (ContentEntry file : files) {

            String cidStr = file.getCid();
            try {
                Multihash.fromBase58(cidStr);
            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
                continue;
            }

            createSkeleton(pid, CID.create(cidStr), file.getFilename(),
                    file.getSize(), file.getMimeType(), file.getImage());


        }

        if (Service.isAutoDownload(getApplicationContext())) {
            Collections.reverse(files);
            for (ContentEntry file : files) {
                downloadContent(pid, CID.create(file.getCid()));
            }
        }

    }

    private Contents downloadContents(@NonNull CID cid) {

        final Gson gson = new Gson();
        final IPFS ipfs = IPFS.getInstance(getApplicationContext());

        try {

            String content = ipfs.loadText(cid, new Progress() {
                @Override
                public boolean isClosed() {
                    return isStopped();
                }
            });

            if (content != null) {

                return gson.fromJson(content, Contents.class);
            }

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }

        return null;
    }
}
