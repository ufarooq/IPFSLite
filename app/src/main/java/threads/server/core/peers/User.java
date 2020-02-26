package threads.server.core.peers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.PrimaryKey;

import java.util.Objects;

import threads.ipfs.PID;


@androidx.room.Entity
public class User {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "pid")
    private final String pid;
    @Nullable
    @ColumnInfo(name = "publicKey")
    private String publicKey;
    @NonNull
    @ColumnInfo(name = "alias")
    private String alias;
    @ColumnInfo(name = "connected")
    private boolean connected;
    @ColumnInfo(name = "blocked")
    private boolean blocked;
    @ColumnInfo(name = "dialing")
    private boolean dialing;
    @ColumnInfo(name = "lite")
    private boolean lite;

    @NonNull
    @ColumnInfo(name = "address")
    private String address;

    @Nullable
    @ColumnInfo(name = "agent")
    private String agent;

    User(@NonNull String alias, @NonNull String pid) {
        this.alias = alias;
        this.pid = pid;
        this.blocked = false;
        this.dialing = false;
        this.connected = false;
        this.lite = false;
        this.address = "";
    }

    @NonNull
    static User createUser(@NonNull String alias, @NonNull PID pid) {

        return new User(alias, pid.getPid());
    }

    public boolean isLite() {
        return lite;
    }

    public void setLite(boolean lite) {
        this.lite = lite;
    }


    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public boolean isDialing() {
        return dialing;
    }

    void setDialing(boolean dialing) {
        this.dialing = dialing;
    }


    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    @NonNull
    public String getPid() {
        return pid;
    }

    @NonNull
    public String getAlias() {
        return alias;
    }

    public void setAlias(@NonNull String alias) {
        this.alias = alias;
    }

    @Nullable
    public String getPublicKey() {
        return publicKey;
    }


    public void setPublicKey(@Nullable String publicKey) {
        this.publicKey = publicKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(pid, user.pid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pid);
    }

    public boolean areItemsTheSame(@NonNull User user) {

        return this.pid.equals(user.pid);

    }

    public boolean sameContent(@NonNull User user) {

        if (this == user) return true;
        return Objects.equals(connected, user.isConnected()) &&
                Objects.equals(dialing, user.isDialing()) &&
                Objects.equals(alias, user.getAlias()) &&
                Objects.equals(lite, user.isLite()) &&
                Objects.equals(blocked, user.isBlocked()) &&
                Objects.equals(publicKey, user.getPublicKey());
    }

    @NonNull
    public PID getPID() {
        return PID.create(pid);
    }


    @Nullable
    public String getAgent() {
        return agent;
    }

    public void setAgent(@Nullable String agent) {
        this.agent = agent;
    }

    @NonNull
    public String getAddress() {
        return address;
    }

    public void setAddress(@NonNull String address) {
        this.address = address;
    }
}
