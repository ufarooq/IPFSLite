package threads.server.services;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import threads.ipfs.IPFS;
import threads.ipfs.PID;
import threads.ipfs.Peer;
import threads.server.core.peers.PEERS;

import static androidx.core.util.Preconditions.checkNotNull;

public class LoadPeersService {

    private static final String TAG = LoadPeersService.class.getSimpleName();


    public static void loadPeers(@NonNull Context context) {
        checkNotNull(context);

        try {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    evaluateAllPeers(context);
                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
                }
            });
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }


    private static void evaluateAllPeers(@NonNull Context context) {
        IPFS ipfs = IPFS.getInstance(context);
        PEERS peersInstance = PEERS.getInstance(context);

        checkNotNull(ipfs);

        // important reset all connection status
        peersInstance.resetPeersConnected();
        List<Peer> peers = ipfs.swarmPeers();

        for (threads.ipfs.Peer peer : peers) {
            storePeer(context, peer);
        }


        List<threads.server.core.peers.Peer> stored = peersInstance.getPeers();
        for (threads.server.core.peers.Peer peer : stored) {
            if (!peer.isConnected()) {
                peersInstance.removePeer(peer);
            }
        }

    }


    private static void storePeer(@NonNull Context context,
                                  @NonNull threads.ipfs.Peer peer) {
        checkNotNull(peer);

        IPFS ipfs = IPFS.getInstance(context);

        boolean isConnected = ipfs.isConnected(peer.getPid());
        boolean isPubsub = peer.isFloodSub() || peer.isMeshSub();

        storePeer(context, peer.getPid(),
                peer.getMultiAddress(), peer.isRelay(), peer.isAutonat(),
                isPubsub, isConnected, peer.getLatency());
    }

    private static void storePeer(@NonNull Context context,
                                  @NonNull PID pid,
                                  @NonNull String multiAddress,
                                  boolean isRelay, boolean isAutonat, boolean isPubsub,
                                  boolean isConnected, long latency) {

        PEERS peers = PEERS.getInstance(context);


        threads.server.core.peers.Peer peer = peers.getPeerByPID(pid);
        if (peer != null) {
            peer.setMultiAddress(multiAddress);
            peer.setRelay(isRelay);
            peer.setAutonat(isAutonat);
            peer.setPubsub(isPubsub);
            peer.setLatency(latency);
            peer.setConnected(isConnected);
            peers.updatePeer(peer);
        } else {
            peer = peers.createPeer(pid, multiAddress);
            peer.setRelay(isRelay);
            peer.setAutonat(isAutonat);
            peer.setPubsub(isPubsub);
            peer.setLatency(latency);
            peer.setConnected(isConnected);
            peers.storePeer(peer);
        }

    }
}
