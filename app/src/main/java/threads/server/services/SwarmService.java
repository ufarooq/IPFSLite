package threads.server.services;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.RandomStringUtils;

import threads.ipfs.IPFS;
import threads.ipfs.PID;
import threads.server.core.peers.Addresses;
import threads.server.core.peers.PeerInfo;
import threads.server.utils.Preferences;

import static androidx.core.util.Preconditions.checkNotNull;

public class SwarmService {
    private static final String TAG = SwarmService.class.getSimpleName();


    public static ConnectInfo connect(@NonNull Context context, @NonNull PID pid) {
        checkNotNull(context);
        checkNotNull(pid);
        final String tag = RandomStringUtils.randomAlphabetic(10);
        final int timeout = Preferences.getConnectionTimeout(context);
        final IPFS ipfs = IPFS.getInstance(context);
        final boolean peerDiscovery = LiteService.isSupportPeerDiscovery(context);

        ConnectInfo info = new ConnectInfo();

        if (ipfs.isConnected(pid)) {
            info.setConnected();
            ipfs.protectPeer(pid, tag);
        } else {

            if (peerDiscovery) {
                PeerInfo peerInfo = IdentityService.getPeerInfo(context, pid, true);

                if (peerInfo != null) {
                    if (swarmConnect(context, peerInfo, tag)) {
                        info.setConnected();
                        ipfs.protectPeer(pid, tag);
                        return info;
                    }
                }
            }

            if (ipfs.swarmConnect(pid, timeout)) {
                info.setConnected();
                ipfs.protectPeer(pid, tag);
                return info;
            }
        }
        return info;
    }


    private static boolean swarmConnect(
            @NonNull Context context, @NonNull PeerInfo peer, @NonNull String tag) {
        checkNotNull(context);
        checkNotNull(peer);
        checkNotNull(tag);
        final int timeout = Preferences.getSwarmTimeout(context);

        final IPFS ipfs = IPFS.getInstance(context);

        Addresses addresses = peer.getAddresses();
        for (String relay : addresses.keySet()) {
            try {
                String ma = addresses.get(relay);
                checkNotNull(ma);
                PID relayPID = PID.create(relay);
                boolean relayConnected = ipfs.isConnected(relayPID);
                if (!relayConnected) {
                    relayConnected = ipfs.swarmConnect(
                            ma + "/" + IPFS.Style.p2p.name() + "/" + relay,
                            timeout);
                }
                if (relayConnected) {

                    if (!tag.isEmpty()) {
                        ipfs.protectPeer(PID.create(relay), tag);
                    }
                }

                if (relayConnected) {
                    String address = ipfs.relayAddress(ma, relayPID, peer.getPID());
                    ipfs.swarmConnect(address, timeout);
                }

            } catch (Throwable e) {
                Log.e(TAG, e.getLocalizedMessage(), e);
            }
        }

        return ipfs.isConnected(peer.getPID());

    }

    public static class ConnectInfo {

        private boolean connected = false;

        public boolean isConnected() {
            return connected;
        }

        void setConnected() {
            this.connected = true;
        }
    }
}
