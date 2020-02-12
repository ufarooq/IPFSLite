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

import org.apache.commons.lang3.RandomStringUtils;

import threads.ipfs.IPFS;
import threads.ipfs.PID;
import threads.ipfs.Peer;
import threads.ipfs.PeerInfo;
import threads.server.InitApplication;
import threads.server.core.peers.Addresses;
import threads.server.core.peers.Content;
import threads.server.core.peers.PEERS;
import threads.server.core.peers.User;

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


        boolean isConnected = false;
        try {
            PEERS peers = PEERS.getInstance(getApplicationContext());

            PID user = PID.create(pid);

            if (!peers.isUserBlocked(user)) {

                if (peers.getUserPublicKey(user.getPid()) == null) {
                    LoadIdentityWorker.identify(getApplicationContext(), user.getPid());
                }


                isConnected = connect(user);
                peers.setUserDialing(pid, false);
                peers.setUserConnected(user, isConnected);

                if (isConnected) {
                    id(user.getPid());
                }

            }


        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        } finally {
            Log.e(TAG, " finish onStart [" + (System.currentTimeMillis() - start) + "]...");
        }
        if (isConnected) {
            DownloadContentsWorker.download(getApplicationContext(), pid);
            return Result.success();
        } else {
            return Result.failure();
        }
    }

    private void id(@NonNull String peerID) {
        int timeout = InitApplication.getConnectionTimeout(getApplicationContext());
        try {
            IPFS ipfs = IPFS.getInstance(getApplicationContext());
            checkNotNull(ipfs, "IPFS not valid");
            PEERS peers = PEERS.getInstance(getApplicationContext());
            PID pid = PID.create(peerID);
            boolean update = false;
            Peer peerInfo = ipfs.swarmPeer(PID.create(peerID));
            String multiAddress = "";
            if (peerInfo != null) {
                multiAddress = peerInfo.getMultiAddress();
            }
            User user = peers.getUserByPID(pid);
            checkNotNull(user);

            if (!multiAddress.isEmpty()) {
                Addresses addresses = user.getAddresses();
                if (!addresses.contains(multiAddress)) {
                    update = true;
                    user.clearAddresses();
                    user.addAddress(multiAddress);
                }
            }

            if (user.getPublicKey() == null || !user.isLite()) {
                PeerInfo pInfo = ipfs.id(pid, timeout);
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
                    if (!user.isLite()) {
                        if (pInfo.isLiteAgent()) {
                            update = true;
                            user.setLite(true);
                        }
                    }
                }
            }
            if (update) {
                peers.updateUser(user);
            }

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    private boolean connect(@NonNull PID pid) {
        checkNotNull(pid);
        String tag = RandomStringUtils.randomAlphabetic(10);
        int timeout = InitApplication.getConnectionTimeout(getApplicationContext());
        IPFS ipfs = IPFS.getInstance(getApplicationContext());

        if (ipfs.isConnected(pid)) {
            ipfs.protectPeer(pid, tag);
            return true;
        } else {

            if (!isStopped()) {
                if (ipfs.swarmConnect(pid, timeout)) {
                    ipfs.protectPeer(pid, tag);
                    return true;
                }
            }
            if (!isStopped()) {
                // now check old addresses
                PEERS peers = PEERS.getInstance(getApplicationContext());
                User user = peers.getUserByPID(pid);
                checkNotNull(user);
                for (String address : user.getAddresses()) {
                    String multiAddress = address.concat("/" + IPFS.Style.p2p + "/" + pid.getPid());
                    Log.e(TAG, "Connect to " + multiAddress);
                    if (ipfs.swarmConnect(multiAddress, timeout)) {
                        ipfs.protectPeer(pid, tag);
                        return true;
                    }
                }
            }
        }
        return false;
    }
}

