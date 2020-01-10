package threads.ipfs.api;

import androidx.annotation.NonNull;

public class IdentityConfig {
    @NonNull
    private String PeerID = "";

    @NonNull
    private String PrivKey = "";

    public static IdentityConfig create() {
        return new IdentityConfig();
    }

    @NonNull
    public String getPrivKey() {
        return PrivKey;
    }

    public void setPrivKey(@NonNull String privKey) {
        PrivKey = privKey;
    }

    @Override
    public String toString() {
        return "IdentityConfig{" +
                "PeerID='" + PeerID + '\'' +
                '}';
    }

    @NonNull
    public String getPeerID() {
        return PeerID;
    }

    public void setPeerID(@NonNull String peerID) {
        PeerID = peerID;
    }


    public PID getPID() {
        return PID.create(PeerID);
    }
}
