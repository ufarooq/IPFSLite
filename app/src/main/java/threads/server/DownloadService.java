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
import androidx.annotation.Nullable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import threads.core.Preferences;
import threads.core.Singleton;
import threads.core.THREADS;
import threads.core.api.Content;
import threads.iota.IOTA;
import threads.ipfs.api.PID;
import threads.share.ConnectService;
import threads.share.PeerService;

import static androidx.core.util.Preconditions.checkNotNull;

public class DownloadService extends JobService {

    private static final String TAG = DownloadService.class.getSimpleName();

    public static void download(@NonNull Context context,
                                @NonNull String pid,
                                @NonNull String cid,
                                @Nullable String peer) {
        checkNotNull(context);
        checkNotNull(pid);
        checkNotNull(cid);
        JobScheduler jobScheduler = (JobScheduler) context.getApplicationContext()
                .getSystemService(JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            ComponentName componentName = new ComponentName(context, DownloadService.class);
            PersistableBundle bundle = new PersistableBundle();
            bundle.putString(Content.PID, pid);
            bundle.putString(Content.CID, cid);
            if (peer != null) {
                bundle.putString(Content.PEER, peer);
            }
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
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }


    @Override
    public boolean onStartJob(JobParameters jobParameters) {

        PersistableBundle bundle = jobParameters.getExtras();
        final String pid = bundle.getString(Content.PID);
        checkNotNull(pid);
        final String cid = bundle.getString(Content.CID);
        checkNotNull(cid);
        final String peer = bundle.getString(Content.PEER);

        Service.getInstance(getApplicationContext());


        final THREADS threads = Singleton.getInstance(
                getApplicationContext()).getThreads();


        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {


                if (!threads.isAccountBlocked(PID.create(pid))) {


                    if (peer != null) {
                        IOTA iota = Singleton.getInstance(getApplicationContext()).getIota();
                        checkNotNull(iota);
                        threads.getPeerByHash(iota, PID.create(pid), peer);
                    }


                    final boolean pubsubEnabled = Preferences.isPubsubEnabled(
                            getApplicationContext());
                    PeerService.publishPeer(getApplicationContext());
                    ConnectService.connectUser(getApplicationContext(),
                            PID.create(pid), pubsubEnabled);


                    Service.downloadMultihash(getApplicationContext(),
                            PID.create(pid), cid);


                }

            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            } finally {
                jobFinished(jobParameters, false);
            }

        });
        return true;
    }
}
