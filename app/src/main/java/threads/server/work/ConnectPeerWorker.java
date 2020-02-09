package threads.server.work;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import threads.ipfs.PID;
import threads.server.core.peers.Content;
import threads.server.core.peers.PEERS;
import threads.server.jobs.JobServiceLoadPublicKey;
import threads.server.services.SwarmService;

import static androidx.core.util.Preconditions.checkNotNull;

public class ConnectPeerWorker extends Worker {

    public static final String WID = "CPW";
    private static final String TAG = ConnectPeerWorker.class.getSimpleName();


    public ConnectPeerWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    public static void connect(@NonNull Context context, @NonNull String pid) {
        checkNotNull(context);
        checkNotNull(pid);
        Constraints.Builder builder = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED);


        Data.Builder data = new Data.Builder();
        data.putString(Content.PID, pid);

        OneTimeWorkRequest syncWorkRequest =
                new OneTimeWorkRequest.Builder(ConnectPeerWorker.class)
                        .setInputData(data.build())
                        .addTag(TAG)
                        .setConstraints(builder.build())
                        .build();

        WorkManager.getInstance(context).enqueueUniqueWork(
                WID + pid, ExistingWorkPolicy.KEEP, syncWorkRequest);


    }


    @NonNull
    @Override
    public Result doWork() {

        String pid = getInputData().getString(Content.PID);
        checkNotNull(pid);
        long start = System.currentTimeMillis();

        try {
            PEERS peers = PEERS.getInstance(getApplicationContext());

            PID user = PID.create(pid);

            if (!peers.isUserBlocked(user)) {
                try {
                    SwarmService.ConnectInfo info =
                            SwarmService.connect(getApplicationContext(), user);

                    peers.setUserConnected(user, info.isConnected());

                    if (info.isConnected()) {
                        // TODO public key calculation
                        String publicKey = peers.getUserPublicKey(pid);
                        checkNotNull(publicKey);
                        if (publicKey.isEmpty()) {
                            JobServiceLoadPublicKey.publicKey(getApplicationContext(), pid);
                        }
                    }

                } finally {
                    peers.setUserDialing(pid, false);
                }
            }


        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        } finally {
            Log.e(TAG, " finish onStart [" + (System.currentTimeMillis() - start) + "]...");
        }

        return Result.success();
    }
}

