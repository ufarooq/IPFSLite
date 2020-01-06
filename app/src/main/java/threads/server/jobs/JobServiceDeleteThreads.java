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

import threads.core.Singleton;
import threads.core.threads.Status;
import threads.core.threads.THREADS;
import threads.ipfs.IPFS;

import static androidx.core.util.Preconditions.checkNotNull;

public class JobServiceDeleteThreads extends JobService {


    private static final String TAG = JobServiceDeleteThreads.class.getSimpleName();
    private static final String ICES = "ices";

    public static void removeThreads(@NonNull Context context, long... idxs) {
        checkNotNull(context);

        JobScheduler jobScheduler = (JobScheduler) context.getApplicationContext()
                .getSystemService(JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            ComponentName componentName = new ComponentName(context, JobServiceDeleteThreads.class);
            PersistableBundle bundle = new PersistableBundle();
            bundle.putLongArray(ICES, idxs);

            JobInfo jobInfo = new JobInfo.Builder(TAG.hashCode(), componentName)
                    .setExtras(bundle)
                    .setOverrideDeadline(0)
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
        final long[] idxs = bundle.getLongArray(ICES);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            long start = System.currentTimeMillis();

            try {

                Singleton.getInstance(getApplicationContext());


                THREADS threads = Singleton.getInstance(getApplicationContext()).getThreads();
                IPFS ipfs = Singleton.getInstance(getApplicationContext()).getIpfs();

                checkNotNull(ipfs, "IPFS is not valid");
                threads.setThreadsStatus(Status.DELETING, idxs);

                threads.removeThreads(ipfs, idxs);

                ipfs.gc();

                ipfs.logBaseDir();

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

