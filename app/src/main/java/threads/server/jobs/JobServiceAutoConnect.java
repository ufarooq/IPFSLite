package threads.server.jobs;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import threads.core.Preferences;
import threads.core.peers.PEERS;
import threads.core.peers.User;
import threads.ipfs.IPFS;
import threads.server.Service;
import threads.share.Network;

import static androidx.core.util.Preconditions.checkNotNull;

public class JobServiceAutoConnect extends JobService {


    private static final String TAG = JobServiceAutoConnect.class.getSimpleName();


    public static void autoConnect(@NonNull Context context) {
        checkNotNull(context);

        JobScheduler jobScheduler = (JobScheduler) context.getApplicationContext()
                .getSystemService(JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            ComponentName componentName = new ComponentName(context, JobServiceAutoConnect.class);

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

        final int timeout = Preferences.getConnectionTimeout(getApplicationContext());

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            long start = System.currentTimeMillis();

            try {


                Service.getInstance(getApplicationContext());


                IPFS ipfs = IPFS.getInstance(getApplicationContext());
                checkNotNull(ipfs, "IPFS not defined");

                PEERS peers = PEERS.getInstance(getApplicationContext());
                List<User> users = peers.getAutoConnectUsers();

                ExecutorService executorService = Executors.newFixedThreadPool(5);
                List<Future> futures = new ArrayList<>();

                for (User user : users) {
                    if (!ipfs.isConnected(user.getPID())) {

                        ipfs.connectPubsubTopic(
                                getApplicationContext(), user.getPID().getPid());


                        futures.add(executorService.submit(() ->
                                ipfs.swarmConnect(user.getPID(), timeout)));
                    }
                }

                for (Future future : futures) {
                    future.get();
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

