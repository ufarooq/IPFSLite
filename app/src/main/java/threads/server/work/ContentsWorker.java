package threads.server.work;

import android.content.Context;
import android.util.Log;
import android.webkit.MimeTypeMap;

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

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import threads.ipfs.CID;
import threads.ipfs.IPFS;
import threads.ipfs.Multihash;
import threads.ipfs.PID;
import threads.ipfs.Progress;
import threads.server.core.contents.CDS;
import threads.server.core.contents.ContentEntry;
import threads.server.core.contents.Contents;
import threads.server.core.events.EVENTS;
import threads.server.core.peers.Content;
import threads.server.core.peers.PEERS;
import threads.server.core.peers.User;
import threads.server.core.threads.THREADS;
import threads.server.core.threads.Thread;
import threads.server.services.LiteService;
import threads.server.services.ThumbnailService;

import static androidx.core.util.Preconditions.checkArgument;
import static androidx.core.util.Preconditions.checkNotNull;

public class ContentsWorker extends Worker {
    private static final String WID = "CW";
    private static final String TAG = ContentsWorker.class.getSimpleName();

    public ContentsWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    public static void download(@NonNull Context context, @NonNull String cid, @NonNull String pid) {
        checkNotNull(context);
        checkNotNull(cid);
        checkNotNull(pid);
        Constraints.Builder builder = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED);

        Data.Builder data = new Data.Builder();
        data.putString(Content.CID, cid);
        data.putString(Content.PID, pid);

        OneTimeWorkRequest syncWorkRequest =
                new OneTimeWorkRequest.Builder(ContentsWorker.class)
                        .addTag(TAG)
                        .setConstraints(builder.build())
                        .setInputData(data.build())
                        .build();

        WorkManager.getInstance(context).enqueueUniqueWork(
                WID + cid, ExistingWorkPolicy.KEEP, syncWorkRequest);


    }

    private void downloadContent(@NonNull CID cid) {
        checkNotNull(cid);

        final THREADS threads = THREADS.getInstance(getApplicationContext());

        final EVENTS events = EVENTS.getInstance(getApplicationContext());

        try {


            List<Thread> entries = threads.getThreadsByContentAndParent(cid, 0L);

            if (!entries.isEmpty()) {
                Thread entry = entries.get(0);

                if (!entry.isDeleting() && !entry.isSeeding()) {
                    threads.setThreadLeaching(entry.getIdx(), true);
                    DownloadThreadWorker.download(getApplicationContext(), entry.getIdx());
                }
            }


        } catch (Throwable e) {
            events.exception(e);
        }

    }


    private void createThread(@NonNull CID cid, @Nullable String filename,
                              @Nullable String mimeType, long fileSize) {

        final THREADS threads = THREADS.getInstance(getApplicationContext());
        final IPFS ipfs = IPFS.getInstance(getApplicationContext());
        checkNotNull(ipfs, "IPFS not valid");
        List<Thread> entries = threads.getThreadsByContentAndParent(cid, 0L);

        if (entries.isEmpty()) {

            Thread thread = threads.createThread(0L);
            thread.setContent(cid);


            if (filename != null) {
                thread.setName(filename);
                if (mimeType != null) {
                    thread.setMimeType(mimeType);
                } else {
                    String evalMimeType = evaluateMimeType(filename);
                    if (evalMimeType != null) {
                        thread.setMimeType(evalMimeType);
                    }

                }
            } else {
                if (mimeType != null) {
                    thread.setMimeType(mimeType);
                }
                thread.setName(cid.getCid());
            }
            thread.setSize(fileSize);

            threads.storeThread(thread);

        }
    }

    private String evaluateMimeType(@NonNull String filename) {
        checkNotNull(filename);
        try {
            Optional<String> extension = ThumbnailService.getExtension(filename);
            if (extension.isPresent()) {
                String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.get());
                if (mimeType != null) {
                    return mimeType;
                }
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
        return null;
    }


    @NonNull
    @Override
    public Result doWork() {

        long start = System.currentTimeMillis();
        Log.e(TAG, " start [" + (System.currentTimeMillis() - start) + "]...");

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

                    Contents contents = downloadContents(cid);

                    if (contents != null) {
                        contentService.finishContent(cid);
                        downloadContents(contents);
                    }
                }
            } else {
                // create a new unknown user
                createBlockedUser(pid);
            }

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        } finally {
            Log.e(TAG, " finish onStart [" + (System.currentTimeMillis() - start) + "]...");
        }
        return Result.success();
    }

    private void createBlockedUser(@NonNull PID pid) {
        checkNotNull(pid);

        IPFS ipfs = IPFS.getInstance(getApplicationContext());
        PEERS peers = PEERS.getInstance(getApplicationContext());
        checkNotNull(ipfs, "IPFS not defined");


        if (peers.getUserByPID(pid) == null) {

            User user = peers.createUser(pid, pid.getPid());
            user.setLite(false);
            user.setBlocked(true);
            peers.storeUser(user);

            ConnectUserWorker.connect(getApplicationContext(), pid.getPid());
        }
    }

    private void downloadContents(@NonNull Contents files) {
        for (ContentEntry file : files) {

            String cidStr = file.getCid();
            try {
                Multihash.fromBase58(cidStr);
            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
                continue;
            }
            createThread(CID.create(cidStr), file.getFilename(),
                    file.getMimeType(), file.getSize());

        }

        if (LiteService.isAutoDownload(getApplicationContext())) {
            Collections.reverse(files);
            for (ContentEntry file : files) {
                downloadContent(CID.create(file.getCid()));
            }
        }

    }

    private Contents downloadContents(@NonNull CID cid) throws IOException {

        final Gson gson = new Gson();
        final IPFS ipfs = IPFS.getInstance(getApplicationContext());

        File file = ipfs.createCacheFile(cid);
        try {

            boolean success = ipfs.loadToFile(file, cid,
                    new Progress() {
                        @Override
                        public boolean isClosed() {
                            return isStopped();
                        }
                    });

            if (success) {
                FileReader reader = new FileReader(file);
                String content = IOUtils.toString(reader);
                reader.close();

                return gson.fromJson(content, Contents.class);
            } else {
                Log.e(TAG, "Content " + cid + " couldn't be downloaded");
            }


        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        } finally {
            if (file.exists()) {
                checkArgument(file.delete());
            }
        }

        return null;
    }
}
