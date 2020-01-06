package threads.core.peers;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import java.util.Objects;

import threads.ipfs.api.PID;

import static androidx.core.util.Preconditions.checkNotNull;

@androidx.room.Entity
public class PeerInfo extends Basis {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "pid")
    private final String pid;

    @NonNull
    @TypeConverters(Addresses.class)
    @ColumnInfo(name = "addresses")
    private Addresses addresses = new Addresses();


    PeerInfo(@NonNull String pid) {
        this.pid = pid;
    }

    public static PeerInfo createPeerInfo(@NonNull PID pid) {
        checkNotNull(pid);
        return new PeerInfo(pid.getPid());
    }

    @NonNull
    public String getPid() {
        return pid;
    }

    public PID getPID() {
        return PID.create(pid);
    }

    @NonNull
    public Addresses getAddresses() {
        return (Addresses) addresses.clone();
    }

    public void setAddresses(@NonNull Addresses addresses) {
        this.addresses = addresses;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PeerInfo peer = (PeerInfo) o;
        return Objects.equals(pid, peer.pid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pid);
    }


    public void addAddress(@NonNull String relayPid, @NonNull String address) {
        checkNotNull(relayPid);
        checkNotNull(address);
        this.addresses.put(relayPid, address);
    }

    public void removeAddresses() {
        this.addresses.clear();
    }

    public void removeAddress(@NonNull String relayPid) {
        checkNotNull(relayPid);
        this.addresses.remove(relayPid);
    }

    public int numAddresses() {
        return this.addresses.size();
    }

    public boolean hasAddress(@NonNull PID pid) {
        return this.addresses.containsKey(pid.getPid());
    }

    public void addAddress(@NonNull PID pid, @NonNull String address) {
        checkNotNull(pid);
        checkNotNull(address);
        this.addresses.put(pid.getPid(), address);
    }

    public void removeAddress(@NonNull PID relay) {
        checkNotNull(relay);
        this.addresses.remove(relay.getPid());
    }

    @Override
    public String toString() {
        return "PeerInfo{" +
                "pid='" + pid + '\'' +
                ", timestamp=" + getTimestamp() +
                ", addresses=" + addresses +
                '}';
    }
}
