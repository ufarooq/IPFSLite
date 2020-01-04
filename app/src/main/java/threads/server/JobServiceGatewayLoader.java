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

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import threads.core.GatewayService;
import threads.core.Network;
import threads.core.Preferences;
import threads.core.Singleton;
import threads.core.THREADS;
import threads.core.api.Thread;
import threads.ipfs.api.CID;

import static androidx.core.util.Preconditions.checkNotNull;

public class JobServiceGatewayLoader extends JobService {

    private static final String TAG = JobServiceGatewayLoader.class.getSimpleName();
    private static final String IDX = "IDX";

    public static void loader(@NonNull Context context, long idx) {
        checkNotNull(context);
        JobScheduler jobScheduler = (JobScheduler) context.getApplicationContext()
                .getSystemService(JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            ComponentName componentName = new ComponentName(context, JobServiceGatewayLoader.class);

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
        final long idx = bundle.getLong(IDX);

        if (!Network.isConnected(getApplicationContext())) {
            return false;
        }
        int timeout = Preferences.getConnectionTimeout(getApplicationContext());

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            long start = System.currentTimeMillis();
            try {

                Service.getInstance(getApplicationContext());

                String gateway = Service.getGateway(getApplicationContext());


                THREADS threads = Singleton.getInstance(getApplicationContext()).getThreads();

                List<CID> contents = new ArrayList<>();
                Thread thread = threads.getThreadByIdx(idx);
                checkNotNull(thread);

                CID cid = thread.getCid();
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

                GatewayService.evaluatePeers(getApplicationContext(), false);

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
