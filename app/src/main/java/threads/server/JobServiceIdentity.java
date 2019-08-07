package threads.server;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import threads.core.IdentityService;
import threads.core.Preferences;
import threads.core.Singleton;
import threads.core.THREADS;
import threads.core.api.Content;
import threads.ipfs.api.PID;

import static androidx.core.util.Preconditions.checkNotNull;

public class JobServiceIdentity extends JobService {
    private static final String TAG = JobServiceIdentity.class.getSimpleName();


    public static void identity(@NonNull Context context) {
        checkNotNull(context);

        JobScheduler jobScheduler = (JobScheduler) context.getApplicationContext()
                .getSystemService(JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            ComponentName componentName = new ComponentName(context, JobServiceIdentity.class);

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
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                final boolean peerDiscovery =
                        threads.server.Service.isSupportPeerDiscovery(getApplicationContext());
                if (peerDiscovery) {
                    THREADS threads = Singleton.getInstance(getApplicationContext()).getThreads();
                    int timeout = Preferences.getConnectionTimeout(getApplicationContext());

                    PID host = Preferences.getPID(getApplicationContext());

                    Map<String, String> params = new HashMap<>();
                    if (host != null) {
                        String alias = threads.getUserAlias(host);
                        params.put(Content.ALIAS, alias);
                    }
                    IdentityService.publishIdentity(getApplicationContext(), BuildConfig.ApiAesKey,
                            params, false, timeout, Service.RELAYS,
                            true);
                }

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
