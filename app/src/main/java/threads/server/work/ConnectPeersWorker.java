package threads.server.work;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.List;
import java.util.concurrent.TimeUnit;

import threads.ipfs.IPFS;
import threads.server.core.peers.PEERS;
import threads.server.core.peers.User;
import threads.server.services.SwarmService;

import static androidx.core.util.Preconditions.checkNotNull;

public class ConnectPeersWorker extends Worker {


    private static final String TAG = ConnectPeersWorker.class.getSimpleName();


    public ConnectPeersWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    public static void connect(@NonNull Context context, int secondsDelay) {
        checkNotNull(context);
        Constraints.Builder builder = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED);


        OneTimeWorkRequest syncWorkRequest =
                new OneTimeWorkRequest.Builder(ConnectPeersWorker.class)
                        .setInitialDelay(secondsDelay, TimeUnit.SECONDS)
                        .addTag(TAG)
                        .setConstraints(builder.build())
                        .build();

        WorkManager.getInstance(context).enqueueUniqueWork(
                TAG, ExistingWorkPolicy.KEEP, syncWorkRequest);


    }


    @NonNull
    @Override
    public Result doWork() {

        long start = System.currentTimeMillis();

        try {
            PEERS peers = PEERS.getInstance(getApplicationContext());
            IPFS ipfs = IPFS.getInstance(getApplicationContext());
            checkNotNull(ipfs, "IPFS not defined");

            List<User> users = peers.getUsers();

            for (User user : users) {

                if (!user.isBlocked()) {

                    ipfs.addPubSubTopic(
                            getApplicationContext(), user.getPID().getPid());


                    SwarmService.ConnectInfo info = SwarmService.connect(
                            getApplicationContext(), user.getPID());

                    peers.setUserConnected(user.getPID(), info.isConnected());

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

