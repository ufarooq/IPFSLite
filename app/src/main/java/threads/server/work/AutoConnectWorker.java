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

import java.util.List;
import java.util.concurrent.TimeUnit;

import threads.ipfs.IPFS;
import threads.server.core.peers.PEERS;
import threads.server.core.peers.User;
import threads.server.utils.Preferences;

import static androidx.core.util.Preconditions.checkNotNull;

public class AutoConnectWorker extends Worker {


    private static final String TAG = AutoConnectWorker.class.getSimpleName();
    private static final String DIALING = "DIALING";


    public AutoConnectWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
    }

    public static void autoConnect(@NonNull Context context,
                                   boolean showDialing,
                                   int secondsDelay) {
        checkNotNull(context);
        Constraints.Builder builder = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED);


        Data.Builder data = new Data.Builder();
        data.putBoolean(DIALING, showDialing);

        OneTimeWorkRequest syncWorkRequest =
                new OneTimeWorkRequest.Builder(AutoConnectWorker.class)
                        .setInitialDelay(secondsDelay, TimeUnit.SECONDS)
                        .setInputData(data.build())
                        .addTag("AutoConnectTag")
                        .setConstraints(builder.build())
                        .build();

        WorkManager.getInstance(context).enqueueUniqueWork(
                "AutoConnect", ExistingWorkPolicy.KEEP, syncWorkRequest);


    }


    @NonNull
    @Override
    public Result doWork() {
        final PEERS peers = PEERS.getInstance(getApplicationContext());
        final int timeout = Preferences.getConnectionTimeout(getApplicationContext());
        boolean dialing = getInputData().getBoolean(DIALING, true);

        long start = System.currentTimeMillis();

        try {
            IPFS ipfs = IPFS.getInstance(getApplicationContext());
            checkNotNull(ipfs, "IPFS not defined");

            List<User> users = peers.getUsers();

            for (User user : users) {

                if (dialing) {
                    peers.setUserDialing(user.getPID(), true);
                }

                ipfs.addPubSubTopic(
                        getApplicationContext(), user.getPID().getPid());


                boolean result = ipfs.swarmConnect(user.getPID(), timeout);
                if (result) {
                    ipfs.protectPeer(user.getPID(), TAG);
                }

                if (dialing) {
                    peers.setUserDialing(user.getPID(), false);
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

