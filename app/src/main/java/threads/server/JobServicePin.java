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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import threads.core.api.Content;

import static androidx.core.util.Preconditions.checkNotNull;

public class JobServicePin extends JobService {

    private static final String TAG = JobServicePin.class.getSimpleName();


    public static void pin(@NonNull Context context, @NonNull String cid) {
        checkNotNull(context);
        checkNotNull(cid);
        JobScheduler jobScheduler = (JobScheduler) context.getApplicationContext()
                .getSystemService(JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            ComponentName componentName = new ComponentName(context, JobServicePin.class);

            PersistableBundle bundle = new PersistableBundle();
            bundle.putString(threads.core.api.Content.CID, cid);

            JobInfo jobInfo = new JobInfo.Builder(cid.hashCode(), componentName)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setRequiresCharging(true)
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

    private static boolean pinContent(@NonNull URL url) {
        checkNotNull(url);
        try (InputStream stream = url.openStream()) {
            while (stream.read() != -1) {
            }
            return true;
        } catch (FileNotFoundException e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        } catch (IOException e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
        return false;
    }

    @Override
    public boolean onStartJob(JobParameters jobParameters) {

        PersistableBundle bundle = jobParameters.getExtras();

        final String cid = bundle.getString(Content.CID);
        checkNotNull(cid);


        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {

            long start = System.currentTimeMillis();
            try {
                URL url = new URL("https://ipfs.io/ipfs/" + cid);
                boolean success = pinContent(url);
                long time = (System.currentTimeMillis() - start) / 1000;

                if (success) {
                    Log.e(TAG, "Success pin : " + url.toString() + " " + time + " [s]");
                } else {
                    Log.e(TAG, "Failed pin : " + url.toString() + " " + time + " [s]");
                }
            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            } finally {
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
