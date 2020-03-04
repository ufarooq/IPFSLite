package threads.server.work;


import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
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
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import threads.ipfs.CID;
import threads.ipfs.IPFS;
import threads.ipfs.Progress;
import threads.server.InitApplication;
import threads.server.MainActivity;
import threads.server.R;
import threads.server.core.peers.Content;
import threads.server.core.threads.THREADS;
import threads.server.core.threads.Thread;
import threads.server.services.ThumbnailService;
import threads.server.utils.MimeType;
import threads.server.utils.Network;

public class DownloadContentWorker extends Worker {
    private static final String WID = "DCW";
    private static final String TAG = DownloadContentWorker.class.getSimpleName();
    private static final String FN = "FN";
    private static final String FS = "FS";
    private static final String CHANNEL_ID = "CHANNEL_ID";
    private final AtomicBoolean mInit = new AtomicBoolean(false);
    private NotificationCompat.Builder mBuilder;
    private NotificationManager mNotificationManager;
    public DownloadContentWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);

    }

    public static String getUniqueId(long idx) {
        return WID + idx;
    }

    public static void download(@NonNull Context context, @NonNull CID cid,
                                long idx, @NonNull String filename, long size) {


        Log.e(TAG, "DownloadContentWorker mark for running : " + filename);

        Constraints.Builder builder = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED);


        Data.Builder data = new Data.Builder();
        data.putString(FN, filename);
        data.putString(Content.CID, cid.getCid());
        data.putLong(FS, size);
        data.putLong(Content.IDX, idx);

        OneTimeWorkRequest syncWorkRequest =
                new OneTimeWorkRequest.Builder(DownloadContentWorker.class)
                        .addTag(TAG)
                        .setInputData(data.build())
                        .setConstraints(builder.build())
                        .build();

        WorkManager.getInstance(context).enqueueUniqueWork(
                getUniqueId(idx), ExistingWorkPolicy.KEEP, syncWorkRequest);
    }

    public static void createChannel(@NonNull Context context) {


        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            try {
                CharSequence name = context.getString(R.string.channel_name);
                String description = context.getString(R.string.channel_description);
                NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name,
                        NotificationManager.IMPORTANCE_HIGH);
                mChannel.setDescription(description);

                NotificationManager notificationManager = (NotificationManager) context.getSystemService(
                        Context.NOTIFICATION_SERVICE);
                if (notificationManager != null) {
                    notificationManager.createNotificationChannel(mChannel);
                }

            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }
        }
    }

    private void reportProgress(long idx, @NonNull String content, int percent) {
        if (!mInit.getAndSet(true)) {
            mBuilder = createProgressNotification(content);
        }
        mBuilder.setProgress(100, percent, false);
        if (mNotificationManager != null) {
            mNotificationManager.notify((int) idx, mBuilder.build());
        }
    }

    private String getFileInfo(@NonNull Thread thread) {
        String fileName = thread.getName();

        String fileSize;
        long size = thread.getSize();

        if (size < 1000) {
            fileSize = String.valueOf(size);
            return getApplicationContext().getString(R.string.link_format, fileName, fileSize);
        } else if (size < 1000 * 1000) {
            fileSize = String.valueOf((double) (size / 1000));
            return getApplicationContext().getString(R.string.link_format_kb, fileName, fileSize);
        } else {
            fileSize = String.valueOf((double) (size / (1000 * 1000)));
            return getApplicationContext().getString(R.string.link_format_mb, fileName, fileSize);
        }
    }

    @NonNull
    @Override
    public Result doWork() {

        long start = System.currentTimeMillis();

        Log.e(TAG, " start [" + (System.currentTimeMillis() - start) + "]...");


        int timeout = InitApplication.getDownloadTimeout(getApplicationContext());
        try {

            mNotificationManager = (NotificationManager)
                    getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

            THREADS threads = THREADS.getInstance(getApplicationContext());
            IPFS ipfs = IPFS.getInstance(getApplicationContext());

            String filename = getInputData().getString(FN);
            Objects.requireNonNull(filename);
            String cidStr = getInputData().getString(Content.CID);
            Objects.requireNonNull(cidStr);
            long idx = getInputData().getLong(Content.IDX, -1);

            long size = getInputData().getLong(FS, -1);

            CID cid = CID.create(cidStr);
            Thread thread = threads.getThreadByIdx(idx);
            Objects.requireNonNull(thread);

            // security check that thread is not already seeding
            if (thread.isSeeding()) {
                return Result.success();
            }


            AtomicLong started = new AtomicLong(System.currentTimeMillis());
            boolean success;
            try {
                String content = getFileInfo(thread);
                File file = ipfs.createCacheFile(cid);
                success = ipfs.loadToFile(file, cid,
                        new Progress() {
                            @Override
                            public boolean isClosed() {

                                long diff = System.currentTimeMillis() - started.get();
                                boolean abort = !Network.isConnected(getApplicationContext())
                                        || (diff > (timeout * 1000));
                                return isStopped() || abort;
                            }

                            @Override
                            public void setProgress(int percent) {
                                threads.setThreadProgress(idx, percent);
                                reportProgress(idx, content, percent);
                                started.set(System.currentTimeMillis());
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
                } else {
                    threads.resetThreadLeaching(idx);
                    if (file.exists()) {
                        boolean res = file.delete();
                        if (!res) {
                            Log.e(TAG, "Could not delete file");
                        }
                    }
                }
                checkParentComplete(thread.getParent());
            } finally {
                if (mNotificationManager != null) {
                    mNotificationManager.cancel((int) idx);
                }
            }

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        } finally {
            Log.e(TAG, " finish onStart [" + (System.currentTimeMillis() - start) + "]...");
        }
        return Result.success();

    }

    private NotificationCompat.Builder createProgressNotification(@NonNull String content) {

        Intent main = new Intent(getApplicationContext(), MainActivity.class);

        int requestID = (int) System.currentTimeMillis();
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), requestID,
                main, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                getApplicationContext(), CHANNEL_ID);
        builder.setContentIntent(pendingIntent);
        builder.setProgress(0, 0, true);
        builder.setAutoCancel(true);
        builder.setContentText(content);
        builder.setPriority(NotificationManager.IMPORTANCE_HIGH);
        builder.setSmallIcon(R.drawable.download);
        return builder;
    }

    private void checkParentComplete(long parent) {

        if (parent == 0L) {
            return;
        }
        THREADS threads = THREADS.getInstance(getApplicationContext());

        try {
            int allSeeding = 0;
            boolean isOneLeaching = false;
            List<Thread> list = threads.getChildren(parent);
            for (Thread entry : list) {
                if (entry.isSeeding()) {
                    allSeeding++;
                } else if (entry.isLeaching()) {
                    isOneLeaching = true;
                }
            }

            boolean finished = allSeeding == list.size();

            int progress = (int) (((double) allSeeding * 100) / (double) list.size());
            threads.setThreadProgress(parent, progress);

            if (finished) {
                threads.setThreadSeeding(parent);
            } else {
                if (!isOneLeaching) {
                    threads.resetThreadLeaching(parent);
                }
            }
            Thread thread = threads.getThreadByIdx(parent);
            Objects.requireNonNull(thread);
            checkParentComplete(thread.getParent());
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }

    }

}
