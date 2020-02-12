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

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import threads.ipfs.CID;
import threads.server.InitApplication;
import threads.server.core.peers.Content;
import threads.server.core.threads.THREADS;
import threads.server.core.threads.Thread;
import threads.server.services.LiteService;

import static androidx.core.util.Preconditions.checkNotNull;

public class JobServicePinLoader extends JobService {

    private static final String TAG = JobServicePinLoader.class.getSimpleName();

    public static void loader(@NonNull Context context, long idx) {
        checkNotNull(context);
        JobScheduler jobScheduler = (JobScheduler) context.getApplicationContext()
                .getSystemService(JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            ComponentName componentName = new ComponentName(context, JobServicePinLoader.class);

            PersistableBundle bundle = new PersistableBundle();
            bundle.putLong(Content.IDX, idx);

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

    private static boolean pinContent(@NonNull URL url, int timeout) {
        checkNotNull(url);
        try {
            URLConnection con = url.openConnection();
            con.setConnectTimeout(15000);
            con.setReadTimeout(timeout * 1000);
            try (InputStream stream = con.getInputStream()) {
                //noinspection StatementWithEmptyBody
                while (stream.read() != -1) {
                }
                return true;
            } catch (Exception e) {
                Log.e(TAG, "" + e.getLocalizedMessage());
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage());
        }
        return false;
    }

    @Override
    public boolean onStartJob(JobParameters jobParameters) {

        PersistableBundle bundle = jobParameters.getExtras();
        final long idx = bundle.getLong(Content.IDX);


        int timeout = InitApplication.getConnectionTimeout(getApplicationContext());

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            long start = System.currentTimeMillis();
            try {
                String gateway = LiteService.getGateway(getApplicationContext());


                THREADS threads = THREADS.getInstance(getApplicationContext());
                List<CID> contents = new ArrayList<>();
                Thread thread = threads.getThreadByIdx(idx);
                checkNotNull(thread);

                CID cid = thread.getContent();
                if (cid != null) {
                    contents.add(cid);
                }


                for (CID content : contents) {
                    long pageTime = System.currentTimeMillis();
                    URL url = new URL(gateway + "/ipfs/" + content);


                    boolean success = pinContent(url, timeout);
                    long time = (System.currentTimeMillis() - pageTime) / 1000;

                    if (success) {
                        Log.e(TAG, "Success publish : " + url.toString() + " " + time + " [s]");
                    } else {
                        Log.e(TAG, "Failed publish : " + url.toString() + " " + time + " [s]");
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
