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

import threads.share.GatewayService;
import threads.share.Network;

import static androidx.core.util.Preconditions.checkNotNull;

public class JobServicePeers extends JobService {


    private static final String TAG = JobServicePeers.class.getSimpleName();


    public static void peers(@NonNull Context context) {
        checkNotNull(context);

        JobScheduler jobScheduler = (JobScheduler) context.getApplicationContext()
                .getSystemService(JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            ComponentName componentName = new ComponentName(context, JobServicePeers.class);

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
    public boolean onStartJob(JobParameters jobParameters) {

        if (!Network.isConnected(getApplicationContext())) {
            return false;
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            long start = System.currentTimeMillis();

            try {
                Service.getInstance(getApplicationContext());

                GatewayService.connectStoredAutonat(getApplicationContext(),
                        3, 3);

                GatewayService.connectStoredRelays(getApplicationContext(), "",
                        Service.RELAYS, 3);

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
