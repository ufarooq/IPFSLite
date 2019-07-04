package threads.server;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.os.PersistableBundle;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import threads.core.api.Content;

import static androidx.core.util.Preconditions.checkNotNull;

public class NotifyService extends JobService {

    private static final String TAG = NotifyService.class.getSimpleName();


    public static void notify(@NonNull Context context,
                              @NonNull String pid,
                              @NonNull String cid,
                              @NonNull Integer est) {
        checkNotNull(context);
        checkNotNull(pid);
        checkNotNull(cid);
        checkNotNull(est);
        JobScheduler jobScheduler = (JobScheduler) context.getApplicationContext()
                .getSystemService(JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            ComponentName componentName = new ComponentName(context, NotifyService.class);

            PersistableBundle bundle = new PersistableBundle();
            bundle.putString(Content.PID, pid);
            bundle.putString(Content.CID, cid);
            bundle.putInt(Content.EST, est);
            JobInfo jobInfo = new JobInfo.Builder(cid.hashCode(), componentName)
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
        final String pid = bundle.getString(Content.PID);
        checkNotNull(pid);
        final String cid = bundle.getString(Content.CID);
        checkNotNull(cid);
        final Integer est = bundle.getInt(Content.EST);
        checkNotNull(est);

        if (!Service.isSupportOfflineNotification(getApplicationContext())) {
            return false;
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                Service.notitfy(getApplicationContext(), pid, cid, est);
            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            } finally {
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
