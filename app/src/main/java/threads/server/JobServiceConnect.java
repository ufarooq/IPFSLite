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

import threads.core.Preferences;
import threads.core.Singleton;
import threads.core.peers.Content;
import threads.ipfs.IPFS;
import threads.ipfs.api.PID;
import threads.share.Network;

import static androidx.core.util.Preconditions.checkNotNull;

public class JobServiceConnect extends JobService {


    private static final String TAG = JobServiceConnect.class.getSimpleName();


    public static void connect(@NonNull Context context, @NonNull PID pid) {
        checkNotNull(context);

        JobScheduler jobScheduler = (JobScheduler) context.getApplicationContext()
                .getSystemService(JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            ComponentName componentName = new ComponentName(context, JobServiceConnect.class);

            PersistableBundle bundle = new PersistableBundle();
            bundle.putString(Content.PID, pid.getPid());
            JobInfo jobInfo = new JobInfo.Builder(pid.hashCode(), componentName)
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

        if (!Network.isConnected(getApplicationContext())) {
            return false;
        }

        final int timeout = Preferences.getConnectionTimeout(getApplicationContext());

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            long start = System.currentTimeMillis();

            try {


                Service.getInstance(getApplicationContext());

                Singleton singleton = Singleton.getInstance(getApplicationContext());

                IPFS ipfs = singleton.getIpfs();
                checkNotNull(ipfs, "IPFS not defined");

                if (!ipfs.isConnected(PID.create(pid))) {

                    singleton.connectPubsubTopic(getApplicationContext(), pid);

                    ipfs.swarmConnect(PID.create(pid), timeout);
                }

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
