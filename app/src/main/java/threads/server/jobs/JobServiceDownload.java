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

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import threads.ipfs.CID;
import threads.server.core.peers.Content;
import threads.server.core.threads.THREADS;
import threads.server.core.threads.Thread;
import threads.server.services.LiteService;
import threads.server.work.DownloadThreadWorker;

import static androidx.core.util.Preconditions.checkNotNull;

public class JobServiceDownload extends JobService {

    private static final String TAG = JobServiceDownload.class.getSimpleName();


    public static void download(@NonNull Context context, @NonNull CID cid) {
        checkNotNull(context);

        checkNotNull(cid);
        JobScheduler jobScheduler = (JobScheduler) context.getApplicationContext()
                .getSystemService(JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            ComponentName componentName = new ComponentName(context, JobServiceDownload.class);

            PersistableBundle bundle = new PersistableBundle();
            bundle.putString(Content.CID, cid.getCid());

            JobInfo jobInfo = new JobInfo.Builder(cid.hashCode(), componentName)
                    .setOverrideDeadline(0)
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
        final String cidStr = bundle.getString(Content.CID);
        checkNotNull(cidStr);


        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            long start = System.currentTimeMillis();
            try {

                CID cid = CID.create(cidStr);

                THREADS threads = THREADS.getInstance(getApplicationContext());


                List<Thread> entries = threads.getThreadsByContentAndParent(cid, 0L);

                if (!entries.isEmpty()) {
                    Thread entry = entries.get(0);
                    if (!entry.isDeleting() && !entry.isSeeding()) {
                        threads.setThreadLeaching(entry.getIdx(), true);
                        DownloadThreadWorker.download(getApplicationContext(), entry.getIdx());
                    }
                } else {
                    long idx = LiteService.createThread(getApplicationContext(), cid,
                            null, 0L, null);

                    threads.setThreadLeaching(idx, true);
                    DownloadThreadWorker.download(getApplicationContext(), idx);
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
