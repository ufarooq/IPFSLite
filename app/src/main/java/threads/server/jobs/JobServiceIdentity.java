package threads.server.jobs;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import threads.iota.EntityService;
import threads.ipfs.IPFS;
import threads.ipfs.PID;
import threads.server.InitApplication;
import threads.server.core.peers.AddressType;
import threads.server.core.peers.Content;

import static androidx.core.util.Preconditions.checkNotNull;

public class JobServiceIdentity extends JobService {
    private static final String TAG = JobServiceIdentity.class.getSimpleName();

    public static void publish(@NonNull Context context) {
        checkNotNull(context);

        JobScheduler jobScheduler = (JobScheduler) context.getApplicationContext()
                .getSystemService(JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            ComponentName componentName = new ComponentName(context, JobServiceIdentity.class);

            JobInfo jobInfo = new JobInfo.Builder(TAG.hashCode(), componentName)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
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

        boolean done = InitApplication.getLoginFlag(getApplicationContext());
        if (done) {
            return false;
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            long start = System.currentTimeMillis();

            try {

                PID host = IPFS.getPID(getApplicationContext());
                checkNotNull(host);
                String alias = IPFS.getDeviceName();
                checkNotNull(alias);
                IPFS ipfs = IPFS.getInstance(getApplicationContext());
                checkNotNull(ipfs);
                String pkey = ipfs.getPublicKey();

                Content params = new Content();
                params.put(Content.ALIAS, alias);
                params.put(Content.PKEY, pkey);
                Gson gson = new Gson();
                String content = gson.toJson(params);

                EntityService entityService = EntityService.getInstance(getApplicationContext());
                String address = AddressType.getAddress(host, AddressType.PEER);
                entityService.insertData(getApplicationContext(), address, content);

                InitApplication.setLoginFlag(getApplicationContext(), true);
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
