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

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import threads.ipfs.CID;
import threads.ipfs.IPFS;
import threads.ipfs.PID;
import threads.server.R;
import threads.server.core.events.EVENTS;
import threads.server.core.peers.Content;
import threads.server.core.peers.PEERS;
import threads.server.core.peers.User;
import threads.server.core.threads.THREADS;
import threads.server.core.threads.Thread;
import threads.server.services.Service;

import static androidx.core.util.Preconditions.checkNotNull;

public class JobServiceDownload extends JobService {

    private static final String TAG = JobServiceDownload.class.getSimpleName();


    public static void download(@NonNull Context context, @NonNull PID pid, @NonNull CID cid) {
        checkNotNull(context);
        checkNotNull(pid);
        checkNotNull(cid);
        JobScheduler jobScheduler = (JobScheduler) context.getApplicationContext()
                .getSystemService(JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            ComponentName componentName = new ComponentName(context, JobServiceDownload.class);

            PersistableBundle bundle = new PersistableBundle();
            bundle.putString(Content.PID, pid.getPid());
            bundle.putString(Content.CID, cid.getCid());

            JobInfo jobInfo = new JobInfo.Builder(cid.hashCode(), componentName)
                    .setOverrideDeadline(0)
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

    public static void downloadContentID(@NonNull Context context,
                                         @NonNull PID sender,
                                         @NonNull CID cid) {

        checkNotNull(context);
        checkNotNull(sender);
        checkNotNull(cid);

        final THREADS threads = THREADS.getInstance(context);
        final PEERS peers = PEERS.getInstance(context);
        final EVENTS events = EVENTS.getInstance(context);
        final PID host = IPFS.getPID(context);

        try {

            String alias;

            if (sender.equals(host)) {
                alias = IPFS.getDeviceName();
            } else {
                User user = peers.getUserByPID(sender);

                if (user == null) {
                    events.error(context.getString(R.string.unknown_peer_sends_data));
                    return;
                } else {
                    alias = user.getAlias();
                }
            }
            List<Thread> entries = threads.getThreadsByCIDAndParent(cid, 0L);

            if (!entries.isEmpty()) {
                Thread entry = entries.get(0);

                if (entry.isDeleting() || entry.isSeeding()) {
                    Service.replySender(context, sender, entry);
                    return;
                } else {
                    Service.downloadThread(context, entry, sender);
                    return;
                }
            }
            long idx = Service.createThread(context, sender, alias, cid,
                    null, -1, null, null);

            Thread thread = threads.getThreadByIdx(idx);
            checkNotNull(thread);
            Service.downloadThread(context, thread, sender);


        } catch (Throwable e) {
            events.exception(e);
        }

    }


    @Override
    public boolean onStartJob(JobParameters jobParameters) {

        PersistableBundle bundle = jobParameters.getExtras();
        final String pid = bundle.getString(Content.PID);
        checkNotNull(pid);
        final String cid = bundle.getString(Content.CID);
        checkNotNull(cid);


        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            long start = System.currentTimeMillis();

            try {

                downloadContentID(getApplicationContext(), PID.create(pid), CID.create(cid));
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
