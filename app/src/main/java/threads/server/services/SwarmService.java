package threads.server.services;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.RandomStringUtils;

import threads.ipfs.IPFS;
import threads.ipfs.PID;
import threads.server.core.peers.PeerInfo;
import threads.server.utils.Preferences;

import static androidx.core.util.Preconditions.checkNotNull;

public class SwarmService {


    public static ConnectInfo connect(@NonNull Context context, @NonNull PID pid) {
        checkNotNull(context);
        checkNotNull(pid);
        final String tag = RandomStringUtils.randomAlphabetic(10);
        final int timeout = Preferences.getConnectionTimeout(context);
        final IPFS ipfs = IPFS.getInstance(context);
        final boolean peerDiscovery = Service.isSupportPeerDiscovery(context);

        ConnectInfo info = new ConnectInfo(pid, tag);

        if (ipfs.isConnected(pid)) {
            info.setConnected();
            ipfs.protectPeer(pid, tag);
        } else {

            if (peerDiscovery) {
                PeerInfo peerInfo = IdentityService.getPeerInfo(
                        context, pid, true);

                if (peerInfo != null) {
                    info.setPeerInfo(peerInfo);
                    if (ConnectService.swarmConnect(context, peerInfo, tag)) {
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


    public static void disconnect(@NonNull Context context, @NonNull ConnectInfo info) {
        checkNotNull(context);
        checkNotNull(info);
        IPFS ipfs = IPFS.getInstance(context);

        if (info.isConnected()) {
            ipfs.unProtectPeer(info.pid, info.tag);
        }

        if (info.getPeerInfo() != null) {
            ConnectService.swarmUnProtect(context, info.getPeerInfo(), info.tag);
        }
    }

    public static class ConnectInfo {

        private final PID pid;
        private final String tag;
        private boolean connected;
        @Nullable
        private PeerInfo peerInfo;

        ConnectInfo(PID pid, String tag) {

            this.pid = pid;
            this.tag = tag;
            this.connected = false;
        }

        @Nullable
        PeerInfo getPeerInfo() {
            return peerInfo;
        }

        void setPeerInfo(@Nullable PeerInfo peerInfo) {
            this.peerInfo = peerInfo;
        }

        boolean isConnected() {
            return connected;
        }

        void setConnected() {
            this.connected = true;
        }
    }
}