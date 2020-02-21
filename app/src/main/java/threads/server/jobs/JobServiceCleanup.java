package threads.server.jobs;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.util.Preconditions;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import threads.iota.EntityService;
import threads.iota.HashDatabase;
import threads.ipfs.CID;
import threads.ipfs.IPFS;
import threads.server.core.contents.CDS;
import threads.server.core.contents.Content;
import threads.server.core.contents.ContentDatabase;
import threads.server.services.LiteService;

import static com.google.common.base.Preconditions.checkNotNull;


public class JobServiceCleanup extends JobService {

    private static final String TAG = JobServiceCleanup.class.getSimpleName();

    public static void cleanup(@NonNull Context context) {
        checkNotNull(context);
        JobScheduler jobScheduler = (JobScheduler) context.getApplicationContext()
                .getSystemService(JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            ComponentName componentName = new ComponentName(context, JobServiceCleanup.class);

            JobInfo jobInfo = new JobInfo.Builder(TAG.hashCode(), componentName)
                    .setPeriodic(TimeUnit.HOURS.toMillis(12))
                    .build();
            int resultCode = jobScheduler.schedule(jobInfo);
            if (resultCode == JobScheduler.RESULT_SUCCESS) {
                Log.e(TAG, "Job scheduled!");
            } else {
                Log.e(TAG, "Job not scheduled");
            }
        }
    }

    private static long getDaysAgo(int days) {
        return System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days);
    }

    @Override
    public boolean onStartJob(JobParameters jobParameters) {

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            long start = System.currentTimeMillis();

            try {
                // LITE service is necessary to start the daemon
                LiteService.getInstance(getApplicationContext());
                CDS contentService = CDS.getInstance(getApplicationContext());
                EntityService entityService = EntityService.getInstance(getApplicationContext());
                IPFS ipfs = IPFS.getInstance(getApplicationContext());

                // remove all old hashes from hash database
                HashDatabase hashDatabase = entityService.getHashDatabase();
                long timestamp = getDaysAgo(28);
                hashDatabase.hashDao().removeAllHashesWithSmallerTimestamp(timestamp);


                // remove all content
                timestamp = getDaysAgo(2);
                ContentDatabase contentDatabase = contentService.getContentDatabase();
                List<Content> entries = contentDatabase.contentDao().
                        getContentWithSmallerTimestamp(timestamp);

                Preconditions.checkNotNull(ipfs, "IPFS not valid");

                try {
                    for (threads.server.core.contents.Content content : entries) {

                        contentDatabase.contentDao().removeContent(content);

                        CID cid = content.getCID();
                        ipfs.rm(cid);
                    }
                } finally {
                    ipfs.gc();
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
