package threads.server.work;


import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.j256.simplemagic.ContentInfo;

import java.io.File;
import java.util.List;

import threads.ipfs.CID;
import threads.ipfs.IPFS;
import threads.server.core.threads.THREADS;
import threads.server.core.threads.Thread;
import threads.server.services.ThumbnailService;
import threads.server.utils.MimeType;
import threads.server.utils.ProgressChannel;

import static androidx.core.util.Preconditions.checkArgument;
import static androidx.core.util.Preconditions.checkNotNull;

public class DownloadContentWorker extends Worker {
    public static final String WID = "DLW";
    private static final String TAG = DownloadContentWorker.class.getSimpleName();
    private static final String ID = "ID";
    private static final String FN = "FN";
    private static final String FS = "FS";
    private static final String IDX = "IDX";

    public DownloadContentWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);

    }

    public static void download(@NonNull Context context, @NonNull CID cid,
                                long threadIdx, @NonNull String filename, long size) {

        checkNotNull(context);
        checkNotNull(cid);
        checkNotNull(filename);


        Constraints.Builder builder = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED);


        Data.Builder data = new Data.Builder();
        data.putString(FN, filename);
        data.putString(ID, cid.getCid());
        data.putLong(FS, size);
        data.putLong(IDX, threadIdx);

        OneTimeWorkRequest syncWorkRequest =
                new OneTimeWorkRequest.Builder(DownloadContentWorker.class)
                        .addTag("DownloadContentWorker")
                        .setInputData(data.build())
                        .setConstraints(builder.build())
                        .build();

        WorkManager.getInstance(context).enqueueUniqueWork(
                WID + threadIdx, ExistingWorkPolicy.KEEP, syncWorkRequest);
    }

    @NonNull
    @Override
    public Result doWork() {

        THREADS threads = THREADS.getInstance(getApplicationContext());
        IPFS ipfs = IPFS.getInstance(getApplicationContext());

        String filename = getInputData().getString(FN);
        checkNotNull(filename);
        String cidStr = getInputData().getString(ID);
        checkNotNull(cidStr);
        long idx = getInputData().getLong(IDX, -1);
        checkArgument(idx >= 0);
        long size = getInputData().getLong(FS, -1);

        CID cid = CID.create(cidStr);
        Thread thread = threads.getThreadByIdx(idx);
        checkNotNull(thread);


        NotificationCompat.Builder builder =
                ProgressChannel.createProgressNotification(
                        getApplicationContext(), filename);

        final NotificationManager notificationManager = (NotificationManager)
                getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        int notifyID = cid.getCid().hashCode();
        Notification notification = builder.build();
        if (notificationManager != null) {
            notificationManager.notify(notifyID, notification);
        }

        boolean success;
        try {
            threads.setThreadLeaching(idx, true);
            markLeaching(thread.getParent());

            File file = ipfs.getTempCacheFile();
            success = ipfs.loadToFile(file, cid,
                    (percent) -> {
                        // TODO isStopped
                        builder.setProgress(100, percent, false);
                        threads.setThreadProgress(idx, percent);
                        if (notificationManager != null) {
                            notificationManager.notify(notifyID, builder.build());
                        }


                    });

            if (success) {
                threads.setThreadSeeding(idx);

                if (size != file.length()) {
                    threads.setThreadSize(idx, file.length());
                }

                if (thread.getMimeType().isEmpty()) {
                    ContentInfo contentInfo = ipfs.getContentInfo(file);
                    if (contentInfo != null) {
                        String mimeType = contentInfo.getMimeType();
                        if (mimeType != null) {
                            threads.setThreadMimeType(idx, mimeType);
                        } else {
                            threads.setThreadMimeType(idx, MimeType.OCTET_MIME_TYPE);
                        }
                        String name = thread.getName();
                        if (!name.contains(".")) {
                            String[] extensions = contentInfo.getFileExtensions();
                            if (extensions != null) {
                                String ext = extensions[0];
                                threads.setThreadName(idx, name + "." + ext);
                            }
                        }
                    } else {
                        threads.setThreadMimeType(idx, MimeType.OCTET_MIME_TYPE);
                    }
                }

                if (threads.getThreadThumbnail(idx) == null) {
                    String mimeType = threads.getThreadMimeType(idx);
                    if (mimeType != null) {
                        CID image = ThumbnailService.getThumbnail(getApplicationContext(),
                                file, mimeType);
                        if (image != null) {
                            threads.setThreadThumbnail(idx, image);
                        }
                    }
                }
            }

            if (file.exists()) {
                checkArgument(file.delete());
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        } finally {
            threads.setThreadLeaching(idx, false);
            if (notificationManager != null) {
                notificationManager.cancel(notifyID);
            }

            checkParentComplete(thread.getParent());
        }

        return Result.success();

    }

    private void markLeaching(long parent) {
        if (parent == 0L) {
            return;
        }
        THREADS threads = THREADS.getInstance(getApplicationContext());
        threads.setThreadLeaching(parent, true);

        Thread thread = threads.getThreadByIdx(parent);
        checkNotNull(thread);
        markLeaching(thread.getParent());
    }


    private void checkParentComplete(long parent) {

        if (parent == 0L) {
            return;
        }
        THREADS threads = THREADS.getInstance(getApplicationContext());

        try {
            int allSeeding = 0;
            List<Thread> list = threads.getChildren(parent);
            for (Thread entry : list) {
                if (entry.isSeeding()) {
                    allSeeding++;
                }
            }

            boolean finished = allSeeding == list.size();

            int progress = (int) (((double) allSeeding * 100) / (double) list.size());
            threads.setThreadProgress(parent, progress);

            if (finished) {
                threads.setThreadSeeding(parent);

                Thread thread = threads.getThreadByIdx(parent);
                checkNotNull(thread);
                checkParentComplete(thread.getParent());
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }

    }
}
