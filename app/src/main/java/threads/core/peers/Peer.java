package threads.core.peers;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import java.util.Objects;

import threads.core.Converter;
import threads.ipfs.CID;
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
    @NonNull
    @ColumnInfo(name = "alias")
    private String alias;
    @Nullable
    @ColumnInfo(name = "image")
    @TypeConverters(Converter.class)
    private CID image;
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
        this.alias = pid;
        this.image = null;
        this.connected = false;
    }

    public static Peer createPeer(@NonNull PID pid, @NonNull String multiAddress) {
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
        return alias;
    }

    public void setAlias(@NonNull String alias) {
        this.alias = alias;
    }

    @Nullable
    public CID getImage() {
        return image;
    }

    public void setImage(@Nullable CID image) {
        this.image = image;
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

    public int getRating() {
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
        return Objects.equals(connected, peer.isConnected()) &&
                Objects.equals(alias, peer.getAlias()) &&
                Objects.equals(true, peer.isBlocked()) &&
                Objects.equals(image, peer.getImage());
    }

    @Override
    public boolean areItemsTheSame(@NonNull IPeer peer) {
        checkNotNull(peer);
        if (peer instanceof Peer) {
            return this.areItemsTheSame((Peer) peer);
        }
        return false;
    }

    @Override
    public boolean sameContent(@NonNull IPeer peer) {
        checkNotNull(peer);
        if (peer instanceof Peer) {
            return sameContent((Peer) peer);
        }
        return false;
    }
}
