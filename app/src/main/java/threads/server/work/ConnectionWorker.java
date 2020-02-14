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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import threads.ipfs.IPFS;
import threads.ipfs.Peer;
import threads.server.core.peers.PEERS;
import threads.server.core.peers.User;

import static androidx.core.util.Preconditions.checkNotNull;

public class ConnectionWorker extends Worker {


    @NonNull
    static final List<String> Bootstrap = new ArrayList<>(Arrays.asList(
            "/ip4/104.131.131.82/tcp/4001/p2p/QmaCpDMGvV2BGHeYERUEnRQAwe3N8SzbUtfsmvsqQLuvuJ"            // mars.i.ipfs.io
    ));
    private static final String TAG = ConnectionWorker.class.getSimpleName();
    private static final String LOAD_USERS = "LOAD_USERS";
    private static final int MIN_PEERS = 10;

    public ConnectionWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    public static void connect(@NonNull Context context, boolean loadUser) {
        checkNotNull(context);
        Constraints.Builder builder = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED);

        Data.Builder data = new Data.Builder();
        data.putBoolean(LOAD_USERS, loadUser);


        OneTimeWorkRequest syncWorkRequest =
                new OneTimeWorkRequest.Builder(ConnectionWorker.class)
                        .addTag(TAG)
                        .setInputData(data.build())
                        .setConstraints(builder.build())
                        .build();

        WorkManager.getInstance(context).enqueueUniqueWork(
                TAG, ExistingWorkPolicy.KEEP, syncWorkRequest);


    }


    @NonNull
    @Override
    public Result doWork() {

        long start = System.currentTimeMillis();
        boolean loadUsers = getInputData().getBoolean(LOAD_USERS, false);

        Log.e(TAG, " start [Load Users : " + loadUsers + "]...");
        try {
            IPFS ipfs = IPFS.getInstance(getApplicationContext());
            checkNotNull(ipfs, "IPFS not defined");

            List<Peer> peers = ipfs.swarmPeers();

            if (peers.size() < MIN_PEERS) {
                for (String address : Bootstrap) {
                    boolean result = ipfs.swarmConnect(address, 10);
                    Log.e(TAG, " \nBootstrap : " + address + " " + result);
                }
            }

            if (loadUsers && !isStopped()) {
                List<User> users = PEERS.getInstance(getApplicationContext()).getUsers();
                for (User user : users) {
                    if (!user.isBlocked()) {
                        ConnectUserWorker.connect(getApplicationContext(), user.getPid());
                    }
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

