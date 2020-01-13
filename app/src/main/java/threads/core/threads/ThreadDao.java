package threads.core.threads;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.TypeConverters;
import androidx.room.Update;

import java.util.List;

import threads.core.Converter;
import threads.ipfs.api.CID;
import threads.ipfs.api.PID;

@Dao
public interface ThreadDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertThread(Thread thread);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertThreads(Thread... threads);

    @Query("SELECT * FROM Thread")
    List<Thread> getThreads();

    @Query("SELECT * FROM Thread")
    LiveData<List<Thread>> getLiveDataThreads();

    @Query("DELETE FROM Thread")
    void clear();

    @Query("SELECT * FROM Thread WHERE lastModified =:date")
    List<Thread> getThreadsByDate(long date);

    @Query("SELECT * FROM Thread WHERE expire < :date")
    List<Thread> getExpiredThreads(long date);

    @Query("SELECT * FROM Thread WHERE kind LIKE :kind AND status = :status")
    @TypeConverters({Kind.class, Status.class})
    List<Thread> getThreadsByKindAndThreadStatus(Kind kind, Status status);

    @Query("SELECT * FROM Thread WHERE pinned = :pinned")
    List<Thread> getThreadsByPinned(boolean pinned);

    @Query("SELECT mimeType FROM Thread WHERE idx = :idx")
    String getMimeType(long idx);

    @Query("SELECT content FROM Thread WHERE idx = :idx")
    @TypeConverters({Converter.class})
    CID getContent(long idx);

    @Query("SELECT content FROM Thread WHERE idx = :idx")
    @TypeConverters({Converter.class})
    CID getThumbnail(long idx);

    @Query("UPDATE Thread SET status = :status  WHERE idx = :idx")
    @TypeConverters({Status.class})
    void setStatus(long idx, Status status);

    @Query("UPDATE Thread SET publishing = :publish  WHERE idx = :idx")
    void setPublishing(long idx, boolean publish);

    @Query("UPDATE Thread SET leaching = :leaching  WHERE idx = :idx")
    void setLeaching(long idx, boolean leaching);

    @Query("UPDATE Thread SET lastModified = :date  WHERE idx = :idx")
    void setThreadDate(long idx, long date);

    @Query("UPDATE Thread SET status = :status  WHERE idx IN (:idxs)")
    @TypeConverters({Status.class})
    void setThreadsStatus(Status status, long... idxs);

    @Query("UPDATE Thread SET publishing = :publish  WHERE idx IN (:idxs)")
    void setThreadsPublishing(boolean publish, long... idxs);

    @Query("UPDATE Thread SET leaching = :leaching  WHERE idx IN (:idxs)")
    void setThreadsLeaching(boolean leaching, long... idxs);

    @Query("UPDATE Thread SET senderAlias = :alias  WHERE idx = :idx")
    void setSenderAlias(long idx, String alias);

    @Query("UPDATE Thread SET pinned = :pinned  WHERE idx = :idx")
    void setPinned(long idx, boolean pinned);

    @Query("SELECT pinned FROM Thread WHERE idx = :idx")
    boolean isPinned(long idx);

    @Query("UPDATE Thread SET senderAlias = :alias  WHERE sender = :pid")
    @TypeConverters(Converter.class)
    void setSenderAlias(PID pid, String alias);

    @Query("UPDATE Thread SET status = :newStatus  WHERE status = :oldStatus")
    @TypeConverters({Status.class})
    void setStatus(Status oldStatus, Status newStatus);

    @Query("SELECT * FROM Thread WHERE status = :status")
    @TypeConverters({Status.class})
    List<Thread> getThreadsByStatus(Status status);

    @Query("SELECT * FROM Thread WHERE content = :cid")
    @TypeConverters({Converter.class})
    List<Thread> getThreadsByCid(CID cid);

    @Query("SELECT * FROM Thread WHERE content = :cid AND thread = :thread")
    @TypeConverters({Converter.class})
    List<Thread> getThreadsByCidAndThread(CID cid, long thread);

    @Delete
    void removeThreads(Thread... threads);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    void updateThreads(Thread... threads);

    @Query("UPDATE Thread SET number = 0 WHERE idx IN (:idxs)")
    void resetNumber(long... idxs);

    @Query("UPDATE Thread SET number = 0")
    void resetThreadsNumber();

    @Query("UPDATE Thread SET publishing = 0")
    void resetThreadsPublishing();

    @Query("UPDATE Thread SET leaching = 0")
    void resetThreadsLeaching();

    @Query("UPDATE Thread SET number = 0 WHERE thread IN (:threads)")
    void resetThreadNumber(long... threads);

    @Query("Select SUM(number) FROM THREAD")
    int getThreadsNumber();

    @Query("SELECT COUNT(idx) FROM Thread WHERE content =:cid OR thumbnail =:cid")
    @TypeConverters({Converter.class})
    int references(CID cid);

    @Query("SELECT * FROM Thread WHERE thread =:thread")
    List<Thread> getThreadsByThread(long thread);

    @Query("SELECT COUNT(idx) FROM Thread WHERE thread =:thread")
    int getThreadReferences(long thread);

    @Query("SELECT * FROM Thread WHERE idx =:idx")
    Thread getThreadByIdx(long idx);

    @Query("SELECT * FROM Thread WHERE idx IN(:idxs)")
    List<Thread> getThreadByIdxs(long... idxs);

    @Query("SELECT * FROM Thread WHERE sender =:senderPid")
    @TypeConverters({Converter.class})
    List<Thread> getThreadsBySenderPid(PID senderPid);

    @Query("SELECT * FROM Thread WHERE thread =:thread")
    LiveData<List<Thread>> getLiveDataThreadsByThread(long thread);

    @Query("UPDATE Thread SET number = number + 1  WHERE idx IN(:idxs)")
    void incrementNumber(long... idxs);

    @Query("UPDATE Thread SET content =:cid  WHERE idx = :idx")
    @TypeConverters({Converter.class})
    void setContent(long idx, CID cid);

    @Query("UPDATE Thread SET mimeType =:mimeType  WHERE idx = :idx")
    void setMimeType(long idx, String mimeType);


    @Query("UPDATE Thread SET thumbnail = :image WHERE idx = :idx")
    @TypeConverters({Converter.class})
    void setThumbnail(long idx, CID image);

    @Query("SELECT status FROM Thread WHERE idx = :idx")
    @TypeConverters({Status.class})
    Status getStatus(long idx);

    @Query("SELECT marked FROM Thread WHERE idx = :idx")
    boolean getMarkedFlag(long idx);

    @Query("UPDATE Thread SET marked = :marked WHERE idx = :idx")
    void setMarkedFlag(long idx, boolean marked);

    @Query("UPDATE Thread SET name = :name WHERE idx = :idx")
    void setName(long idx, String name);
}
