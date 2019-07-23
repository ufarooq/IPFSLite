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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import threads.core.Singleton;
import threads.core.api.Content;
import threads.ipfs.IPFS;
import threads.ipfs.api.CID;

import static androidx.core.util.Preconditions.checkNotNull;

public class JobServiceView extends JobService {

    private static final int TIMEOUT = 120000;
    private static final String TAG = JobServicePin.class.getSimpleName();


    public static void view(@NonNull Context context, @NonNull String cid) {
        checkNotNull(context);
        checkNotNull(cid);
        JobScheduler jobScheduler = (JobScheduler) context.getApplicationContext()
                .getSystemService(JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            ComponentName componentName = new ComponentName(context, JobServiceView.class);

            PersistableBundle bundle = new PersistableBundle();
            bundle.putString(threads.core.api.Content.CID, cid);

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


    @Override
    public boolean onStartJob(JobParameters jobParameters) {

        PersistableBundle bundle = jobParameters.getExtras();

        final String cid = bundle.getString(threads.core.api.Content.CID);
        checkNotNull(cid);


        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {


            try {
                Service.getInstance(getApplicationContext());

                final IPFS ipfs = Singleton.getInstance(getApplicationContext()).getIpfs();

                checkNotNull(ipfs, "IPFS not valid");
                ipfs.dhtPublish(CID.create(cid), true, TIMEOUT);

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

        PersistableBundle bundle = jobParameters.getExtras();

        final String cid = bundle.getString(Content.CID);
        checkNotNull(cid);

        Log.e(TAG, "onStopJob " + cid);
        return false;
    }
}
