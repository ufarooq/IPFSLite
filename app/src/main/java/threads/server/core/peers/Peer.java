package threads.server.core.peers;


import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.PrimaryKey;

import java.util.Objects;

import threads.ipfs.PID;

import static androidx.core.util.Preconditions.checkNotNull;

@androidx.room.Entity
public class Peer extends Basis implements IPeer, Comparable<Peer> {


    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "pid")
    private final String pid;
    @NonNull
    @ColumnInfo(name = "multiAddress")
    private String multiAddress;
    @ColumnInfo(name = "isRelay")
    private boolean isRelay;
    @ColumnInfo(name = "isAutonat")
    private boolean isAutonat;
    @ColumnInfo(name = "isPubsub")
    private boolean isPubsub;
    @ColumnInfo(name = "rating")
    private int rating;
    @ColumnInfo(name = "connected")
    private boolean connected;

    Peer(@NonNull String pid, @NonNull String multiAddress) {
        super();
        this.pid = pid;
        this.multiAddress = multiAddress;
        this.isRelay = false;
        this.isAutonat = false;
        this.isPubsub = false;
        this.rating = 0;
        this.connected = false;
    }

    static Peer createPeer(@NonNull PID pid, @NonNull String multiAddress) {
        checkNotNull(pid);
        checkNotNull(multiAddress);
        return new Peer(pid.getPid(), multiAddress);
    }


    @Override
    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    @Override
    public boolean isDialing() {
        return false;
    }

    @NonNull
    public String getAlias() {
        return pid;
    }

    public boolean isPubsub() {
        return isPubsub;
    }

    public void setPubsub(boolean pubsub) {
        isPubsub = pubsub;
    }

    public boolean isAutonat() {
        return isAutonat;
    }

    public void setAutonat(boolean autonat) {
        isAutonat = autonat;
    }

    int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
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
                ", isRelay=" + isRelay +
                ", isAutonat=" + isAutonat +
                ", isPubsub=" + isPubsub +
                ", rating=" + rating +
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
        return isRelay;
    }

    public void setRelay(boolean relay) {
        isRelay = relay;
    }

    @NonNull
    public String getMultiAddress() {
        return multiAddress;
    }

    public void setMultiAddress(@NonNull String multiAddress) {
        checkNotNull(multiAddress);
        this.multiAddress = multiAddress;
    }

    @Override
    public int compareTo(@NonNull Peer peer) {
        return Double.compare(peer.rating, this.rating);
    }

    @Override
    @NonNull
    public PID getPID() {
        return PID.create(getPid());
    }

    public boolean areItemsTheSame(@NonNull Peer peer) {
        checkNotNull(peer);
        return this.pid.equals(peer.pid);

    }

    public boolean isBlocked() {
        return true;
    }

    public boolean sameContent(@NonNull Peer peer) {
        checkNotNull(peer);
        if (this == peer) return true;
        return Objects.equals(connected, peer.isConnected());
    }


}
