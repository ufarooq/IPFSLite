package threads.core;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.RandomStringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import threads.core.api.Additional;
import threads.core.api.Additionals;
import threads.core.api.Peer;
import threads.core.api.User;
import threads.iota.EntityService;
import threads.ipfs.api.PID;
import threads.ipfs.api.PeerInfo;

import static androidx.core.util.Preconditions.checkArgument;
import static androidx.core.util.Preconditions.checkNotNull;

public class IdentityService {

    public static final String TAG = IdentityService.class.getSimpleName();

    @Nullable
    public static ResultInfo publishIdentity(@NonNull Context context,
                                             @NonNull Map<String, String> params,
                                             int numRelays) {

        checkNotNull(context);
        checkNotNull(params);

        checkArgument(numRelays >= 0);

        if (!Network.isConnected(context)) {
            return null;
        }

        int timeout = 3;


        // first load stored relays
        String tag = RandomStringUtils.randomAlphabetic(10);

        List<Peer> storedRelays =
                GatewayService.connectStoredRelays(context, tag, numRelays, timeout);


        final THREADS threads = Singleton.getInstance(context).getThreads();

        try {

            PID host = Preferences.getPID(context);
            if (host != null) {

                threads.core.api.PeerInfo peer = threads.getPeerInfoByPID(host);
                if (peer != null) {
                    return updatePeerInfo(context, peer, storedRelays, params, tag,
                            timeout, numRelays);

                } else {
                    return createPeerInfo(context, storedRelays, host, params, tag,
                            timeout, numRelays);
                }
            }

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }

        return null;
    }

    private static List<String> getMultiAddresses(@NonNull PeerInfo info) {
        checkNotNull(info);
        List<String> addresses = new ArrayList<>();
        for (String address : info.getMultiAddresses()) {
            if (!address.startsWith("/ip6/::1/") && !address.startsWith("/ip4/127.0.0.1/")) {
                addresses.add(address);
            }
        }
        return addresses;
    }

    private static ResultInfo createPeerInfo(@NonNull Context context,
                                             @NonNull List<Peer> storedRelays,
                                             @NonNull PID user,
                                             @NonNull Map<String, String> params,
                                             @NonNull String tag,
                                             int timeout,
                                             int numPeers) {

        final THREADS threads = Singleton.getInstance(context).getThreads();

        threads.core.api.PeerInfo peerInfo = threads.createPeerInfo(user);

        updatesPeerInfo(context, peerInfo, storedRelays, params, tag, timeout, numPeers);

        threads.storePeerInfo(peerInfo);

        return new ResultInfo(peerInfo, tag, insertPeer(context, peerInfo));


    }

    private static boolean insertPeer(@NonNull Context context,
                                      @NonNull threads.core.api.PeerInfo peer) {

        THREADS threads = Singleton.getInstance(context).getThreads();
        EntityService entityService = Singleton.getInstance(context).getEntityService();

        threads.setHash(peer, null);

        long start = System.currentTimeMillis();

        boolean success = threads.insertPeerInfo(context, entityService, peer);

        long time = (System.currentTimeMillis() - start) / 1000;
        if (success) {
            Singleton.getInstance(context).getConsoleListener().info(
                    "Success store peer discovery information: " + time + " [s]");
        } else {
            Singleton.getInstance(context).getConsoleListener().error(
                    "Failed store peer discovery information: " + time + " [s]");
        }
        return success;
    }


    public static threads.core.api.PeerInfo getPeerInfo(@NonNull Context context,
                                                        @NonNull PID pid,
                                                        boolean updateUser) {
        checkNotNull(context);
        checkNotNull(pid);

        final THREADS threads = Singleton.getInstance(context).getThreads();
        final EntityService entityService = Singleton.getInstance(context).getEntityService();
        threads.core.api.PeerInfo peerInfo = threads.getPeer(context, entityService, pid);
        if (peerInfo != null && updateUser) {
            boolean update = false;
            User user = threads.getUserByPID(pid);
            if (user != null) {
                Additionals additionals = peerInfo.getAdditionals();
                for (String key : additionals.keySet()) {
                    Additional additional = additionals.get(key);
                    checkNotNull(additional);
                    String value = additional.getValue();
                    user.addAdditional(key, value, true);
                    update = true;
                }
            }
            if (update) {
                threads.updateUser(user);
            }
        }
        return peerInfo;
    }


    private static void updatesPeerInfo(@NonNull Context context,
                                        @NonNull threads.core.api.PeerInfo peerInfo,
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


        peerInfo.removeAdditionals();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            peerInfo.addAdditional(entry.getKey(), entry.getValue(), false);
        }

    }

    private static ResultInfo updatePeerInfo(
            @NonNull Context context,
            @NonNull threads.core.api.PeerInfo peerInfo,
            @NonNull List<Peer> storedRelays,
            @NonNull Map<String, String> params,
            @NonNull String tag,
            int timeout,
            int numPeers) {
        checkArgument(timeout > 0);

        updatesPeerInfo(context, peerInfo, storedRelays, params, tag, timeout, numPeers);

        THREADS threads = Singleton.getInstance(context).getThreads();

        threads.updatePeerInfo(peerInfo);
        return new ResultInfo(peerInfo, tag, insertPeer(context, peerInfo));
    }


    public static class ResultInfo {

        @NonNull
        private final String tag;
        private final boolean insert;
        @NonNull
        private final threads.core.api.PeerInfo peerInfo;

        ResultInfo(@NonNull threads.core.api.PeerInfo peerInfo,
                   @NonNull String tag, boolean insert) {

            this.peerInfo = peerInfo;
            this.tag = tag;
            this.insert = insert;
        }

        @NonNull
        public String getTag() {
            return tag;
        }

        @NonNull
        public threads.core.api.PeerInfo getPeerInfo() {
            return peerInfo;
        }

        public boolean isInsert() {
            return insert;
        }


    }

}
