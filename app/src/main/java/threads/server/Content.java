package threads.server;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import threads.core.api.Converter;
import threads.ipfs.api.CID;
import threads.ipfs.api.PID;

import static androidx.core.util.Preconditions.checkNotNull;


@androidx.room.Entity
public class Content {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "cid")
    private final String cid;
    @NonNull
    @TypeConverters(Converter.class)
    @ColumnInfo(name = "pid")
    private final PID pid;
    @ColumnInfo(name = "timestamp")
    private long timestamp;

    @ColumnInfo(name = "finished")
    private boolean finished;

    Content(@NonNull PID pid, @NonNull String cid, long timestamp, boolean finished) {
        this.pid = pid;
        this.cid = cid;
        this.timestamp = timestamp;
        this.finished = finished;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public static Content create(@NonNull PID pid, @NonNull CID cid, boolean finished) {
        checkNotNull(pid);
        checkNotNull(cid);
        return new Content(pid, cid.getCid(), System.currentTimeMillis(), finished);
    }

    @NonNull
    public PID getPid() {
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
