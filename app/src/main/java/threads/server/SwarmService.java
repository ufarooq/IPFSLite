package threads.server;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.RandomStringUtils;

import threads.core.ConnectService;
import threads.core.IdentityService;
import threads.core.Preferences;
import threads.core.Singleton;
import threads.core.api.PeerInfo;
import threads.ipfs.IPFS;
import threads.ipfs.api.PID;

import static androidx.core.util.Preconditions.checkNotNull;

public class SwarmService {


    public static ConnectInfo connect(@NonNull Context context, @NonNull PID pid) {
        checkNotNull(context);
        checkNotNull(pid);
        final String tag = RandomStringUtils.randomAlphabetic(10);
        final int timeout = Preferences.getConnectionTimeout(context);
        final IPFS ipfs = Singleton.getInstance(context).getIpfs();
        final boolean peerDiscovery = Service.isSupportPeerDiscovery(context);

        ConnectInfo info = new ConnectInfo(pid, tag);
        if (ipfs != null) {
            if (ipfs.isConnected(pid)) {
                info.setConnected(true);
                ipfs.protectPeer(pid, tag);
            } else {

                if (peerDiscovery) {
                    threads.core.api.PeerInfo peerInfo = IdentityService.getPeerInfo(
                            context, pid, BuildConfig.ApiAesKey, true);

                    if (peerInfo != null) {
                        info.setPeerInfo(peerInfo);
                        if (ConnectService.swarmConnect(context, peerInfo, tag)) {
                            info.setConnected(true);

                            ipfs.protectPeer(pid, tag);

                            return info;
                        }

                    }

                }

                if (ipfs.swarmConnect(pid, timeout)) {
                    info.setConnected(true);
                    ipfs.protectPeer(pid, tag);
                    return info;
                }

            }
        }

        return info;
    }


    public static void disconnect(@NonNull Context context, @NonNull ConnectInfo info) {
        checkNotNull(context);
        checkNotNull(info);
        final IPFS ipfs = Singleton.getInstance(context).getIpfs();
        if (ipfs != null) {
            if (info.isConnected()) {
                ipfs.unProtectPeer(info.pid, info.tag);
            }
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
        private threads.core.api.PeerInfo peerInfo;

        ConnectInfo(PID pid, String tag) {

            this.pid = pid;
            this.tag = tag;
            this.connected = false;
        }

        @Nullable
        public PeerInfo getPeerInfo() {
            return peerInfo;
        }

        public void setPeerInfo(@Nullable PeerInfo peerInfo) {
            this.peerInfo = peerInfo;
        }

        public boolean isConnected() {
            return connected;
        }

        public void setConnected(boolean connected) {
            this.connected = connected;
        }
    }
}
