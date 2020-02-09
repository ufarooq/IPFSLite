package threads.server.work;

import android.content.Context;
import android.provider.DocumentsContract;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import threads.ipfs.CID;
import threads.ipfs.IPFS;
import threads.ipfs.LinkInfo;
import threads.server.core.threads.THREADS;
import threads.server.core.threads.Thread;

import static androidx.core.util.Preconditions.checkArgument;
import static androidx.core.util.Preconditions.checkNotNull;

public class DownloadThreadWorker extends Worker {
    public static final String WID = "DTW";
    private static final String TAG = DownloadThreadWorker.class.getSimpleName();
    private static final String IDX = "IDX";

    public DownloadThreadWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);

    }

    public static void download(@NonNull Context context, long idx) {
        checkNotNull(context);


        Constraints.Builder builder = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED);


        Data.Builder data = new Data.Builder();
        data.putLong(IDX, idx);

        OneTimeWorkRequest syncWorkRequest =
                new OneTimeWorkRequest.Builder(DownloadThreadWorker.class)
                        .addTag(DownloadThreadWorker.TAG)
                        .setInputData(data.build())
                        .setConstraints(builder.build())
                        .build();

        WorkManager.getInstance(context).enqueueUniqueWork(
                WID + idx, ExistingWorkPolicy.KEEP, syncWorkRequest);
    }

    @NonNull
    @Override
    public Result doWork() {

        THREADS threads = THREADS.getInstance(getApplicationContext());

        long idx = getInputData().getLong(IDX, -1);
        checkArgument(idx >= 0);

        Thread thread = threads.getThreadByIdx(idx);
        checkNotNull(thread);

        CID cid = thread.getContent();
        checkNotNull(cid);

        List<LinkInfo> links = getLinks(cid);

        if (links != null) {
            if (links.isEmpty()) {

                DownloadContentWorker.download(getApplicationContext(), cid,
                        thread.getIdx(), thread.getName(), thread.getSize());


            } else {

                // thread is directory
                if (!thread.isDir()) {
                    threads.setMimeType(thread, DocumentsContract.Document.MIME_TYPE_DIR);
                }
                downloadLinks(thread, links);

            }
        }


        return Result.success();
    }


    @Nullable
    private List<LinkInfo> getLinks(@NonNull CID cid) {

        checkNotNull(cid);
        IPFS ipfs = IPFS.getInstance(getApplicationContext());

        List<LinkInfo> links = ipfs.ls(cid, this::isStopped);
        if (links == null) {
            Log.e(TAG, "no links");
            return null;
        }
        List<LinkInfo> result = new ArrayList<>();
        for (LinkInfo link : links) {
            if (!link.getName().isEmpty()) {
                result.add(link);
            }
        }
        return result;
    }

    private Thread getDirectoryThread(@NonNull Thread thread, @NonNull CID cid) {
        checkNotNull(thread);
        checkNotNull(cid);

        THREADS threads = THREADS.getInstance(getApplicationContext());
        List<Thread> entries = threads.getThreadsByContent(cid);
        if (!entries.isEmpty()) {
            for (Thread entry : entries) {
                if (entry.getParent() == thread.getIdx()) {
                    return entry;
                }
            }
        }
        return null;
    }

    private void downloadLinks(@NonNull Thread thread, @NonNull List<LinkInfo> links) {

        for (LinkInfo link : links) {

            CID cid = link.getCid();
            Thread entry = getDirectoryThread(thread, cid);
            if (entry != null) {
                if (!entry.isSeeding()) {
                    downloadLink(entry, link);
                }
            } else {

                long idx = createThread(cid, link, thread.getIdx());
                entry = THREADS.getInstance(getApplicationContext()).getThreadByIdx(idx);
                checkNotNull(entry);

                downloadLink(entry, link);
            }

        }
    }

    private void downloadLink(@NonNull Thread thread, @NonNull LinkInfo link) {

        if (link.isDirectory()) {
            DownloadThreadWorker.download(getApplicationContext(), thread.getIdx());
        } else {
            DownloadContentWorker.download(getApplicationContext(), link.getCid(),
                    thread.getIdx(), link.getName(), link.getSize());
        }

    }

    private long createThread(@NonNull CID cid, @NonNull LinkInfo link, long parent) {


        checkNotNull(cid);
        checkNotNull(link);


        THREADS threads = THREADS.getInstance(getApplicationContext());

        Thread thread = threads.createThread(parent);
        thread.setContent(cid);
        String filename = link.getName();
        thread.setName(filename);

        long size = link.getSize();
        thread.setSize(size);

        if (link.isDirectory()) {
            thread.setMimeType(DocumentsContract.Document.MIME_TYPE_DIR);
        } else {
            String mimeType = evaluateMimeType(filename);
            if (mimeType != null) {
                thread.setMimeType(mimeType);
            }
        }

        return threads.storeThread(thread);
    }


    private Optional<String> getExtension(@Nullable String filename) {
        return Optional.ofNullable(filename)
                .filter(f -> f.contains("."))
                .map(f -> f.substring(filename.lastIndexOf(".") + 1));
    }

    @Nullable
    private String evaluateMimeType(@NonNull String filename) {
        try {
            Optional<String> extension = getExtension(filename);
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


}
