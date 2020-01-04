package threads.core.api;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.TypeConverters;
import androidx.room.Update;

import java.util.List;

import threads.ipfs.api.CID;
import threads.ipfs.api.PID;

@Dao
public interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertNote(Note note);


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertNotes(Note... note);

    @Query("SELECT * FROM Note WHERE kind LIKE :kind AND status LIKE :status")
    @TypeConverters({Kind.class, Status.class})
    List<Note> getNotesByKindAndStatus(Kind kind, Status status);

    @Query("SELECT hash FROM Note WHERE idx = :idx ")
    String getHash(long idx);


    @Query("UPDATE Note SET status = :status WHERE idx = :idx")
    @TypeConverters({Status.class})
    void setStatus(long idx, Status status);

    @Query("UPDATE Note SET status = :status WHERE idx IN(:idxs)")
    @TypeConverters({Status.class})
    void setNotesStatus(Status status, long... idxs);

    @Query("UPDATE Note SET status = :newStatus WHERE status = :oldStatus")
    @TypeConverters({Status.class})
    void setStatus(Status oldStatus, Status newStatus);

    @Query("UPDATE Note SET cid = :cid WHERE idx = :idx")
    @TypeConverters({Converter.class})
    void setCid(long idx, CID cid);

    @Query("UPDATE Note SET publishing = 0")
    void resetNotesPublishing();

    @Query("UPDATE Note SET leaching = 0")
    void resetNotesLeaching();


    @Query("UPDATE Note SET publishing = :publish  WHERE idx = :idx")
    void setPublishing(long idx, boolean publish);

    @Query("UPDATE Note SET leaching = :leaching  WHERE idx = :idx")
    void setLeaching(long idx, boolean leaching);

    @Query("UPDATE Note SET publishing = :publish  WHERE idx IN (:idxs)")
    void setNotesPublishing(boolean publish, long... idxs);

    @Query("UPDATE Note SET leaching = :leaching  WHERE idx IN (:idxs)")
    void setNotesLeaching(boolean leaching, long... idxs);

    @Query("UPDATE Note SET senderAlias = :alias  WHERE idx = :idx")
    void setSenderAlias(long idx, String alias);

    @Query("UPDATE Note SET senderAlias = :alias  WHERE senderPid = :pid")
    @TypeConverters(Converter.class)
    void setSenderAlias(PID pid, String alias);

    @Query("UPDATE Note SET hash = :hash WHERE idx = :idx")
    void setHash(long idx, String hash);

    @Query("SELECT * FROM Note")
    List<Note> getNotes();

    @Query("UPDATE Note SET mimeType =:mimeType  WHERE idx = :idx")
    void setMimeType(long idx, String mimeType);

    @Query("SELECT * FROM Note WHERE cid = :cid")
    @TypeConverters({Converter.class})
    List<Note> getNotesByCID(CID cid);

    @Query("SELECT * FROM Note WHERE hash = :hash")
    Note getNoteByHash(String hash);

    @Query("SELECT * FROM Note WHERE senderPid = :pid AND status = :status")
    @TypeConverters({Converter.class, Status.class})
    List<Note> getNotesBySenderPIDAndStatus(PID pid, Status status);


    @Query("SELECT * FROM Note WHERE idx =:idx")
    Note getNoteByIdx(long idx);

    @Query("SELECT * FROM Note WHERE thread =:thread")
    List<Note> getNotesByThread(long thread);

    @Query("SELECT * FROM Note WHERE date =:date")
    List<Note> getNotesByDate(long date);

    @Query("SELECT * FROM Note WHERE thread =:thread")
    LiveData<List<Note>> getLiveDataNotesByThread(long thread);

    @Query("SELECT * FROM Note")
    LiveData<List<Note>> getLiveDataNotes();

    @Update(onConflict = OnConflictStrategy.REPLACE)
    void updateNote(Note notification);

    @Delete
    void removeNote(Note notification);

    @Query("SELECT mimeType FROM Note WHERE idx = :idx")
    String getMimeType(long idx);

    @Query("SELECT * FROM Note WHERE expire < :date")
    List<Note> getExpiredNotes(long date);

    @Query("DELETE FROM Note")
    void clear();

    @Query("SELECT * FROM Note WHERE noteType =:type")
    @TypeConverters({NoteType.class})
    List<Note> getNotesByType(NoteType type);


    @Query("SELECT COUNT(idx) FROM Note WHERE cid =:cid OR image =:cid")
    @TypeConverters({Converter.class})
    int references(CID cid);

    @Query("UPDATE Note SET image = :image WHERE idx = :idx")
    @TypeConverters({Converter.class})
    void setImage(long idx, CID image);


    @Query("UPDATE Note SET blocked = :blocked WHERE senderPid = :pid")
    @TypeConverters({Converter.class})
    void setSenderBlocked(PID pid, boolean blocked);

    @Query("SELECT status FROM Note WHERE idx = :idx")
    @TypeConverters({Status.class})
    Status getStatus(long idx);

    @Query("SELECT noteType FROM Note WHERE idx = :idx")
    @TypeConverters({NoteType.class})
    NoteType getNoteType(long idx);

    @Query("UPDATE Note SET number = number + 1  WHERE idx IN(:idxs)")
    void incrementNumber(long... idxs);

    @Query("UPDATE Note SET number = 0")
    void resetNotesNumber();
}
