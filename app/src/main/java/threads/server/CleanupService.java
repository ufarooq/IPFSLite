package threads.server;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import threads.core.Singleton;
import threads.iota.EntityService;
import threads.iota.HashDatabase;
import threads.ipfs.IPFS;
import threads.ipfs.api.CID;

import static androidx.core.util.Preconditions.checkNotNull;

public class CleanupService extends JobService {

    private static final String TAG = CleanupService.class.getSimpleName();


    public static void cleanup(@NonNull Context context) {
        checkNotNull(context);
        JobScheduler jobScheduler = (JobScheduler) context.getApplicationContext()
                .getSystemService(JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            ComponentName componentName = new ComponentName(context, CleanupService.class);

            JobInfo jobInfo = new JobInfo.Builder(CleanupService.class.hashCode(), componentName)
                    .setPeriodic(TimeUnit.HOURS.toMillis(12))
                    .setPersisted(true)
                    .build();
            int resultCode = jobScheduler.schedule(jobInfo);
            if (resultCode == JobScheduler.RESULT_SUCCESS) {
                Log.e(TAG, "Job scheduled!");
            } else {
                Log.e(TAG, "Job not scheduled");
            }
        }
    }

    public static long getDaysAgo(int days) {
        return System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days);
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }

    @Override
    public boolean onStartJob(JobParameters jobParameters) {

        Service.getInstance(getApplicationContext());
        final ContentService contentService = ContentService.getInstance(getApplicationContext());
        final EntityService entityService = EntityService.getInstance(getApplicationContext());
        final IPFS ipfs = Singleton.getInstance(getApplicationContext()).getIpfs();
        if (ipfs != null) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {

                    // remove all old hashes from hash database
                    HashDatabase hashDatabase = entityService.getHashDatabase();
                    long timestamp = getDaysAgo(28);
                    hashDatabase.hashDao().removeAllHashesWithSmallerTimestamp(timestamp);


                    // remove all content
                    timestamp = getDaysAgo(14);
                    ContentDatabase contentDatabase = contentService.getContentDatabase();
                    List<Content> entries = contentDatabase.contentDao().
                            getContentWithSmallerTimestamp(timestamp);
                    for (Content content : entries) {

                        contentDatabase.contentDao().removeContent(content);

                        CID cid = content.getCID();
                        ipfs.rm(cid);
                    }

                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
                } finally {
                    jobFinished(jobParameters, false);
                }
            });
            return true;
        }
        return false;

    }
}
