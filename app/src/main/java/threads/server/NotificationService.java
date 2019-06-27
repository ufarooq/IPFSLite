package threads.server;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import threads.core.Preferences;
import threads.core.api.Content;
import threads.core.api.Server;

import static androidx.core.util.Preconditions.checkNotNull;

public class NotificationService extends JobService {

    private static final String TAG = NotificationService.class.getSimpleName();


    public static void notifications(@NonNull Context context) {
        checkNotNull(context);
        JobScheduler jobScheduler = (JobScheduler) context.getApplicationContext()
                .getSystemService(JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            ComponentName componentName = new ComponentName(context, NotificationService.class);

            JobInfo jobInfo = new JobInfo.Builder(NotificationService.class.hashCode(), componentName)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setPeriodic(JobInfo.getMinPeriodMillis())
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
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }


    @Override
    public boolean onStartJob(JobParameters jobParameters) {

        final String token = Preferences.getToken(getApplicationContext());
        final int timeout = Preferences.getTangleTimeout(getApplicationContext());
        final Server server = Preferences.getTangleServer(getApplicationContext());
        final threads.iota.NotificationService notificationService =
                threads.iota.NotificationService.getInstance(getApplicationContext());
        final Gson gson = new Gson();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {

                List<String> notifications = notificationService.getNotifications(token,
                        server.getProtocol(), server.getHost(), server.getPort(), timeout);
                for (String notification : notifications) {

                    Content data = gson.fromJson(notification, Content.class);
                    if (data != null) {

                        if (data.containsKey(Content.PID) && data.contains(Content.CID)) {
                            final String pid = data.get(Content.PID);
                            checkNotNull(pid);
                            final String cid = data.get(Content.CID);
                            checkNotNull(cid);
                            final String peer = data.get(Content.PEER);
                            DownloadService.download(getApplicationContext(), pid, cid, peer);
                        }

                    }

                }

            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            } finally {
                jobFinished(jobParameters, false);
            }

        });
        return true;
    }

}
