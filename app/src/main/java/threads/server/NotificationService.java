package threads.server;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import threads.core.Preferences;
import threads.core.Singleton;
import threads.core.api.AddressType;
import threads.core.api.Content;
import threads.core.api.Server;
import threads.iota.Entity;
import threads.iota.EntityService;
import threads.ipfs.api.CID;
import threads.ipfs.api.Encryption;
import threads.ipfs.api.Multihash;
import threads.ipfs.api.PID;

import static androidx.core.util.Preconditions.checkNotNull;

public class NotificationService extends JobService {

    private static final String TAG = NotificationService.class.getSimpleName();


    public static void notifications(@NonNull Context context) {
        checkNotNull(context);
        JobScheduler jobScheduler = (JobScheduler) context.getApplicationContext()
                .getSystemService(JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            ComponentName componentName = new ComponentName(context, NotificationService.class);

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

    public static void periodic(@NonNull Context context) {
        checkNotNull(context);
        JobScheduler jobScheduler = (JobScheduler) context.getApplicationContext()
                .getSystemService(JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            ComponentName componentName = new ComponentName(context, NotificationService.class);

            JobInfo jobInfo = new JobInfo.Builder(NotificationService.class.hashCode(), componentName)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setPeriodic(JobInfo.getMinPeriodMillis(), JobInfo.getMinFlexMillis())
                    .setPersisted(true)
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

        Log.e(TAG, "onStartJob!");
        final PID host = Preferences.getPID(getApplicationContext());
        final String privateKey = Preferences.getPrivateKey(getApplicationContext());
        final ContentService contentService = ContentService.getInstance(getApplicationContext());
        if (host != null && !privateKey.isEmpty()) {

            final int timeout = Preferences.getTangleTimeout(getApplicationContext());
            final Server server = Preferences.getTangleServer(getApplicationContext());
            final EntityService entityService = EntityService.getInstance(getApplicationContext());
            final Gson gson = new Gson();

            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    String address = AddressType.getAddress(host, AddressType.NOTIFICATION);
                    List<Entity> entities = entityService.loadEntities(address,
                            server.getProtocol(), server.getHost(), server.getPort(), timeout);

                    Log.e(TAG, "Received " + entities.size() + " incoming messages");

                    for (Entity entity : entities) {
                        String notification = entity.getContent();
                        Content data;
                        try {
                            data = gson.fromJson(notification, Content.class);
                        } catch (Throwable e) {
                            Log.e(TAG, "" + e.getLocalizedMessage(), e);
                            continue;
                        }
                        if (data != null) {
                            if (data.containsKey(Content.EST)) {
                                if (data.containsKey(Content.PID) && data.containsKey(Content.CID)) {
                                    try {
                                        final String pidStr = Encryption.decryptRSA(
                                                data.get(Content.PID), privateKey);
                                        checkNotNull(pidStr);

                                        final String cidStr = Encryption.decryptRSA(
                                                data.get(Content.CID), privateKey);
                                        checkNotNull(cidStr);

                                        // check if cid is valid
                                        try {
                                            Multihash.fromBase58(cidStr);
                                        } catch (Throwable e) {
                                            Log.e(TAG, "" + e.getLocalizedMessage(), e);
                                            continue;
                                        }

                                        // check if pid is valid
                                        try {
                                            Multihash.fromBase58(pidStr);
                                        } catch (Throwable e) {
                                            Log.e(TAG, "" + e.getLocalizedMessage(), e);
                                            continue;
                                        }

                                        PID pid = PID.create(pidStr);
                                        CID cid = CID.create(cidStr);

                                        contentService.insertContent(pid, cid, false);


                                        final Integer est = Integer.valueOf(data.get(Content.EST));
                                        checkNotNull(est);

                                        Singleton.getInstance(
                                                getApplicationContext()).getConsoleListener().info(
                                                "Receive Inbox Notification from PID :" + pid);

                                        switch (NotificationType.toNotificationType(est)) {
                                            case OFFER:
                                                DownloadService.download(
                                                        getApplicationContext(), pid, cid);
                                                break;
                                            case PROVIDE:
                                                UploadService.upload(
                                                        getApplicationContext(), pid, cid);
                                                break;
                                        }

                                    } catch (Throwable e) {
                                        Log.e(TAG, "" + e.getLocalizedMessage(), e);
                                    }
                                }
                            }
                        }
                    }

                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
                } finally {
                    jobFinished(jobParameters, false);
                }

            });
            return true;
        }
        return false;
    }

}
