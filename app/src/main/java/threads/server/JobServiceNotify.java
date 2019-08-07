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

public class JobServiceNotify extends JobService {

    private static final String TAG = JobServiceNotify.class.getSimpleName();


    public static void notify(@NonNull Context context,
                              @NonNull String pid,
                              @NonNull String cid) {
        checkNotNull(context);
        checkNotNull(pid);
        checkNotNull(cid);
        JobScheduler jobScheduler = (JobScheduler) context.getApplicationContext()
                .getSystemService(JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            ComponentName componentName = new ComponentName(context, JobServiceNotify.class);

            PersistableBundle bundle = new PersistableBundle();
            bundle.putString(Content.PID, pid);
            bundle.putString(Content.CID, cid);

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

        if (!Service.isSendNotificationsEnabled(getApplicationContext())) {
            return false;
        }

        final int timeout = Preferences.getConnectionTimeout(getApplicationContext());
        final PID host = Preferences.getPID(getApplication());
        final boolean peerDiscovery = Service.isSupportPeerDiscovery(getApplicationContext());
        final long startTime = System.currentTimeMillis();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {

            try {

                Service.getInstance(getApplicationContext());
                final THREADS threads = Singleton.getInstance(getApplicationContext()).getThreads();

                boolean success = false;
                if (peerDiscovery) {

                    Map<String, String> params = new HashMap<>();
                    if (host != null) {
                        String alias = threads.getUserAlias(host);
                        params.put(Content.ALIAS, alias);
                    }
                    success = IdentityService.publishIdentity(
                            getApplicationContext(), BuildConfig.ApiAesKey, params, false,
                            timeout, Service.RELAYS, true);
                }

                String hash = null;
                if (success) {
                    if (host != null) {
                        hash = threads.getPeerInfoHash(host);
                    }
                }

                Service.notify(getApplicationContext(), pid, cid, hash, startTime);


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
