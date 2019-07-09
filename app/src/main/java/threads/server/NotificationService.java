package threads.server;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static androidx.core.util.Preconditions.checkNotNull;

public class NotificationService extends JobService {

    private static final String TAG = NotificationService.class.getSimpleName();


    public static void notifications(@NonNull Context context) {
        checkNotNull(context);
        JobScheduler jobScheduler = (JobScheduler) context.getApplicationContext()
                .getSystemService(JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            ComponentName componentName = new ComponentName(context, NotificationService.class);

            JobInfo jobInfo = new JobInfo.Builder(TAG.hashCode(), componentName)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
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

        if (!Service.isSupportOfflineNotification(getApplicationContext())) {
            return false;
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {

                Service.getInstance(getApplicationContext());

                Service.notifications(getApplicationContext());

            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            } finally {
                jobFinished(jobParameters, false);
            }

        });
        return true;

    }

}
