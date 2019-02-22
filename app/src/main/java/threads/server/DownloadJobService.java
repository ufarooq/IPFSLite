package threads.server;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.PersistableBundle;
import android.util.Log;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import io.ipfs.multihash.Multihash;
import threads.core.IThreadsAPI;
import threads.core.Preferences;
import threads.core.Singleton;
import threads.core.api.Kind;
import threads.core.api.Thread;
import threads.core.api.ThreadStatus;
import threads.core.api.User;
import threads.ipfs.IPFS;
import threads.ipfs.api.CID;
import threads.ipfs.api.Link;
import threads.ipfs.api.PID;

import static com.google.common.base.Preconditions.checkNotNull;

public class DownloadJobService extends JobService {

    private static final String TAG = DownloadJobService.class.getSimpleName();
    private static final String MULTIHASH = "MULTIHASH";


    public static void download(@NonNull Context context, @NonNull String multihash) {
        checkNotNull(context);
        checkNotNull(multihash);

        JobScheduler jobScheduler = (JobScheduler) context.getApplicationContext()
                .getSystemService(JOB_SCHEDULER_SERVICE);

        if (jobScheduler != null) {
            ComponentName componentName = new ComponentName(context, DownloadJobService.class);
            PersistableBundle bundle = new PersistableBundle();
            bundle.putString(MULTIHASH, multihash);

            JobInfo jobInfo = new JobInfo.Builder(new Random().nextInt(), componentName)
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

        final IThreadsAPI threadsAPI = Singleton.getInstance().getThreadsAPI();
        final String multihash = bundle.getString(DownloadJobService.MULTIHASH);
        checkNotNull(multihash);
        final IPFS ipfs = Singleton.getInstance().getIpfs();
        if (ipfs != null) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {


                    // check if multihash is valid
                    try {
                        Multihash.fromBase58(multihash);
                    } catch (Throwable e) {
                        Preferences.error(getString(R.string.multihash_not_valid));
                        return;
                    }

                    PID pid = Preferences.getPID(getApplicationContext());
                    checkNotNull(pid);
                    User user = threadsAPI.getUserByPid(pid);
                    checkNotNull(user);


                    CID cid = CID.create(multihash);
                    List<Link> links = ipfs.ls(cid);
                    if (links.isEmpty() || links.size() > 1) {
                        Preferences.error("Not expected CID object TODO"); // TODO
                        return;
                    }
                    Link link = links.get(0);

                    String filename = link.getPath();


                    byte[] image = IThreadsAPI.getImage(getApplicationContext(),
                            R.drawable.file_document);

                    Thread thread = threadsAPI.createThread(user, ThreadStatus.OFFLINE, Kind.OUT,
                            filename, multihash, image, false, false);
                    threadsAPI.storeThread(thread);

                    NotificationCompat.Builder builder =
                            NotificationSender.createDownloadProgressNotification(
                                    getApplicationContext(), link.getPath());

                    final NotificationManager notificationManager = (NotificationManager)
                            getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
                    int notifyID = NotificationSender.NOTIFICATIONS_COUNTER.incrementAndGet();
                    Notification notification = builder.build();
                    if (notificationManager != null) {
                        notificationManager.notify(notifyID, notification);
                    }


                    try {
                        threadsAPI.preload(ipfs, link.getCid().getCid(), link.getSize(), (percent) -> {


                            builder.setProgress(100, percent, false);
                            if (notificationManager != null) {
                                notificationManager.notify(notifyID, builder.build());
                            }

                        });
                        threadsAPI.setStatus(thread, ThreadStatus.ONLINE);

                    } catch (Throwable e) {
                        threadsAPI.setStatus(thread, ThreadStatus.DELETING); // TODO error state
                        throw e;
                    } finally {

                        if (notificationManager != null) {
                            notificationManager.cancel(notifyID);
                        }
                    }

                    NotificationSender.showLinkNotification(getApplicationContext(), link);

                    Uri uri = Uri.parse(Preferences.getGateway(getApplicationContext()) + multihash);

                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);

                } catch (Throwable e) {
                    Preferences.evaluateException(Preferences.EXCEPTION, e);
                } finally {
                    jobFinished(jobParameters, false);
                }
            });
        }
        return true;
    }


    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }
}