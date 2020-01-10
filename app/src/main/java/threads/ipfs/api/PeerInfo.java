package threads.ipfs.api;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static androidx.core.util.Preconditions.checkNotNull;

public class PeerInfo {
    private static final String ID = "ID";
    private static final String PublicKey = "PublicKey";
    private static final String AgentVersion = "AgentVersion";
    private static final String Addresses = "Addresses";
    private static final String ProtocolVersion = "ProtocolVersion";


    @NonNull
    private final PID pid;
    @Nullable
    private String publicKey;
    @NonNull
    private String agentVersion = "";
    @NonNull
    private String protocolVersion = "";
    @NonNull
    private List<String> multiAddresses = new ArrayList<>();

    private PeerInfo(@NonNull PID pid) {
        checkNotNull(pid);
        this.pid = pid;
    }

    public static PeerInfo create(@NonNull Map map) {
        checkNotNull(map);
        String id = (String) map.get(ID);
        checkNotNull(id);
        PID pid = PID.create(id);
        PeerInfo peerInfo = new PeerInfo(pid);
        String publicKey = (String) map.get(PublicKey);
        if (publicKey != null) {
            peerInfo.setPublicKey(publicKey);
        }
        String agentVersion = (String) map.get(AgentVersion);
        if (agentVersion != null) {
            peerInfo.setAgentVersion(agentVersion);
        }
        String protocolVersion = (String) map.get(ProtocolVersion);
        if (protocolVersion != null) {
            peerInfo.setProtocolVersion(protocolVersion);
        }
        Object result = map.get(Addresses);
        if (result instanceof List) {
            for (Object object : (List) result) {
                if (object instanceof String) {
                    peerInfo.addAddress((String) object);
                }
            }
        }
        return peerInfo;
    }

    public boolean isLiteAgent() {
        return agentVersion.startsWith("ipfs-lite/");
    }

    @NonNull
    public PID getPID() {
        return pid;
    }

    @Nullable
    public String getPublicKey() {
        return publicKey;
    }

    private void setPublicKey(@NonNull String publicKey) {
        checkNotNull(publicKey);
        this.publicKey = publicKey;
    }

    @NonNull
    public String getAgentVersion() {
        return agentVersion;
    }

    private void setAgentVersion(@NonNull String agentVersion) {
        checkNotNull(agentVersion);
        this.agentVersion = agentVersion;
    }

    @NonNull
    public String getProtocolVersion() {
        return protocolVersion;
    }

    private void setProtocolVersion(@NonNull String protocolVersion) {
        checkNotNull(protocolVersion);
        this.protocolVersion = protocolVersion;
    }

    @Override
    @NonNull
    public String toString() {
        return "PeerInfo{" +
                "pid=" + pid +
                ", publicKey='" + publicKey + '\'' +
                ", agentVersion='" + agentVersion + '\'' +
                ", protocolVersion='" + protocolVersion + '\'' +
                ", multiAddresses=" + multiAddresses +
                '}';
    }

    @NonNull
    public List<String> getMultiAddresses() {
        return multiAddresses;
    }

    private void addAddress(@NonNull String multiAddress) {
        checkNotNull(multiAddress);
        multiAddresses.add(multiAddress);
    }

}
