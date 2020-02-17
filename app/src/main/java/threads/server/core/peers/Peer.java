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


    Peer(@NonNull String pid) {
        this.pid = pid;

    }

    static Peer createPeer(@NonNull PID pid) {
        checkNotNull(pid);
        return new Peer(pid.getPid());
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

    @NonNull
    public PID getPID() {
        return PID.create(getPid());
    }

    public boolean areItemsTheSame(@NonNull Peer peer) {
        checkNotNull(peer);
        return this.pid.equals(peer.pid);

    }

    public boolean sameContent(@NonNull Peer peer) {
        return areItemsTheSame(peer);
    }


}
