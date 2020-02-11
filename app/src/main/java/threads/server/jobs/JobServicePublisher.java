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
import java.util.concurrent.TimeUnit;

import threads.ipfs.CID;
import threads.ipfs.IPFS;
import threads.server.core.threads.THREADS;
import threads.server.core.threads.Thread;
import threads.server.services.BootstrapService;
import threads.server.services.LiteService;
import threads.server.utils.Preferences;
import threads.server.work.ConnectPeersWorker;

import static androidx.core.util.Preconditions.checkNotNull;

public class JobServicePublisher extends JobService {

    private static final String TAG = JobServicePublisher.class.getSimpleName();

    public static void publish(@NonNull Context context) {
        checkNotNull(context);

        int time = LiteService.getPublishServiceTime(context);

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


        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            long start = System.currentTimeMillis();
            try {

                IPFS ipfs = IPFS.getInstance(getApplicationContext());
                int timeout = Preferences.getConnectionTimeout(getApplicationContext());


                THREADS threads = THREADS.getInstance(getApplicationContext());
                List<CID> contents = new ArrayList<>();
                List<Thread> list = threads.getPinnedThreads();

                for (Thread thread : list) {
                    CID cid = thread.getContent();
                    if (cid != null) {
                        contents.add(cid);
                    }
                }

                if (!list.isEmpty()) {
                    BootstrapService.bootstrap(getApplicationContext());
                    ConnectPeersWorker.connect(getApplicationContext(), 10);
                }

                for (CID content : contents) {
                    Executors.newSingleThreadExecutor().submit(
                            () -> ipfs.dhtPublish(content, true, timeout));
                }


                for (Thread thread : list) {
                    JobServicePinLoader.loader(getApplicationContext(), thread.getIdx());
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
