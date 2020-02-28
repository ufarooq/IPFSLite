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

import java.util.Objects;

import threads.ipfs.IPFS;
import threads.ipfs.PID;
import threads.ipfs.Peer;
import threads.ipfs.PeerInfo;
import threads.server.InitApplication;
import threads.server.core.peers.Content;
import threads.server.core.peers.PEERS;
import threads.server.core.peers.User;

public class ConnectUserWorker extends Worker {

    private static final String WID = "CPW";
    private static final String TAG = ConnectUserWorker.class.getSimpleName();


    public ConnectUserWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    public static String getUniqueId(@NonNull String pid) {
        return WID + pid;
    }

    public static void connect(@NonNull Context context, @NonNull String pid) {

        Constraints.Builder builder = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED);


        Data.Builder data = new Data.Builder();
        data.putString(Content.PID, pid);

        OneTimeWorkRequest syncWorkRequest =
                new OneTimeWorkRequest.Builder(ConnectUserWorker.class)
                        .setInputData(data.build())
                        .addTag(TAG)
                        .setConstraints(builder.build())
                        .build();

        WorkManager.getInstance(context).enqueueUniqueWork(
                getUniqueId(pid), ExistingWorkPolicy.KEEP, syncWorkRequest);


    }


    @NonNull
    @Override
    public Result doWork() {

        String pid = getInputData().getString(Content.PID);
        Objects.requireNonNull(pid);
        long start = System.currentTimeMillis();

        Log.e(TAG, " start connect [" + pid + "]...");
        boolean isConnected = false;
        try {
            int timeout = InitApplication.getConnectionTimeout(getApplicationContext());
            PEERS peers = PEERS.getInstance(getApplicationContext());

            if (!peers.isUserBlocked(pid)) {

                connect(PID.create(pid));

                peers.setUserDialing(pid, false);


                IPFS ipfs = IPFS.getInstance(getApplicationContext());


                boolean update = false;

                User user = peers.getUserByPID(PID.create(pid));
                Objects.requireNonNull(user);

                if (!isStopped()) {
                    PeerInfo pInfo = ipfs.id(PID.create(pid), timeout);
                    if (pInfo != null) {

                        if (user.getPublicKey() == null) {
                            String pKey = pInfo.getPublicKey();
                            if (pKey != null) {
                                if (!pKey.isEmpty()) {
                                    update = true;
                                    user.setPublicKey(pKey);
                                }
                            }
                        }
                        if (user.getAgent() == null) {
                            update = true;
                            String agent = pInfo.getAgentVersion();
                            user.setAgent(agent);
                            if (agent.endsWith("lite")) {
                                user.setLite(true);
                            }
                        }
                    }
                }

                if (!isStopped()) {
                    Peer peerInfo = ipfs.swarmPeer(PID.create(pid));
                    String multiAddress = "";
                    if (peerInfo != null) {
                        multiAddress = peerInfo.getMultiAddress();
                    }

                    if (!multiAddress.isEmpty() && !multiAddress.contains("p2p-circuit")) {
                        if (!Objects.equals(user.getAddress(), multiAddress)) {
                            update = true;
                            user.setAddress(multiAddress);
                        }
                    }
                }
                if (update) {
                    peers.updateUser(user);
                }
            }
            IPFS ipfs = IPFS.getInstance(getApplicationContext());
            isConnected = ipfs.isConnected(PID.create(pid));

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        } finally {
            Log.e(TAG, " finish onStart [" + (System.currentTimeMillis() - start) + "]...");
        }
        if (isConnected) {
            DownloadContentsWorker.download(getApplicationContext(), pid); // TODO do this from outside in a chain
            return Result.success();
        } else {
            return Result.failure();
        }
    }


    private void connect(@NonNull PID pid) {

        int timeout = InitApplication.getConnectionTimeout(getApplicationContext());
        IPFS ipfs = IPFS.getInstance(getApplicationContext());

        if (!ipfs.isConnected(pid)) {
            if (!isStopped()) {
                // now check old addresses
                PEERS peers = PEERS.getInstance(getApplicationContext());
                User user = peers.getUserByPID(pid);
                Objects.requireNonNull(user);
                String address = user.getAddress();
                if (!address.isEmpty() && !address.contains("p2p-circuit")) {
                    String multiAddress = address.concat("/" + IPFS.Style.p2p + "/" + pid.getPid());
                    Log.e(TAG, "Connect to " + multiAddress);
                    if (ipfs.swarmConnect(multiAddress, timeout)) {
                        return;
                    }
                }
            }

            if (!isStopped()) {
                if (ipfs.swarmConnect(pid, timeout)) {
                    return;
                }
            }

            if (!isStopped()) {
                ipfs.arbitraryRelay(pid, timeout);
            }
        }
    }
}

