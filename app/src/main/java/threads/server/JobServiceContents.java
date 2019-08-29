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

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import threads.core.Network;
import threads.core.api.Content;
import threads.ipfs.api.CID;
import threads.ipfs.api.PID;

import static androidx.core.util.Preconditions.checkNotNull;

public class JobServiceContents extends JobService {

    private static final String TAG = JobServiceContents.class.getSimpleName();

    public static void contents(@NonNull Context context,
                                @NonNull PID pid,
                                @NonNull CID cid) {
        checkNotNull(context);
        checkNotNull(pid);
        checkNotNull(cid);
        JobScheduler jobScheduler = (JobScheduler) context.getApplicationContext()
                .getSystemService(JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            ComponentName componentName = new ComponentName(context, JobServiceContents.class);

            PersistableBundle bundle = new PersistableBundle();
            bundle.putString(threads.core.api.Content.PID, pid.getPid());
            bundle.putString(Content.CID, cid.getCid());

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

    private static long getMinutesAgo(int minutes) {
        return System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(minutes);
    }


    @Override
    public boolean onStartJob(JobParameters jobParameters) {

        PersistableBundle bundle = jobParameters.getExtras();
        final String pid = bundle.getString(Content.PID);
        checkNotNull(pid);
        final String cid = bundle.getString(Content.CID);
        checkNotNull(cid);

        if (!Network.isConnectedMinHighBandwidth(getApplicationContext())) {
            return false;
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            long start = System.currentTimeMillis();

            try {
                Service.getInstance(getApplicationContext());

                final ContentService contentService =
                        ContentService.getInstance(getApplicationContext());
                boolean connected = ContentsService.download(getApplicationContext(),
                        PID.create(pid), CID.create(cid));

                if (connected) {
                    // load old entries when connected
                    long timestamp = getMinutesAgo(30);

                    List<threads.server.Content> contents =
                            contentService.getContentDatabase().
                                    contentDao().getContents(
                                    PID.create(pid), timestamp, false);

                    for (threads.server.Content entry : contents) {
                        ContentsService.download(
                                getApplicationContext(), entry.getPid(), entry.getCID());
                    }
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
