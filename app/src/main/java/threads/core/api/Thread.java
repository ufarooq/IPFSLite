package threads.core.api;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import java.util.Objects;

import threads.core.MimeType;
import threads.ipfs.api.CID;
import threads.ipfs.api.PID;

import static androidx.core.util.Preconditions.checkNotNull;

@androidx.room.Entity
public class Thread extends Entity {

    @ColumnInfo(name = "thread")
    private final long thread;
    @NonNull
    @TypeConverters(Kind.class)
    @ColumnInfo(name = "kind")
    private final Kind kind;
    @NonNull
    @TypeConverters(Converter.class)
    @ColumnInfo(name = "senderPid")
    private final PID senderPid;
    @NonNull
    @ColumnInfo(name = "senderKey")
    private final String senderKey;
    @NonNull
    @ColumnInfo(name = "sesKey")
    private final String sesKey;
    @ColumnInfo(name = "date")
    private long date;
    @NonNull
    @ColumnInfo(name = "senderAlias")
    private String senderAlias;
    @PrimaryKey(autoGenerate = true)
    private long idx;
    @Nullable
    @ColumnInfo(name = "image")
    @TypeConverters(Converter.class)
    private CID image;
    @ColumnInfo(name = "marked")
    private boolean marked;
    @ColumnInfo(name = "number")
    private int number = 0;
    @Nullable
    @TypeConverters(Converter.class)
    @ColumnInfo(name = "cid")
    private CID cid;
    @ColumnInfo(name = "expire")
    private long expire;
    @NonNull
    @TypeConverters(Status.class)
    @ColumnInfo(name = "status")
    private Status status;
    @NonNull
    @TypeConverters(Members.class)
    @ColumnInfo(name = "members")
    private Members members = new Members();
    @NonNull
    @ColumnInfo(name = "mimeType")
    private String mimeType;
    @ColumnInfo(name = "pinned")
    private boolean pinned;
    @ColumnInfo(name = "publishing")
    private boolean publishing;
    @ColumnInfo(name = "leaching")
    private boolean leaching;
    @ColumnInfo(name = "request")
    private boolean request;
    @ColumnInfo(name = "blocked")
    private boolean blocked;

    @Nullable
    @ColumnInfo(name = "title")
    private String title;

    Thread(@NonNull Status status,
           @NonNull PID senderPid,
           @NonNull String senderAlias,
           @NonNull String senderKey,
           @NonNull String sesKey,
           @NonNull Kind kind,
           long date,
           long thread) {
        this.thread = thread;
        this.senderPid = senderPid;
        this.senderAlias = senderAlias;
        this.senderKey = senderKey;
        this.sesKey = sesKey;
        this.kind = kind;
        this.expire = System.currentTimeMillis();
        this.status = status;
        this.marked = false;
        this.date = date;
        this.mimeType = MimeType.PLAIN_MIME_TYPE;
        this.pinned = false;
        this.publishing = false;
        this.request = false;
        this.blocked = false;
        this.leaching = false;
    }

    public static Thread createThread(@NonNull Status status,
                                      @NonNull PID senderPid,
                                      @NonNull String senderAlias,
                                      @NonNull String senderKey,
                                      @NonNull String sesKey,
                                      @NonNull Kind kind,
                                      long date,
                                      long thread) {
        checkNotNull(status);
        checkNotNull(senderPid);
        checkNotNull(senderAlias);
        checkNotNull(senderKey);
        checkNotNull(sesKey);
        checkNotNull(kind);
        return new Thread(status,
                senderPid, senderAlias, senderKey,
                sesKey, kind, date, thread);
    }

    @Nullable
    public String getTitle() {
        return title;
    }

    public void setTitle(@Nullable String title) {
        this.title = title;
    }

    public boolean isLeaching() {
        return leaching;
    }

    public void setLeaching(boolean leaching) {
        this.leaching = leaching;
    }

    public boolean isMarked() {
        return marked;
    }

    public void setMarked(boolean marked) {
        this.marked = marked;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }


    public long getExpire() {
        return expire;
    }

    public void setExpire(long expire) {
        this.expire = expire;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public boolean isRequest() {
        return request;
    }

    public void setRequest(boolean request) {
        this.request = request;
    }

    public boolean isPublishing() {
        return publishing;
    }

    public void setPublishing(boolean publishing) {
        this.publishing = publishing;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    public long getIdx() {
        return idx;
    }

    void setIdx(long idx) {
        this.idx = idx;
    }

    @NonNull
    public String getSenderBox() {
        return AddressType.getAddress(getSenderPid(), AddressType.INBOX);
    }

    @NonNull
    public String getSenderKey() {
        return senderKey;
    }

    @NonNull
    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(@NonNull String mimeType) {
        this.mimeType = mimeType;
    }

    @NonNull
    public PID getSenderPid() {
        return senderPid;
    }

    public long getExpireDate() {
        return expire;
    }

    public void setExpireDate(long expireDate) {
        this.expire = expireDate;
    }

    @NonNull
    public Status getStatus() {
        return status;
    }

    public void setStatus(@NonNull Status status) {
        checkNotNull(status);
        this.status = status;
    }

    @NonNull
    public Kind getKind() {
        return kind;
    }

    @NonNull
    public Members getMembers() {
        return members;
    }

    public void setMembers(@NonNull Members members) {
        this.members = members;
    }

    public boolean addMember(@NonNull PID pid) {
        checkNotNull(pid);
        return this.members.add(pid);
    }

    public boolean removeMember(@NonNull PID pid) {
        checkNotNull(pid);
        return this.members.remove(pid);
    }

    public boolean sameThread(@NonNull Thread o) {
        checkNotNull(o);
        if (this == o) return true;
        return Objects.equals(cid, o.getCid()) &&
                Objects.equals(senderPid, o.getSenderPid()) &&
                Objects.equals(image, o.getImage()) &&
                Objects.equals(title, o.getTitle());
    }

    public boolean sameContent(@NonNull Thread o) {
        checkNotNull(o);
        if (this == o) return true;
        return number == o.getNumber() &&
                marked == o.isMarked() &&
                status == o.getStatus() &&
                pinned == o.isPinned() &&
                publishing == o.isPublishing() &&
                leaching == o.isLeaching() &&
                request == o.isRequest() &&
                blocked == o.isBlocked() &&
                Objects.equals(cid, o.getCid()) &&
                Objects.equals(senderAlias, o.getSenderAlias()) &&
                Objects.equals(image, o.getImage()) &&
                Objects.equals(date, o.getDate());
    }

    @Nullable
    public CID getImage() {
        return image;
    }

    public void setImage(@Nullable CID image) {
        this.image = image;
    }

    public boolean areItemsTheSame(@NonNull Thread thread) {
        checkNotNull(thread);
        return idx == thread.getIdx();

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Thread thread = (Thread) o;
        return getIdx() == thread.getIdx();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getIdx());
    }

    @Nullable
    public CID getCid() {
        return cid;
    }

    public void setCid(@Nullable CID cid) {
        this.cid = cid;
    }

    @NonNull
    public String getSenderAlias() {
        return senderAlias;
    }

    public void setSenderAlias(@NonNull String senderAlias) {
        checkNotNull(senderAlias);
        this.senderAlias = senderAlias;
    }


    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public long getThread() {
        return thread;
    }

    @NonNull
    public String getSesKey() {
        return sesKey;
    }

    public void increaseUnreadMessagesNumber() {
        number++;
    }

    public boolean isEncrypted() {
        return !sesKey.isEmpty();
    }

    public boolean isExpired() {
        return getExpire() < System.currentTimeMillis();
    }
}