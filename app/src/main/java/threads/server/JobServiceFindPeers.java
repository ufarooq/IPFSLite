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

import threads.core.IdentityService;
import threads.core.Network;
import threads.core.Singleton;
import threads.core.THREADS;
import threads.core.api.Content;
import threads.core.api.PeerInfo;
import threads.core.api.User;
import threads.core.api.UserType;
import threads.ipfs.IPFS;
import threads.ipfs.api.CID;
import threads.ipfs.api.PID;
import threads.ipfs.api.Peer;
import threads.share.ThumbnailService;

import static androidx.core.util.Preconditions.checkNotNull;

public class JobServiceFindPeers extends JobService {


    private static final String TAG = JobServiceFindPeers.class.getSimpleName();


    public static void findPeers(@NonNull Context context) {
        checkNotNull(context);

        JobScheduler jobScheduler = (JobScheduler) context.getApplicationContext()
                .getSystemService(JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            ComponentName componentName = new ComponentName(context, JobServiceFindPeers.class);

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

        final int timeout = 3;
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {

            try {

                if (Network.isConnectedFast(getApplicationContext())) {
                    Service.getInstance(getApplicationContext());


                    IPFS ipfs = Singleton.getInstance(getApplicationContext()).getIpfs();
                    THREADS threads = Singleton.getInstance(getApplicationContext()).getThreads();
                    checkNotNull(ipfs, "IPFS not defined");


                    List<Peer> peers = ipfs.swarmPeers();

                    for (Peer peer : peers) {
                        PID pid = peer.getPid();

                        if (threads.getUserByPID(pid) == null) {

                            PeerInfo peerInfo = IdentityService.getPeerInfo(
                                    getApplicationContext(), pid, "", false);
                            if (peerInfo != null) {
                                String alias = peerInfo.getAdditionalValue(Content.ALIAS);
                                if (!alias.isEmpty()) {
                                    threads.ipfs.api.PeerInfo info = ipfs.id(pid, timeout);
                                    if (info != null) {
                                        String pubKey = info.getPublicKey();
                                        if (pubKey != null && !pubKey.isEmpty()) {
                                            CID image = ThumbnailService.getImage(getApplicationContext(),
                                                    alias, BuildConfig.ApiAesKey, R.drawable.server_network);

                                            User user = threads.createUser(pid, pubKey, alias,
                                                    UserType.UNKNOWN, image);
                                            user.setBlocked(true);
                                            threads.storeUser(user);
                                        }
                                    }
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

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }
}
