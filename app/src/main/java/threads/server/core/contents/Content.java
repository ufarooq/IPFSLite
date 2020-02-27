package threads.server.core.contents;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.PrimaryKey;

import threads.ipfs.CID;


@androidx.room.Entity
public class Content {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "cid")
    private final String cid;
    @NonNull
    @ColumnInfo(name = "pid")
    private final String pid;

    @ColumnInfo(name = "timestamp")
    private long timestamp;

    @SuppressWarnings("CanBeFinal")
    @ColumnInfo(name = "finished")
    private boolean finished;

    Content(@NonNull String pid, @NonNull String cid, long timestamp, boolean finished) {
        this.pid = pid;
        this.cid = cid;
        this.timestamp = timestamp;
        this.finished = finished;
    }

    public static Content create(@NonNull String pid, @NonNull String cid, boolean finished) {

        return new Content(pid, cid, System.currentTimeMillis(), finished);
    }

    boolean isFinished() {
        return finished;
    }

    @NonNull
    public String getPid() {
        return pid;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @NonNull
    public String getCid() {
        return cid;
    }

    @NonNull
    public CID getCID() {
        return CID.create(cid);
    }
}
