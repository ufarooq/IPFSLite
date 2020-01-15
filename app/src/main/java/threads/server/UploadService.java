package threads.server;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import threads.core.Preferences;
import threads.core.events.EVENTS;
import threads.core.threads.Kind;
import threads.core.threads.Status;
import threads.core.threads.THREADS;
import threads.core.threads.Thread;
import threads.ipfs.CID;
import threads.ipfs.IPFS;
import threads.ipfs.PID;
import threads.share.ThumbnailService;

import static androidx.core.util.Preconditions.checkNotNull;


public class UploadService extends Service {

    private static final ExecutorService UPLOAD_SERVICE = Executors.newFixedThreadPool(1);
    private static final String HIGH_CHANNEL_ID = "HIGH_CHANNEL_ID";
    private static final int NOTIFICATION_ID = 999;
    private static final String TAG = UploadService.class.getSimpleName();
    private static final String URI = "URI";
    private final AtomicInteger counter = new AtomicInteger(0);

    public static void invoke(@NonNull Context context, @NonNull Uri uri) {
        checkNotNull(context);
        checkNotNull(uri);
        try {
            Intent intent = new Intent(context, UploadService.class);
            intent.putExtra(URI, uri);
            ContextCompat.startForegroundService(context, intent);
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    private static void createChannel(@NonNull Context context) {
        checkNotNull(context);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            try {
                CharSequence name = context.getString(R.string.upload_channel_name);
                String description = context.getString(R.string.upload_channel_description);
                int importance = NotificationManager.IMPORTANCE_HIGH;
                NotificationChannel mChannel = new NotificationChannel(HIGH_CHANNEL_ID, name, importance);
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

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        checkNotNull(intent);

        if (counter.get() == 0) {
            startForeground(NOTIFICATION_ID, buildNotification());
        }
        Uri uri = intent.getParcelableExtra(URI);
        checkNotNull(uri);
        storeData(uri);
        return START_NOT_STICKY;
    }

    private void storeData(@NonNull Uri uri) {
        checkNotNull(uri);
        final EVENTS events = EVENTS.getInstance(getApplicationContext());
        final THREADS threads = THREADS.getInstance(getApplicationContext());

        counter.incrementAndGet();
        UPLOAD_SERVICE.submit(() -> {
            try {


                IPFS ipfs = IPFS.getInstance(getApplicationContext());
                checkNotNull(ipfs, "IPFS is not valid");
                InputStream inputStream =
                        getContentResolver().openInputStream(uri);
                checkNotNull(inputStream);

                PID pid = IPFS.getPID(getApplicationContext());
                checkNotNull(pid);
                String alias = IPFS.getDeviceName();
                checkNotNull(alias);

                ThumbnailService.FileDetails fileDetails =
                        ThumbnailService.getFileDetails(getApplicationContext(), uri);
                checkNotNull(fileDetails);

                String name = fileDetails.getFileName();
                long size = fileDetails.getFileSize();
                String mimeType = fileDetails.getMimeType();

                buildNotification(name);

                Thread thread = threads.createThread(
                        pid, alias, Status.INIT, Kind.IN, 0L);

                thread.setName(name);
                thread.setSize(size);
                thread.setMimeType(mimeType);
                CID thumbnail = ThumbnailService.getThumbnail(
                        getApplicationContext(), uri, mimeType);
                if (thumbnail != null) {
                    thread.setThumbnail(thumbnail);
                }
                long idx = threads.storeThread(thread);


                try {
                    threads.setThreadLeaching(idx, true);

                    CID cid = ipfs.storeStream(inputStream, true);
                    checkNotNull(cid);

                    // cleanup of entries with same CID and hierarchy
                    List<Thread> sameEntries = threads.getThreadsByCIDAndParent(cid, 0L);
                    threads.removeThreads(ipfs, sameEntries);


                    threads.setThreadContent(idx, cid);
                    threads.setThreadStatus(idx, Status.SEEDING);
                    threads.setThreadLeaching(idx, false);
                } catch (Throwable e) {
                    threads.removeThreads(ipfs, idx);
                    buildFailedNotification(name);
                    throw e;
                }

            } catch (FileNotFoundException e) {
                Preferences.error(events, getString(R.string.file_not_found));
            } catch (Throwable e) {
                Preferences.evaluateException(events, Preferences.EXCEPTION, e);
            } finally {
                if (counter.decrementAndGet() == 0) {
                    try {
                        stopForeground(true);
                    } finally {
                        stopSelf();
                    }
                }
            }
        });
    }

    @Override
    public void onCreate() {
        try {
            createChannel(getApplicationContext());
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
        super.onCreate();
    }


    private Notification buildNotification() {

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(),
                HIGH_CHANNEL_ID);

        builder.setContentTitle(getString(R.string.upload_to_ipfs));
        builder.setSmallIcon(R.drawable.action_upload);
        builder.setPriority(NotificationManager.IMPORTANCE_MAX);

        Notification notification = builder.build();

        NotificationManager notificationManager = (NotificationManager)
                getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, notification);
        }
        return notification;
    }

    private void buildNotification(@NonNull String name) {
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, HIGH_CHANNEL_ID)
                        .setSmallIcon(R.drawable.action_upload)
                        .setContentTitle(getString(R.string.upload) + " : " + name)
                        .setPriority(NotificationCompat.PRIORITY_MAX);

        Notification notification = builder.build();

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, notification);
        }
    }

    private void buildFailedNotification(@NonNull String name) {

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(),
                HIGH_CHANNEL_ID);

        builder.setContentTitle(getString(R.string.upload_failed, name));
        builder.setSmallIcon(R.drawable.action_upload);
        builder.setPriority(NotificationManager.IMPORTANCE_MAX);

        Notification notification = builder.build();

        NotificationManager notificationManager = (NotificationManager)
                getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, notification);
        }
    }


}
