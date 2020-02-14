package threads.server.core.peers;


import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.PrimaryKey;

import java.util.Objects;

import threads.ipfs.PID;

import static androidx.core.util.Preconditions.checkNotNull;

@androidx.room.Entity
public class Peer {


    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "pid")
    private final String pid;
    @NonNull
    @ColumnInfo(name = "multiAddress")
    private String multiAddress;
    @ColumnInfo(name = "relay")
    private boolean relay;
    @ColumnInfo(name = "autonat")
    private boolean autonat;


    Peer(@NonNull String pid, @NonNull String multiAddress) {
        this.pid = pid;
        this.multiAddress = multiAddress;
        this.relay = false;
        this.autonat = false;

    }

    static Peer createPeer(@NonNull PID pid, @NonNull String multiAddress) {
        checkNotNull(pid);
        checkNotNull(multiAddress);
        return new Peer(pid.getPid(), multiAddress);
    }

    public boolean isAutonat() {
        return autonat;
    }

    public void setAutonat(boolean autonat) {
        this.autonat = autonat;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Peer peer = (Peer) o;
        return pid.equals(peer.pid);
    }

    @Override
    @NonNull
    public String toString() {
        return "Peer{" +
                "pid='" + pid + '\'' +
                ", multiAddress='" + multiAddress + '\'' +
                ", relay=" + relay +
                ", autonat=" + autonat +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(pid);
    }

    @NonNull
    public String getPid() {
        return pid;
    }

    public boolean isRelay() {
        return relay;
    }

    public void setRelay(boolean relay) {
        this.relay = relay;
    }

    @NonNull
    public String getMultiAddress() {
        return multiAddress;
    }

    public void setMultiAddress(@NonNull String multiAddress) {
        checkNotNull(multiAddress);
        this.multiAddress = multiAddress;
    }

    @NonNull
    public PID getPID() {
        return PID.create(getPid());
    }

    public boolean areItemsTheSame(@NonNull Peer peer) {
        checkNotNull(peer);
        return this.pid.equals(peer.pid);

    }

    public boolean sameContent(@NonNull Peer peer) {
        checkNotNull(peer);
        if (this == peer) return true;
        return Objects.equals(autonat, peer.isAutonat()) &&
                Objects.equals(relay, peer.isRelay());
    }


}
