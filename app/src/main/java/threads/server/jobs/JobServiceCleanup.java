package threads.server.jobs;

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
import java.util.concurrent.TimeUnit;

import threads.server.services.Service;

import static com.google.common.base.Preconditions.checkNotNull;


public class JobServiceCleanup extends JobService {

    private static final String TAG = JobServiceCleanup.class.getSimpleName();

    public static void cleanup(@NonNull Context context) {
        checkNotNull(context);
        JobScheduler jobScheduler = (JobScheduler) context.getApplicationContext()
                .getSystemService(JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            ComponentName componentName = new ComponentName(context, JobServiceCleanup.class);

            JobInfo jobInfo = new JobInfo.Builder(TAG.hashCode(), componentName)
                    .setPeriodic(TimeUnit.HOURS.toMillis(12))
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

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            long start = System.currentTimeMillis();

            try {

                Service.cleanup(getApplicationContext());

            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            } finally {
                Log.e(TAG, " finish onStart [" + (System.currentTimeMillis() - start) + "]...");
                jobFinished(jobParameters, false);
            }
        });

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }
}
