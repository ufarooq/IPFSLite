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

public class ConnectionWorker extends Worker {

    @SuppressWarnings("SpellCheckingInspection")
    @NonNull
    private static final List<String> Bootstrap = new ArrayList<>(Arrays.asList(
            "/ip4/147.75.80.110/tcp/4001/p2p/QmbFgm5zan8P6eWWmeyfncR5feYEMPbht5b1FW1C37aQ7y",
            "/ip4/147.75.195.153/tcp/4001/p2p/QmW9m57aiBDHAkKj9nmFSEn7ZqrcF1fZS4bipsTCHburei",
            "/ip4/147.75.70.221/tcp/4001/p2p/Qme8g49gm3q4Acp7xWBKg3nAa9fxZ1YmyDJdyGgoG6LsXh",
            "/ip4/104.131.131.82/tcp/4001/p2p/QmaCpDMGvV2BGHeYERUEnRQAwe3N8SzbUtfsmvsqQLuvuJ",            // mars.i.ipfs.io
            "/ip6/2604:a880:1:20::203:d001/tcp/4001/p2p/QmSoLPppuBtQSGwKDZT2M73ULpjvfd3aZ6ha4oFGL1KrGM",  // pluto.i.ipfs.io
            "/ip6/2400:6180:0:d0::151:6001/tcp/4001/p2p/QmSoLSafTMBsPKadTEgaXctDQVcqN88CNLHXMkTNwMKPnu",  // saturn.i.ipfs.io
            "/ip6/2604:a880:800:10::4a:5001/tcp/4001/p2p/QmSoLV4Bbm51jM9C4gDYZQ9Cy3U6aXMJDAbzgu2fzaDs64", // venus.i.ipfs.io
            "/ip6/2a03:b0c0:0:1010::23:1001/tcp/4001/p2p/QmSoLer265NRgSp2LA3dPaeykiS1J6DifTC88f5uVQKNAd"  // earth.i.ipfs.io
    ));
    private static final String TAG = ConnectionWorker.class.getSimpleName();
    private static final String LOAD_USERS = "LOAD_USERS";
    private static final int MIN_PEERS = 10;

    public ConnectionWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    public static void connect(@NonNull Context context, boolean loadUser) {

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

