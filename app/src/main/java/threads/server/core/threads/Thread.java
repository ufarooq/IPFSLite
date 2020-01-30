package threads.server.core.threads;

import android.provider.DocumentsContract;

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
public class Thread {

    @ColumnInfo(name = "parent")
    private final long parent; // checked

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
    @ColumnInfo(name = "thumbnail")
    @TypeConverters(Converter.class)
    private CID thumbnail;  // checked
    @ColumnInfo(name = "number")
    private int number = 0;  // checked
    @ColumnInfo(name = "progress")
    private int progress;  // checked
    @Nullable
    @TypeConverters(Converter.class)
    @ColumnInfo(name = "content")
    private CID content;  // checked
    @ColumnInfo(name = "size")
    private long size;  // checked
    @NonNull
    @ColumnInfo(name = "mimeType")
    private String mimeType;  // checked
    @ColumnInfo(name = "pinned")
    private boolean pinned; // checked
    @ColumnInfo(name = "publishing")
    private boolean publishing; // checked
    @ColumnInfo(name = "leaching")
    private boolean leaching; // checked
    @ColumnInfo(name = "seeding")
    private boolean seeding; // checked
    @ColumnInfo(name = "deleting")
    private boolean deleting; // checked
    @NonNull
    @ColumnInfo(name = "name")
    private String name = "";


    Thread(@NonNull PID sender,
           @NonNull String senderAlias,
           long parent) {
        this.parent = parent;
        this.sender = sender;
        this.senderAlias = senderAlias;
        this.lastModified = System.currentTimeMillis();
        this.mimeType = "";
        this.pinned = false;
        this.publishing = false;
        this.leaching = false;
        this.seeding = false;
        this.deleting = false;
        this.progress = 0;
    }

    static Thread createThread(@NonNull PID senderPid, @NonNull String senderAlias, long parent) {
        checkNotNull(senderPid);
        checkNotNull(senderAlias);
        return new Thread(senderPid, senderAlias, parent);
    }

    public boolean isLeaching() {
        return leaching;
    }

    public void setLeaching(boolean leaching) {
        this.leaching = leaching;
    }


    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
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
        checkNotNull(mimeType);
        this.mimeType = mimeType;
    }

    @NonNull
    public PID getSender() {
        return sender;
    }


    public boolean sameContent(@NonNull Thread o) {
        checkNotNull(o);
        if (this == o) return true;
        return number == o.getNumber() &&
                pinned == o.isPinned() &&
                progress == o.getProgress() &&
                publishing == o.isPublishing() &&
                leaching == o.isLeaching() &&
                seeding == o.isSeeding() &&
                deleting == o.isDeleting() &&
                Objects.equals(mimeType, o.getMimeType()) &&
                Objects.equals(content, o.getContent()) &&
                Objects.equals(senderAlias, o.getSenderAlias()) &&
                Objects.equals(thumbnail, o.getThumbnail()) &&
                Objects.equals(lastModified, o.getLastModified());
    }

    @Nullable
    public CID getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(@Nullable CID thumbnail) {
        this.thumbnail = thumbnail;
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
    public CID getContent() {
        return content;
    }

    public void setContent(@Nullable CID content) {
        this.content = content;
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

    public long getParent() {
        return parent;
    }

    public boolean hasImage() {
        return thumbnail != null;
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

    public boolean isSeeding() {
        return seeding;
    }

    public void setSeeding(boolean seeding) {
        this.seeding = seeding;
    }

    public boolean isDeleting() {
        return deleting;
    }

    public void setDeleting(boolean deleting) {
        this.deleting = deleting;
    }
}
