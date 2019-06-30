package threads.server;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import threads.core.Preferences;
import threads.core.Singleton;
import threads.core.THREADS;
import threads.core.api.User;
import threads.ipfs.api.PID;
import threads.share.ConnectService;
import threads.share.PeerService;

import static androidx.core.util.Preconditions.checkNotNull;

public class ContentsService extends JobService {

    private static final String TAG = ContentsService.class.getSimpleName();


    public static void downloadContents(@NonNull Context context) {
        checkNotNull(context);
        JobScheduler jobScheduler = (JobScheduler) context.getApplicationContext()
                .getSystemService(JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            ComponentName componentName = new ComponentName(context, ContentsService.class);

            JobInfo jobInfo = new JobInfo.Builder(TAG.hashCode(), componentName)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setPeriodic(TimeUnit.HOURS.toMillis(12))
                    .setRequiresCharging(true)
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

    public static long getMinutesAgo(int minutes) {
        return System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(minutes);
    }

    @Override
    public boolean onStartJob(JobParameters jobParameters) {

        Log.e(TAG, "onStartJob!");

        Service.getInstance(getApplicationContext());
        final ContentService contentService = ContentService.getInstance(getApplicationContext());
        final THREADS threads = Singleton.getInstance(getApplicationContext()).getThreads();
        final PID pid = Preferences.getPID(getApplicationContext());
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {

                PeerService.publishPeer(getApplicationContext());

                for (User user : threads.getUsers()) {

                    if (pid != null) {
                        if (user.getPID().equals(pid)) {
                            continue;
                        }
                    }

                    if (!threads.isAccountBlocked(user.getPID())) {

                        boolean success = ConnectService.connectUser(
                                getApplicationContext(), user.getPID());

                        if (success) {
                            long timestamp = getMinutesAgo(30);

                            List<Content> contents = contentService.getContentDatabase().
                                    contentDao().getContents(
                                    user.getPID(), timestamp, false);

                            for (Content content : contents) {
                                Service.downloadMultihash(getApplicationContext(),
                                        content.getPid(), content.getCID());
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

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }

}
