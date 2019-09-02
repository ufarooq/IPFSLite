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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import threads.core.GatewayService;
import threads.core.Network;
import threads.core.Preferences;
import threads.core.Singleton;
import threads.core.THREADS;
import threads.ipfs.IPFS;
import threads.ipfs.api.CID;

import static androidx.core.util.Preconditions.checkNotNull;

public class JobServicePublish extends JobService {

    private static final String TAG = JobServicePublish.class.getSimpleName();
    private static final String IDX = "IDX";

    public static void pin(@NonNull Context context, long idx) {
        checkNotNull(context);
        JobScheduler jobScheduler = (JobScheduler) context.getApplicationContext()
                .getSystemService(JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            ComponentName componentName = new ComponentName(context, JobServicePublish.class);

            PersistableBundle bundle = new PersistableBundle();
            bundle.putLong(IDX, idx);

            JobInfo jobInfo = new JobInfo.Builder((int) idx, componentName)
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
        final long idx = bundle.getLong(IDX);

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
                GatewayService.connectStoredRelays(getApplicationContext(), 20, 3);

                THREADS threads = Singleton.getInstance(getApplicationContext()).getThreads();

                List<CID> contents = new ArrayList<>();

                CID cid = threads.getThreadCID(idx);
                if (cid != null) {
                    contents.add(cid);
                }

                for (CID content : contents) {
                    Executors.newSingleThreadExecutor().submit(
                            () -> ipfs.dhtPublish(content, true, timeout));
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
