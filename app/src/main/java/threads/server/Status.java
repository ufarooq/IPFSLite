package threads.server;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

import static com.google.common.base.Preconditions.checkNotNull;

@Entity(tableName = "Status")
public class Status {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "uid")
    private final String uid;
    private boolean networkAvailable = false;
    private boolean serverRunning = false;
    private boolean serverReachable = false;

    Status(@NonNull String uid) {
        checkNotNull(uid);
        this.uid = uid;
    }

    public static Status createStatus(@NonNull String uid) {
        checkNotNull(uid);
        return new Status(uid);
    }

    public String getUid() {
        return uid;
    }

    public boolean isNetworkAvailable() {
        return networkAvailable;
    }

    public void setNetworkAvailable(boolean networkAvailable) {
        this.networkAvailable = networkAvailable;
    }

    public boolean isServerRunning() {
        return serverRunning;
    }

    public void setServerRunning(boolean serverRunning) {
        this.serverRunning = serverRunning;
    }

    public boolean isServerReachable() {
        return serverReachable;
    }

    public void setServerReachable(boolean serverReachable) {
        this.serverReachable = serverReachable;
    }
}
