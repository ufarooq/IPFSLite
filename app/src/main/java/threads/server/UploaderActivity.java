package threads.server;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import threads.core.threads.Kind;
import threads.core.threads.Status;
import threads.core.threads.THREADS;
import threads.core.threads.Thread;
import threads.ipfs.IPFS;
import threads.ipfs.api.CID;
import threads.ipfs.api.PID;
import threads.server.jobs.JobServiceDownload;
import threads.share.MimeType;
import threads.share.ThumbnailService;

import static androidx.core.util.Preconditions.checkNotNull;

public class UploaderActivity extends Activity {
    private static final String TAG = UploaderActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        handleIntents();

        finish();

    }

    private void handleIntents() {

        Intent intent = getIntent();
        final String action = intent.getAction();


        try {

            if (Intent.ACTION_SEND.equals(action)) {
                String type = intent.getType();
                if ("text/plain".equals(type)) {
                    handleSendText(intent);
                } else {
                    handleSend(intent, false);
                }
            } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
                if (intent.hasExtra(Intent.EXTRA_STREAM)) {
                    handleSend(intent, true);
                } else {
                    String type = intent.getType();
                    if ("text/plain".equals(type)) {
                        handleSendText(intent);
                    } else {
                        handleSend(intent, true);
                    }
                }
            }

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage());
        }


    }

    private void handleSendText(Intent intent) {

        try {
            String text = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (text != null && !text.isEmpty()) {

                CodecDecider codecDecider = CodecDecider.evaluate(text);

                if (codecDecider.getCodex() == CodecDecider.Codec.MULTIHASH ||
                        codecDecider.getCodex() == CodecDecider.Codec.URI) {

                    PID host = IPFS.getPID(getApplicationContext());
                    checkNotNull(host);
                    String multihash = codecDecider.getMultihash();

                    JobServiceDownload.download(getApplicationContext(),
                            host, CID.create(multihash));
                } else {
                    storeText(text);
                }
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    private void handleSend(Intent intent, boolean multi) {

        try {
            Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (multi) {
                ClipData mClipData = intent.getClipData();
                if (mClipData != null) {
                    for (int i = 0; i < mClipData.getItemCount(); i++) {
                        ClipData.Item item = mClipData.getItemAt(i);
                        storeData(item.getUri());
                    }
                } else if (uri != null) {
                    storeData(uri);
                }

            } else if (uri != null) {
                storeData(uri);
            }


        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }

    }


    public void storeData(@NonNull Uri uri) {

        checkNotNull(uri);

        final THREADS threads = THREADS.getInstance(getApplicationContext());
        ThumbnailService.FileDetails fileDetails = ThumbnailService.getFileDetails(getApplicationContext(), uri);
        if (fileDetails == null) {
            // TODO add message
            return;
        }

        final NotificationCompat.Builder builder =
                ProgressChannel.createProgressNotification(
                        getApplicationContext(), fileDetails.getFileName());


        final NotificationManager notificationManager = (NotificationManager)
                getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        final int notifyID = uri.hashCode();
        Notification notification = builder.build();
        if (notificationManager != null) {
            notificationManager.notify(notifyID, notification);
        }

        final IPFS ipfs = IPFS.getInstance(getApplicationContext());

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                checkNotNull(ipfs, "IPFS is not valid");
                InputStream inputStream = getContentResolver().openInputStream(uri);
                checkNotNull(inputStream);

                PID pid = IPFS.getPID(getApplicationContext());
                checkNotNull(pid);
                String alias = IPFS.getDeviceName();
                checkNotNull(alias);


                String name = fileDetails.getFileName();
                long size = fileDetails.getFileSize();

                Thread thread = threads.createThread(pid, alias,
                        Status.INIT, Kind.IN, 0L);

                ThumbnailService.Result res =
                        ThumbnailService.getThumbnail(getApplicationContext(), uri);

                thread.setName(name);
                thread.setSize(size);
                thread.setThumbnail(res.getCid());
                thread.setMimeType(fileDetails.getMimeType());
                long idx = threads.storeThread(thread);


                try {
                    threads.setThreadLeaching(idx, true);

                    CID cid = ipfs.storeStream(inputStream, true);
                    checkNotNull(cid);

                    // cleanup of entries with same CID and hierarchy
                    List<Thread> sameEntries = threads.getThreadsByCIDAndThread(cid, 0L);
                    threads.removeThreads(ipfs, sameEntries);


                    threads.setThreadContent(idx, cid);
                    threads.setThreadStatus(idx, Status.DONE);

                    builder.setProgress(100, 100, false);
                    if (notificationManager != null) {
                        notificationManager.notify(notifyID, builder.build());
                    }
                } catch (Throwable e) {
                    threads.setThreadStatus(idx, Status.ERROR);
                    throw e;
                } finally {
                    threads.setThreadLeaching(idx, false);
                }

            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }
        });
    }


    void storeText(@NonNull String text) {
        checkNotNull(text);

        final THREADS threads = THREADS.getInstance(getApplicationContext());
        final IPFS ipfs = IPFS.getInstance(getApplicationContext());

        final NotificationCompat.Builder builder =
                ProgressChannel.createProgressNotification(
                        getApplicationContext(), text);


        final NotificationManager notificationManager = (NotificationManager)
                getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        final int notifyID = text.hashCode();
        Notification notification = builder.build();
        if (notificationManager != null) {
            notificationManager.notify(notifyID, notification);
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {

                PID pid = IPFS.getPID(getApplicationContext());
                checkNotNull(pid);
                String alias = IPFS.getDeviceName();
                checkNotNull(alias);

                String mimeType = MimeType.PLAIN_MIME_TYPE;

                long size = text.length();

                Thread thread = threads.createThread(pid, alias,
                        Status.INIT, Kind.IN, 0L);
                thread.setSize(size);
                thread.setMimeType(mimeType);

                long idx = threads.storeThread(thread);


                try {
                    threads.setThreadLeaching(idx, true);

                    CID cid = ipfs.storeText(text, "", true);
                    checkNotNull(cid);


                    // cleanup of entries with same CID and hierarchy
                    List<Thread> sameEntries = threads.getThreadsByCIDAndThread(cid, 0L);
                    threads.removeThreads(ipfs, sameEntries);

                    threads.setThreadName(idx, cid.getCid());
                    threads.setThreadContent(idx, cid);
                    threads.setThreadStatus(idx, Status.DONE);

                    builder.setProgress(100, 100, false);
                    if (notificationManager != null) {
                        notificationManager.notify(notifyID, builder.build());
                    }

                } catch (Throwable e) {
                    threads.setThreadStatus(idx, Status.ERROR);
                } finally {
                    threads.setThreadLeaching(idx, false);
                }

            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }
        });
    }

}
