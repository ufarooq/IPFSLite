package threads.server.work;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.ArrayList;
import java.util.List;

import threads.ipfs.IPFS;
import threads.ipfs.Peer;
import threads.server.core.peers.PEERS;

import static androidx.core.util.Preconditions.checkNotNull;

public class LoadPeersWorker extends Worker {


    private static final String TAG = LoadPeersWorker.class.getSimpleName();

    public LoadPeersWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    public static void loadPeers(@NonNull Context context) {
        checkNotNull(context);


        OneTimeWorkRequest syncWorkRequest =
                new OneTimeWorkRequest.Builder(LoadPeersWorker.class)
                        .addTag(TAG)
                        .build();

        WorkManager.getInstance(context).enqueueUniqueWork(
                TAG, ExistingWorkPolicy.KEEP, syncWorkRequest);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            evaluateAllPeers();
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }


        return Result.success();
    }


    private void evaluateAllPeers() {
        IPFS ipfs = IPFS.getInstance(getApplicationContext());
        PEERS peersInstance = PEERS.getInstance(getApplicationContext());

        checkNotNull(ipfs);

        List<Peer> peers = ipfs.swarmPeers();

        List<threads.server.core.peers.Peer> list = new ArrayList<>();
        for (threads.ipfs.Peer peer : peers) {
            list.add(createPeer(peer));
        }

        peersInstance.clearPeers();
        peersInstance.storePeers(list);

    }

    private threads.server.core.peers.Peer createPeer(@NonNull threads.ipfs.Peer peer) {
        checkNotNull(peer);
        PEERS peers = PEERS.getInstance(getApplicationContext());

        return peers.createPeer(peer.getPid());
    }
}
