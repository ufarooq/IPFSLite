package threads.server;

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
import java.util.concurrent.TimeUnit;

import threads.core.GatewayService;
import threads.core.Network;
import threads.core.Preferences;
import threads.core.Singleton;
import threads.core.THREADS;
import threads.core.api.Thread;
import threads.ipfs.IPFS;
import threads.ipfs.api.CID;

import static androidx.core.util.Preconditions.checkNotNull;

public class JobServicePublisher extends JobService {

    private static final String TAG = JobServicePublisher.class.getSimpleName();

    public static void publish(@NonNull Context context) {
        checkNotNull(context);

        int time = Service.getPublishServiceTime(context);

        JobScheduler jobScheduler = (JobScheduler) context.getApplicationContext()
                .getSystemService(JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            ComponentName componentName = new ComponentName(context, JobServicePublisher.class);


            JobInfo jobInfo = new JobInfo.Builder(TAG.hashCode(), componentName)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setRequiresCharging(true)
                    .setPeriodic(TimeUnit.HOURS.toMillis(time))
                    .build();

            // cancel a running job with same tag
            jobScheduler.cancel(TAG.hashCode());

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


        if (!Network.isConnectedMinHighBandwidth(getApplicationContext())) {
            return false;
        }


        int timeout = Preferences.getConnectionTimeout(getApplicationContext());
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            long start = System.currentTimeMillis();
            try {

                Service.getInstance(getApplicationContext());


                IPFS ipfs = Singleton.getInstance(getApplicationContext()).getIpfs();

                checkNotNull(ipfs, "IPFS not valid");


                // first notifications stored relays
                GatewayService.connectStoredRelays(
                        getApplicationContext(), "", 20, 3);


                THREADS threads = Singleton.getInstance(getApplicationContext()).getThreads();

                List<CID> contents = new ArrayList<>();
                List<Thread> list = threads.getPinnedThreads();

                for (Thread thread : list) {
                    CID cid = thread.getCid();
                    if (cid != null) {
                        contents.add(cid);
                    }
                }


                for (CID content : contents) {
                    Executors.newSingleThreadExecutor().submit(
                            () -> ipfs.dhtPublish(content, true, timeout));
                }


                for (Thread thread : list) {
                    JobServiceGatewayLoader.loader(getApplicationContext(), thread.getIdx());
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
