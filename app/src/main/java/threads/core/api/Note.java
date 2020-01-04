package threads.core.api;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import java.util.Objects;

import threads.ipfs.api.CID;
import threads.ipfs.api.PID;

import static androidx.core.util.Preconditions.checkNotNull;


@androidx.room.Entity
public class Note extends Entity {

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
    @NonNull
    @TypeConverters(Kind.class)
    @ColumnInfo(name = "kind")
    private final Kind kind;

    @ColumnInfo(name = "thread")
    private final long thread;

    @NonNull
    @TypeConverters(NoteType.class)
    @ColumnInfo(name = "noteType")
    private final NoteType noteType;
    @NonNull
    @ColumnInfo(name = "mimeType")
    private final String mimeType;
    @ColumnInfo(name = "date")
    private final long date;
    @NonNull
    @ColumnInfo(name = "senderAlias")
    private String senderAlias;
    @PrimaryKey(autoGenerate = true)
    private long idx;
    @Nullable
    @TypeConverters(Converter.class)
    @ColumnInfo(name = "cid")
    private CID cid;
    @NonNull
    @TypeConverters(Status.class)
    @ColumnInfo(name = "status")
    private Status status;
    @Nullable
    @ColumnInfo(name = "image")
    @TypeConverters(Converter.class)
    private CID image;
    @ColumnInfo(name = "publishing")
    private boolean publishing;
    @ColumnInfo(name = "leaching")
    private boolean leaching;
    @ColumnInfo(name = "expire")
    private long expire;
    @ColumnInfo(name = "blocked")
    private boolean blocked;

    @ColumnInfo(name = "number")
    private int number = 0;

    Note(long thread,
         @NonNull PID senderPid,
         @NonNull String senderAlias,
         @NonNull String senderKey,
         @NonNull String sesKey,
         @NonNull String mimeType,
         @NonNull Status status,
         @NonNull Kind kind,
         @NonNull NoteType noteType,
         long date) {
        this.thread = thread;
        this.senderPid = senderPid;
        this.senderAlias = senderAlias;
        this.senderKey = senderKey;
        this.sesKey = sesKey;
        this.mimeType = mimeType;
        this.status = status;
        this.kind = kind;
        this.noteType = noteType;
        this.date = date;
        this.publishing = false;
        this.leaching = false;
        this.blocked = false;
        this.expire = System.currentTimeMillis();

    }

    public static Note createNote(
            long thread,
            @NonNull PID senderPid,
            @NonNull String senderAlias,
            @NonNull String senderKey,
            @NonNull String sesKey,
            @NonNull Status status,
            @NonNull Kind kind,
            @NonNull NoteType noteType,
            @NonNull String mimeType,
            long date) {
        checkNotNull(senderPid);
        checkNotNull(senderAlias);
        checkNotNull(senderKey);
        checkNotNull(sesKey);
        checkNotNull(status);
        checkNotNull(kind);
        checkNotNull(noteType);
        checkNotNull(mimeType);

        return new Note(thread,
                senderPid,
                senderAlias,
                senderKey,
                sesKey,
                mimeType,
                status,
                kind,
                noteType,
                date);
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public boolean isLeaching() {
        return leaching;
    }

    public void setLeaching(boolean leaching) {
        this.leaching = leaching;
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

    public boolean isPublishing() {
        return publishing;
    }

    public void setPublishing(boolean publishing) {
        this.publishing = publishing;
    }

    @Nullable
    public CID getImage() {
        return image;
    }

    public void setImage(@Nullable CID image) {
        this.image = image;
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
    public String getSesKey() {
        return sesKey;
    }

    @NonNull
    public PID getSenderPid() {
        return senderPid;
    }

    @NonNull
    public String getMimeType() {
        return mimeType;
    }

    public long getIdx() {
        return idx;
    }

    void setIdx(long idx) {
        this.idx = idx;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Note that = (Note) o;
        return Objects.equals(idx, that.idx);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idx);
    }

    @NonNull
    public String getSenderAlias() {
        return senderAlias;
    }

    public void setSenderAlias(@NonNull String senderAlias) {
        checkNotNull(senderAlias);
        this.senderAlias = senderAlias;
    }

    @NonNull
    public Kind getKind() {
        return kind;
    }

    @NonNull
    public NoteType getNoteType() {
        return noteType;
    }

    public long getDate() {
        return date;
    }


    public boolean areItemsTheSame(@NonNull Note note) {
        checkNotNull(note);
        return idx == note.getIdx();
    }

    public long getThread() {
        return thread;
    }


    @NonNull
    public Status getStatus() {
        return status;
    }

    public void setStatus(@NonNull Status status) {
        this.status = status;
    }

    public boolean sameContent(@NonNull Note o) {
        checkNotNull(o);
        if (this == o) return true;
        return number == o.getNumber() &&
                noteType == o.getNoteType() &&
                status == o.getStatus() &&
                publishing == o.isPublishing() &&
                leaching == o.isLeaching() &&
                blocked == o.isBlocked() &&
                date == o.getDate() &&
                Objects.equals(cid, o.getCid()) &&
                Objects.equals(senderAlias, o.getSenderAlias()) &&
                Objects.equals(image, o.getImage());
    }

    public boolean sameNote(@NonNull Note o) {
        checkNotNull(o);
        if (this == o) return true;
        return noteType == o.getNoteType() &&
                date == o.getDate() &&
                Objects.equals(cid, o.getCid()) &&
                Objects.equals(thread, o.getThread()) &&
                Objects.equals(senderPid, o.getSenderPid()) &&
                Objects.equals(image, o.getImage());
    }

    @Override
    @NonNull
    public String toString() {
        return "Note{" +
                "senderAlias='" + senderAlias + '\'' +
                ", senderPid='" + senderPid + '\'' +
                ", senderKey='" + senderKey + '\'' +
                ", sesKey='" + sesKey + '\'' +
                ", kind=" + kind +
                ", thread='" + thread + '\'' +
                ", noteType=" + noteType +
                ", mimeType='" + mimeType + '\'' +
                ", date=" + date +
                ", idx=" + idx +
                ", cid='" + cid + '\'' +
                ", status=" + status + '}';
    }

    public boolean isEncrypted() {
        return !sesKey.isEmpty();
    }

    @Nullable
    public CID getCid() {
        return cid;
    }

    public void setCid(@Nullable CID cid) {
        this.cid = cid;
    }


    public boolean isExpired() {
        return getExpire() < System.currentTimeMillis();
    }
}
