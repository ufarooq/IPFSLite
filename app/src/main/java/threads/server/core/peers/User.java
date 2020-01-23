package threads.server.core.peers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import java.util.Objects;

import threads.ipfs.CID;
import threads.ipfs.PID;
import threads.server.core.Converter;

import static androidx.core.util.Preconditions.checkNotNull;


@androidx.room.Entity
public class User extends Basis implements IPeer {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "pid")
    private final String pid;
    @NonNull
    @ColumnInfo(name = "publicKey")
    private String publicKey;
    @NonNull
    @ColumnInfo(name = "alias")
    private String alias;
    @NonNull
    @TypeConverters(UserType.class)
    @ColumnInfo(name = "type")
    private UserType type;

    @ColumnInfo(name = "autoConnect")
    private boolean autoConnect;
    @ColumnInfo(name = "connected")
    private boolean connected;
    @Nullable
    @ColumnInfo(name = "image")
    @TypeConverters(Converter.class)
    private CID image;
    @ColumnInfo(name = "blocked")
    private boolean blocked;
    @ColumnInfo(name = "dialing")
    private boolean dialing;

    User(@NonNull UserType type,
         @NonNull String alias,
         @NonNull String publicKey,
         @NonNull String pid,
         @Nullable CID image) {
        this.type = type;
        this.alias = alias;
        this.publicKey = publicKey;
        this.pid = pid;
        this.image = image;
        this.blocked = false;
        this.dialing = false;
        this.connected = false;
        this.autoConnect = false;
    }

    @NonNull
    public static User createUser(@NonNull UserType type,
                                  @NonNull String alias,
                                  @NonNull String publicKey,
                                  @NonNull PID pid,
                                  @Nullable CID image) {
        checkNotNull(type);

        checkNotNull(alias);
        checkNotNull(publicKey);
        checkNotNull(pid);
        return new User(type, alias, publicKey, pid.getPid(), image);
    }

    public boolean isAutoConnect() {
        return autoConnect;
    }

    public void setAutoConnect(boolean autoConnect) {
        this.autoConnect = autoConnect;
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public boolean isDialing() {
        return dialing;
    }

    public void setDialing(boolean dialing) {
        this.dialing = dialing;
    }


    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    @NonNull
    public UserType getType() {
        return type;
    }

    public void setType(@NonNull UserType type) {
        checkNotNull(type);
        this.type = type;
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

    @NonNull
    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(@NonNull String publicKey) {
        checkNotNull(publicKey);
        this.publicKey = publicKey;
    }

    @Nullable
    public CID getImage() {
        return image;
    }


    public void setImage(@Nullable CID image) {
        this.image = image;
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
        checkNotNull(user);
        return this.pid.equals(user.pid);

    }

    public boolean sameContent(@NonNull User user) {
        checkNotNull(user);
        if (this == user) return true;
        return Objects.equals(connected, user.isConnected()) &&
                Objects.equals(autoConnect, user.isAutoConnect()) &&
                Objects.equals(dialing, user.isDialing()) &&
                Objects.equals(alias, user.getAlias()) &&
                Objects.equals(blocked, user.isBlocked()) &&
                Objects.equals(image, user.getImage()) &&
                Objects.equals(publicKey, user.getPublicKey());
    }

    @Override
    @NonNull
    public PID getPID() {
        return PID.create(pid);
    }


    public boolean isValid() {
        return !publicKey.isEmpty();
    }
}
