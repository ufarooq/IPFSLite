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

import com.google.gson.Gson;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import threads.core.Preferences;
import threads.core.Singleton;
import threads.core.THREADS;
import threads.core.api.AddressType;
import threads.core.api.Content;
import threads.iota.IOTA;
import threads.ipfs.IPFS;
import threads.ipfs.api.Encryption;
import threads.ipfs.api.PID;
import threads.ipfs.api.PeerInfo;

import static androidx.core.util.Preconditions.checkNotNull;

public class NotifyService extends JobService {

    private static final String TAG = NotifyService.class.getSimpleName();
    private final Gson gson = new Gson();

    public static void notify(@NonNull Context context,
                              @NonNull String pid,
                              @NonNull String cid,
                              @NonNull Integer est) {
        checkNotNull(context);
        checkNotNull(pid);
        checkNotNull(cid);
        checkNotNull(est);
        JobScheduler jobScheduler = (JobScheduler) context.getApplicationContext()
                .getSystemService(JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            ComponentName componentName = new ComponentName(context, NotifyService.class);

            PersistableBundle bundle = new PersistableBundle();
            bundle.putString(Content.PID, pid);
            bundle.putString(Content.CID, cid);
            bundle.putInt(Content.EST, est);
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
        final String pid = bundle.getString(Content.PID);
        checkNotNull(pid);
        final String cid = bundle.getString(Content.CID);
        checkNotNull(cid);
        final Integer est = bundle.getInt(Content.EST);
        checkNotNull(est);
        final THREADS threads = Singleton.getInstance(getApplicationContext()).getThreads();
        final PID host = Preferences.getPID(getApplicationContext());
        checkNotNull(host);
        final IOTA iota = Singleton.getInstance(getApplicationContext()).getIota();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                String address = AddressType.getAddress(
                        PID.create(pid), AddressType.NOTIFICATION);

                String publicKey = threads.getUserPublicKey(pid);
                if (publicKey.isEmpty()) {
                    IPFS ipfs = Singleton.getInstance(getApplicationContext()).getIpfs();
                    checkNotNull(ipfs);
                    int timeout = Preferences.getConnectionTimeout(
                            getApplicationContext());
                    PeerInfo info = ipfs.id(PID.create(pid), timeout);
                    if (info != null) {
                        String key = info.getPublicKey();
                        if (key != null) {
                            threads.setUserPublicKey(pid, key);
                            publicKey = key;
                        }
                    }
                }
                if (!publicKey.isEmpty()) {

                    Content content = new Content();

                    content.put(Content.PID, Encryption.encryptRSA(host.getPid(), publicKey));
                    content.put(Content.CID, Encryption.encryptRSA(cid, publicKey));
                    content.put(Content.EST, NotificationType.toNotificationType(est).toString());

                    String json = gson.toJson(content);
                    iota.insertTransaction(address, json);

                    Singleton.getInstance(getApplicationContext()).getConsoleListener().info(
                            "Send Notification to PID Inbox :" + pid);

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
