package threads.server;

import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.PersistableBundle;
import android.util.Log;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import threads.core.IThreadsAPI;
import threads.core.Preferences;
import threads.core.Singleton;
import threads.ipfs.IPFS;
import threads.ipfs.api.CID;
import threads.ipfs.api.Link;

import static com.google.common.base.Preconditions.checkNotNull;

public class DownloadJobService extends JobService {

    private static final String TAG = DownloadJobService.class.getSimpleName();
    private static final String CID_KEY = "CID_KEY";


    public static void download(@NonNull Context context, @NonNull String cid) {
        checkNotNull(context);
        checkNotNull(cid);

        JobScheduler jobScheduler = (JobScheduler) context.getApplicationContext()
                .getSystemService(JOB_SCHEDULER_SERVICE);

        if (jobScheduler != null) {
            ComponentName componentName = new ComponentName(context, DownloadJobService.class);
            PersistableBundle bundle = new PersistableBundle();
            bundle.putString(CID_KEY, cid);

            JobInfo jobInfo = new JobInfo.Builder(new Random().nextInt(), componentName)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setExtras(bundle)
                    .build();
            int resultCode = jobScheduler.schedule(jobInfo);
            if (resultCode == JobScheduler.RESULT_SUCCESS) {
                Log.e(TAG, "Job scheduled!");
            } else {
                Log.e(TAG, "Job not scheduled");
            }
        }
    }

    @Override
    public boolean onStartJob(JobParameters jobParameters) {

        PersistableBundle bundle = jobParameters.getExtras();
        DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        checkNotNull(downloadManager);
        final IThreadsAPI threadsAPI = Singleton.getInstance().getThreadsAPI();
        final String cid = bundle.getString(DownloadJobService.CID_KEY);
        checkNotNull(cid);
        final IPFS ipfs = Singleton.getInstance().getIpfs();
        if (ipfs != null) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    CID cidLink = CID.create(cid);
                    List<Link> links = ipfs.ls(cidLink);
                    Link link = links.get(0);


                    NotificationCompat.Builder builder =
                            NotificationSender.createDownloadProgressNotification(
                                    getApplicationContext(), link.getPath());

                    final NotificationManager notificationManager = (NotificationManager)
                            getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
                    int notifyID = NotificationSender.NOTIFICATIONS_COUNTER.incrementAndGet();
                    Notification notification = builder.build();
                    if (notificationManager != null) {
                        notificationManager.notify(notifyID, notification);
                    }


                    try {
                        threadsAPI.preload(ipfs, link.getCid().getCid(), link.getSize(), (percent) -> {


                            builder.setProgress(100, percent, false);
                            if (notificationManager != null) {
                                notificationManager.notify(notifyID, builder.build());
                            }

                        });

                    } finally {

                        if (notificationManager != null) {
                            notificationManager.cancel(notifyID);
                        }
                    }


                    NotificationSender.showLinkNotification(getApplicationContext(), link);


                    Uri uri = Uri.parse(Preferences.getGateway(getApplicationContext()) + cid);

                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);

                } catch (Throwable e) {
                    Preferences.evaluateException(Preferences.EXCEPTION, e);
                }
            });
        }
        return true;
    }


    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }
}