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

import threads.core.GatewayService;
import threads.core.Network;
import threads.core.Preferences;
import threads.core.Singleton;
import threads.core.api.Content;
import threads.ipfs.IPFS;
import threads.ipfs.api.CID;

import static androidx.core.util.Preconditions.checkNotNull;

public class JobServicePublish extends JobService {

    private static final String TAG = JobServicePublish.class.getSimpleName();

    public static void publish(@NonNull Context context, @NonNull CID cid, boolean connectStoredRelays) {
        checkNotNull(context);
        checkNotNull(cid);
        JobScheduler jobScheduler = (JobScheduler) context.getApplicationContext()
                .getSystemService(JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            ComponentName componentName = new ComponentName(context, JobServicePublish.class);

            PersistableBundle bundle = new PersistableBundle();
            bundle.putString(Content.CID, cid.getCid());
            bundle.putBoolean(Content.PEERS, connectStoredRelays);

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
        final String cid = bundle.getString(Content.CID);
        checkNotNull(cid);
        final boolean connectPeers = bundle.getBoolean(Content.PEERS);
        if (!Network.isConnectedMinHighBandwidth(getApplicationContext())) {
            return false;
        }
        int timeout = Preferences.getConnectionTimeout(getApplicationContext());

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            long start = System.currentTimeMillis();
            try {

                Service.getInstance(getApplicationContext());


                final IPFS ipfs = Singleton.getInstance(getApplicationContext()).getIpfs();

                checkNotNull(ipfs, "IPFS not valid");

                // first notifications stored relays

                if (connectPeers) {
                    GatewayService.connectStoredRelays(getApplicationContext(), "",
                            20, 3);
                }

                ipfs.dhtPublish(CID.create(cid), true, timeout);


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
