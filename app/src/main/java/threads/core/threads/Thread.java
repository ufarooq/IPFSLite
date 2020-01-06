package threads.core.threads;

import android.provider.DocumentsContract;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import java.util.Objects;

import threads.core.Converter;
import threads.ipfs.api.CID;
import threads.ipfs.api.PID;
import threads.share.MimeType;

import static androidx.core.util.Preconditions.checkNotNull;

@androidx.room.Entity
public class Thread {

    @ColumnInfo(name = "thread")
    private final long thread; // checked
    @NonNull
    @TypeConverters(Kind.class)
    @ColumnInfo(name = "kind")
    private final Kind kind; // checked
    @NonNull
    @TypeConverters(Converter.class)
    @ColumnInfo(name = "sender")
    private final PID sender; // checked

    @ColumnInfo(name = "lastModified")
    private long lastModified; // checked

    @NonNull
    @ColumnInfo(name = "senderAlias")
    private String senderAlias; // checked
    @PrimaryKey(autoGenerate = true)
    private long idx; // checked
    @Nullable
    @ColumnInfo(name = "image")
    @TypeConverters(Converter.class)
    private CID image;  // checked
    @ColumnInfo(name = "marked")
    private boolean marked;  // todo
    @ColumnInfo(name = "number")
    private int number = 0;  // checked
    @ColumnInfo(name = "progress")
    private int progress = 0;  // checked
    @Nullable
    @TypeConverters(Converter.class)
    @ColumnInfo(name = "cid")
    private CID cid;  // checked
    @ColumnInfo(name = "expire")
    private long expire;  // checked
    @ColumnInfo(name = "size")
    private long size;  // checked
    @NonNull
    @TypeConverters(Status.class)
    @ColumnInfo(name = "status")
    private Status status;  // todo
    @NonNull
    @ColumnInfo(name = "mimeType")
    private String mimeType;  // checked
    @ColumnInfo(name = "pinned")
    private boolean pinned; // todo
    @ColumnInfo(name = "publishing")
    private boolean publishing; // todo
    @ColumnInfo(name = "leaching")
    private boolean leaching; // todo

    @NonNull
    @ColumnInfo(name = "name")
    private String name = "";


    Thread(@NonNull Status status,
           @NonNull PID sender,
           @NonNull String senderAlias,
           @NonNull Kind kind,
           long thread) {
        this.thread = thread;
        this.sender = sender;
        this.senderAlias = senderAlias;
        this.kind = kind;
        this.expire = System.currentTimeMillis();
        this.status = status;
        this.marked = false;
        this.lastModified = System.currentTimeMillis();
        this.mimeType = MimeType.PLAIN_MIME_TYPE;
        this.pinned = false;
        this.publishing = false;
        this.leaching = false;
    }

    public static Thread createThread(@NonNull Status status,
                                      @NonNull PID senderPid,
                                      @NonNull String senderAlias,
                                      @NonNull Kind kind,
                                      long thread) {
        checkNotNull(status);
        checkNotNull(senderPid);
        checkNotNull(senderAlias);
        checkNotNull(kind);
        return new Thread(status,
                senderPid, senderAlias, kind, thread);
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

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }


    public long getExpire() {
        return expire;
    }

    public void setExpire(long expire) {
        this.expire = expire;
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
    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(@NonNull String mimeType) {
        this.mimeType = mimeType;
    }

    @NonNull
    public PID getSender() {
        return sender;
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



    public boolean sameContent(@NonNull Thread o) {
        checkNotNull(o);
        if (this == o) return true;
        return number == o.getNumber() &&
                marked == o.isMarked() &&
                status == o.getStatus() &&
                pinned == o.isPinned() &&
                progress == o.getProgress() &&
                publishing == o.isPublishing() &&
                leaching == o.isLeaching() &&
                Objects.equals(cid, o.getCid()) &&
                Objects.equals(senderAlias, o.getSenderAlias()) &&
                Objects.equals(image, o.getImage()) &&
                Objects.equals(lastModified, o.getLastModified());
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

    public void increaseUnreadMessagesNumber() {
        number++;
    }

    public boolean isExpired() {
        return getExpire() < System.currentTimeMillis();
    }

    public boolean hasImage() {
        return image != null;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    public boolean isDir() {
        return DocumentsContract.Document.MIME_TYPE_DIR.equals(getMimeType());
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }
}
