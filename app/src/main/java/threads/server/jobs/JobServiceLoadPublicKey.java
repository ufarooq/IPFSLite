package threads.server.jobs;

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
import threads.core.peers.PEERS;
import threads.ipfs.IPFS;
import threads.ipfs.api.PID;

import static androidx.core.util.Preconditions.checkNotNull;

public class JobServiceLoadPublicKey extends JobService {

    private static final String TAG = JobServiceLoadPublicKey.class.getSimpleName();
    private static final String PEER_ID = "PEER_ID";

    public static void publicKey(@NonNull Context context, @NonNull String pid) {
        checkNotNull(context);
        checkNotNull(pid);

        JobScheduler jobScheduler = (JobScheduler) context.getApplicationContext()
                .getSystemService(JOB_SCHEDULER_SERVICE);

        if (jobScheduler != null) {
            ComponentName componentName = new ComponentName(context, JobServiceLoadPublicKey.class);
            PersistableBundle bundle = new PersistableBundle();
            bundle.putString(PEER_ID, pid);

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
        final String peerID = bundle.getString(PEER_ID);
        checkNotNull(peerID);
        int timeout = Preferences.getConnectionTimeout(getApplicationContext());
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            long start = System.currentTimeMillis();

            try {
                IPFS ipfs = Singleton.getInstance(getApplicationContext()).getIpfs();
                checkNotNull(ipfs, "IPFS not valid");
                PEERS peers = Singleton.getInstance(getApplicationContext()).getPeers();

                PID pid = PID.create(peerID);

                threads.ipfs.api.PeerInfo pInfo = ipfs.id(pid, timeout);
                if (pInfo != null) {
                    String pKey = pInfo.getPublicKey();
                    if (pKey != null) {
                        if (!pKey.isEmpty()) {
                            peers.setUserPublicKey(pid, pKey);
                        }
                    }
                }

            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            } finally {
                Log.e(TAG, " finish running [" + (System.currentTimeMillis() - start) + "]...");
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
