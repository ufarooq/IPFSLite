package threads.server.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import threads.ipfs.CID;
import threads.ipfs.IPFS;
import threads.ipfs.PID;
import threads.server.R;
import threads.server.core.events.EVENTS;
import threads.server.core.threads.THREADS;
import threads.server.core.threads.Thread;

import static androidx.core.util.Preconditions.checkNotNull;


public class UploadService extends Service {

    private static final ExecutorService UPLOAD_SERVICE = Executors.newFixedThreadPool(1);
    private static final String HIGH_CHANNEL_ID = "HIGH_CHANNEL_ID";
    private static final int NOTIFICATION_ID = 999;
    private static final String TAG = UploadService.class.getSimpleName();
    private static final String URI = "URI";
    private static final int THUMBNAIL_SIZE = 128;
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
    public static CID getThumbnail(@NonNull Context context,
                                   @NonNull Uri uri,
                                   @NonNull String mimeType) {
        checkNotNull(context);
        checkNotNull(uri);
        checkNotNull(mimeType);

        CID cid = null;
        byte[] bytes = null;


        try {
            bytes = getPreviewImage(context, uri, mimeType);
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage());
        }

        if (bytes != null) {
            try {
                cid = IPFS.getInstance(context).storeData(bytes);
            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }
        }

        return cid;
    }

    @Nullable
    private static byte[] getPreviewImage(@NonNull Context context,
                                          @NonNull Uri uri,
                                          @NonNull String mimeType) throws Exception {
        checkNotNull(context);
        checkNotNull(uri);
        checkNotNull(mimeType);

        Bitmap bitmap = getPreview(context, uri, mimeType);
        if (bitmap != null) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.WEBP, 100, stream);
            byte[] image = stream.toByteArray();
            bitmap.recycle();
            return image;
        }
        return null;
    }

    @Nullable
    private static Bitmap getPreview(@NonNull Context context, @NonNull Uri uri, @NonNull String mimeType) throws Exception {
        checkNotNull(context);
        checkNotNull(uri);
        checkNotNull(mimeType);

        if (mimeType.startsWith("video")) {

            MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
            mediaMetadataRetriever.setDataSource(context, uri);

            Bitmap bitmap = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {

                try {
                    bitmap = mediaMetadataRetriever.getPrimaryImage();
                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage());
                }
            }
            try {
                if (bitmap == null) {
                    final WeakReference<Bitmap> weakBmp = new WeakReference<>(mediaMetadataRetriever.getFrameAtTime());
                    bitmap = weakBmp.get();
                }
            } catch (Throwable e) {
                bitmap = mediaMetadataRetriever.getFrameAtTime();
            }
            mediaMetadataRetriever.release();
            return bitmap;

        } else if (mimeType.startsWith("application/pdf")) {
            return getPDFBitmap(context, uri);
        } else if (mimeType.startsWith("image")) {

            Bitmap bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
            return ThumbnailUtils.extractThumbnail(bitmap, THUMBNAIL_SIZE, THUMBNAIL_SIZE);
        }
        return null;
    }

    @NonNull
    private static Bitmap getPDFBitmap(@NonNull Context context, @NonNull Uri uri) throws Exception {
        checkNotNull(context);
        checkNotNull(uri);

        ParcelFileDescriptor fileDescriptor = context.getContentResolver().openFileDescriptor(
                uri, "r");
        checkNotNull(fileDescriptor);
        PdfRenderer pdfRenderer = new PdfRenderer(fileDescriptor);

        PdfRenderer.Page rendererPage = pdfRenderer.openPage(0);
        int rendererPageWidth = rendererPage.getWidth();
        int rendererPageHeight = rendererPage.getHeight();


        Bitmap bitmap = Bitmap.createBitmap(
                rendererPageWidth,
                rendererPageHeight,
                Bitmap.Config.ARGB_8888);
        rendererPage.render(bitmap, null, null,
                PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);


        rendererPage.close();

        pdfRenderer.close();
        fileDescriptor.close();
        return bitmap;
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

                Thread thread = threads.createThread(pid, alias, 0L);

                thread.setName(name);
                thread.setSize(size);
                thread.setMimeType(mimeType);
                CID thumbnail;
                thumbnail = getThumbnail(uri);
                if (thumbnail == null) {
                    thumbnail = getThumbnail(getApplicationContext(), uri, mimeType);
                }
                if (thumbnail != null) {
                    thread.setThumbnail(thumbnail);
                }
                long idx = threads.storeThread(thread);


                try {
                    threads.setThreadLeaching(idx, true);

                    CID cid = ipfs.storeInputStream(inputStream);
                    checkNotNull(cid);

                    // cleanup of entries with same CID and hierarchy
                    List<Thread> sameEntries = threads.getThreadsByContentAndParent(cid, 0L);
                    threads.removeThreads(ipfs, sameEntries);


                    threads.setThreadContent(idx, cid);
                    threads.setThreadSeeding(idx);

                } catch (Throwable e) {
                    threads.removeThreads(ipfs, idx);
                    buildFailedNotification(name);
                    throw e;
                }

            } catch (FileNotFoundException e) {
                events.error(getString(R.string.file_not_found));
            } catch (Throwable e) {
                events.exception(e);
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

    @Nullable
    private CID getThumbnail(@NonNull Uri uri) {
        checkNotNull(uri);

        CID cid = null;
        byte[] bytes = null;


        try {
            bytes = getPreviewImage(uri);
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage());
        }

        if (bytes != null) {
            try {
                cid = IPFS.getInstance(getApplicationContext()).storeData(bytes);
            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }
        }

        return cid;
    }

    @Nullable
    private byte[] getPreviewImage(@NonNull Uri uri) {
        checkNotNull(uri);

        Bitmap bitmap = getBitmapThumbnail(uri);
        if (bitmap != null) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.WEBP, 100, stream);
            byte[] image = stream.toByteArray();
            bitmap.recycle();
            return image;
        }
        return null;
    }

    private Bitmap getBitmapThumbnail(@NonNull Uri uri) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            try {
                return getContentResolver().loadThumbnail(
                        uri, new Size(THUMBNAIL_SIZE, THUMBNAIL_SIZE), null);


            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }
        }
        return null;
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

        builder.setContentTitle(getString(R.string.upload_ipfs_lite));
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
                        .setContentTitle(getString(R.string.upload_ipfs_lite))
                        .setContentText(name)
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
