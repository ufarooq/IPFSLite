package threads.server.services;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.RandomStringUtils;

import java.util.List;
import java.util.Map;

import threads.iota.EntityService;
import threads.ipfs.IPFS;
import threads.ipfs.PID;
import threads.server.core.peers.Additional;
import threads.server.core.peers.Additions;
import threads.server.core.peers.PEERS;
import threads.server.core.peers.Peer;
import threads.server.core.peers.User;

import static androidx.core.util.Preconditions.checkArgument;
import static androidx.core.util.Preconditions.checkNotNull;

public class IdentityService {

    private static final String TAG = IdentityService.class.getSimpleName();


    public static void publishIdentity(@NonNull Context context,
                                       @NonNull Map<String, String> params,
                                       int numRelays) {

        checkNotNull(context);
        checkNotNull(params);

        checkArgument(numRelays >= 0);


        int timeout = 3;


        // first load stored relays
        String tag = RandomStringUtils.randomAlphabetic(10);

        List<Peer> storedRelays =
                GatewayService.connectStoredRelays(context, tag, numRelays, timeout);


        final PEERS threads = PEERS.getInstance(context);


        try {

            PID host = IPFS.getPID(context);
            if (host != null) {

                threads.server.core.peers.PeerInfo peer = threads.getPeerInfoByPID(host);
                if (peer != null) {
                    updatePeerInfo(context, peer, storedRelays, params, tag,
                            timeout, numRelays);

                } else {
                    createPeerInfo(context, storedRelays, host, params, tag,
                            timeout, numRelays);
                }
            }

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }

    }

    private static void createPeerInfo(@NonNull Context context,
                                       @NonNull List<Peer> storedRelays,
                                       @NonNull PID user,
                                       @NonNull Map<String, String> params,
                                       @NonNull String tag,
                                       int timeout,
                                       int numPeers) {

        final PEERS threads = PEERS.getInstance(context);


        threads.server.core.peers.PeerInfo peerInfo = threads.createPeerInfo(user);

        updatesPeerInfo(context, peerInfo, storedRelays, params, tag, timeout, numPeers);

        threads.storePeerInfo(peerInfo);
        insertPeer(context, peerInfo);
    }

    private static void insertPeer(@NonNull Context context,
                                   @NonNull threads.server.core.peers.PeerInfo peer) {

        PEERS threads = PEERS.getInstance(context);

        EntityService entityService = EntityService.getInstance(context);
        threads.setHash(peer, null);

        long start = System.currentTimeMillis();

        boolean success = threads.insertPeerInfo(context, entityService, peer);

        long time = (System.currentTimeMillis() - start) / 1000;
        if (success) {
            Log.w(TAG,
                    "Success store peer discovery information: " + time + " [s]");
        } else {
            Log.w(TAG,
                    "Failed store peer discovery information: " + time + " [s]");
        }
    }


    static threads.server.core.peers.PeerInfo getPeerInfo(
            @NonNull Context context, @NonNull PID pid, boolean updateUser) {
        checkNotNull(context);
        checkNotNull(pid);

        final PEERS peers = PEERS.getInstance(context);

        final EntityService entityService = EntityService.getInstance(context);
        threads.server.core.peers.PeerInfo peerInfo = peers.getPeer(context, entityService, pid);
        if (peerInfo != null && updateUser) {
            boolean update = false;
            User user = peers.getUserByPID(pid);
            if (user != null) {
                Additions additions = peerInfo.getAdditions();
                for (String key : additions.keySet()) {
                    Additional additional = additions.get(key);
                    checkNotNull(additional);
                    String value = additional.getValue();
                    user.addAdditional(key, value, true);
                    update = true;
                }
            }
            if (update) {
                peers.updateUser(user);
            }
        }
        return peerInfo;
    }


    private static void updatesPeerInfo(@NonNull Context context,
                                        @NonNull threads.server.core.peers.PeerInfo peerInfo,
                                        @NonNull List<Peer> storedRelays,
                                        @NonNull Map<String, String> params,
                                        @NonNull String tag, int timeout, int numPeers) {
        checkArgument(timeout > 0);

        peerInfo.removeAddresses();
        List<Peer> peers = GatewayService.getRelayPeers(context, tag, numPeers, timeout);
        for (Peer relay : peers) {
            peerInfo.addAddress(relay.getPid(), relay.getMultiAddress());
        }

        if (peerInfo.numAddresses() < numPeers) {
            for (Peer relay : storedRelays) {
                if (peerInfo.numAddresses() < numPeers) {
                    if (!peerInfo.hasAddress(relay.getPID())) {
                        peerInfo.addAddress(relay.getPid(), relay.getMultiAddress());
                    }
                }
            }
        }

        if (peerInfo.numAddresses() < numPeers) {
            List<Peer> connectedPeers = GatewayService.getConnectedPeers(context, tag, numPeers);
            for (Peer peer : connectedPeers) {
                if (peerInfo.numAddresses() < numPeers) {
                    if (!peerInfo.hasAddress(peer.getPID())) {
                        peerInfo.addAddress(peer.getPid(), peer.getMultiAddress());
                    }
                }
            }
        }


        peerInfo.removeAdditions();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            peerInfo.addAdditional(entry.getKey(), entry.getValue(), false);
        }

    }

    private static void updatePeerInfo(
            @NonNull Context context,
            @NonNull threads.server.core.peers.PeerInfo peerInfo,
            @NonNull List<Peer> storedRelays,
            @NonNull Map<String, String> params,
            @NonNull String tag,
            int timeout,
            int numPeers) {
        checkArgument(timeout > 0);

        updatesPeerInfo(context, peerInfo, storedRelays, params, tag, timeout, numPeers);

        PEERS peers = PEERS.getInstance(context);


        peers.updatePeerInfo(peerInfo);
        insertPeer(context, peerInfo);

    }
}
