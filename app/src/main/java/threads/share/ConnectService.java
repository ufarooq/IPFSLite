package threads.share;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import threads.core.Preferences;
import threads.core.peers.Addresses;
import threads.core.peers.PeerInfo;
import threads.ipfs.IPFS;
import threads.ipfs.PID;

import static androidx.core.util.Preconditions.checkArgument;
import static androidx.core.util.Preconditions.checkNotNull;

public class ConnectService {

    private static final String TAG = ConnectService.class.getSimpleName();


    public static boolean connectPeer(@NonNull Context context, @NonNull PID pid,
                                      boolean supportDiscovery, boolean updateUser, int timeout) {

        checkNotNull(context);

        checkNotNull(pid);

        checkArgument(timeout > 0);
        if (!Network.isConnected(context)) {
            return false;
        }


        if (supportDiscovery) {
            PeerInfo peer = IdentityService.getPeerInfo(
                    context, pid, updateUser);
            if (peer != null) {

                if (swarmConnect(context, peer, "")) {
                    return true;
                }

            }

        }
        final IPFS ipfs = IPFS.getInstance(context);
        if (ipfs != null) {
            ipfs.swarmConnect(pid, timeout);

            return ipfs.isConnected(pid);

        }
        return false;
    }


    public static void swarmUnProtect(@NonNull Context context,
                                      @NonNull PeerInfo peer,
                                      @NonNull String tag) {
        checkNotNull(context);
        checkNotNull(peer);
        checkNotNull(tag);
        final IPFS ipfs = IPFS.getInstance(context);
        if (ipfs != null) {
            Addresses addresses = peer.getAddresses();
            for (String relay : addresses.keySet()) {
                PID pid = PID.create(relay);
                if (!tag.isEmpty()) {
                    ipfs.unProtectPeer(pid, tag);
                }
            }
        }
    }

    public static boolean swarmConnect(@NonNull Context context,
                                       @NonNull PeerInfo peer,
                                       @NonNull String tag) {
        checkNotNull(context);
        checkNotNull(peer);

        final int timeout = Preferences.getSwarmTimeout(context);

        final IPFS ipfs = IPFS.getInstance(context);
        if (ipfs != null) {
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
        return false;
    }


}
