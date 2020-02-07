package threads.server.core.threads;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.TypeConverters;
import androidx.room.Update;

import java.util.List;

import threads.ipfs.CID;
import threads.ipfs.PID;
import threads.server.core.Converter;

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

    @Query("SELECT * FROM Thread WHERE lastModified =:lastModified")
    List<Thread> getThreadsByLastModified(long lastModified);

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

    @Query("UPDATE Thread SET publishing = :publish  WHERE idx = :idx")
    void setPublishing(long idx, boolean publish);

    @Query("UPDATE Thread SET leaching = :leaching  WHERE idx = :idx")
    void setLeaching(long idx, boolean leaching);

    @Query("UPDATE Thread SET lastModified = :lastModified  WHERE idx = :idx")
    void setThreadLastModified(long idx, long lastModified);

    @Query("UPDATE Thread SET seeding = 0, deleting = 1 WHERE idx IN (:idxs)")
    void setThreadsDeleting(long... idxs);

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

    @Query("SELECT * FROM Thread WHERE content = :content")
    @TypeConverters({Converter.class})
    List<Thread> getThreadsByContent(CID content);

    @Query("SELECT * FROM Thread WHERE content = :cid AND parent = :parent")
    @TypeConverters({Converter.class})
    List<Thread> getThreadsByContentAndParent(CID cid, long parent);

    @Delete
    void removeThreads(Thread... threads);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    void updateThreads(Thread... threads);

    @Query("UPDATE Thread SET publishing = 0")
    void resetThreadsPublishing();

    @Query("UPDATE Thread SET leaching = 0")
    void resetThreadsLeaching();

    @Query("SELECT COUNT(idx) FROM Thread WHERE content =:cid OR thumbnail =:cid")
    @TypeConverters({Converter.class})
    int references(CID cid);

    @Query("SELECT * FROM Thread WHERE parent =:thread")
    List<Thread> getChildren(long thread);

    @Query("SELECT COUNT(idx) FROM Thread WHERE parent =:thread")
    int getThreadReferences(long thread);

    @Query("SELECT * FROM Thread WHERE idx =:idx")
    Thread getThreadByIdx(long idx);

    @Query("SELECT * FROM Thread WHERE idx IN(:idxs)")
    List<Thread> getThreadsByIdx(long... idxs);

    @Query("SELECT * FROM Thread WHERE sender =:senderPid")
    @TypeConverters({Converter.class})
    List<Thread> getThreadsBySenderPid(PID senderPid);

    @Query("SELECT * FROM Thread WHERE parent =:parent AND deleting = 0 AND name LIKE :query")
    LiveData<List<Thread>> getLiveDataVisibleChildrenByQuery(long parent, String query);

    @Query("UPDATE Thread SET content =:cid  WHERE idx = :idx")
    @TypeConverters({Converter.class})
    void setContent(long idx, CID cid);

    @Query("UPDATE Thread SET mimeType =:mimeType  WHERE idx = :idx")
    void setMimeType(long idx, String mimeType);

    @Query("UPDATE Thread SET thumbnail = :image WHERE idx = :idx")
    @TypeConverters({Converter.class})
    void setThumbnail(long idx, CID image);

    @Query("UPDATE Thread SET name = :name WHERE idx = :idx")
    void setName(long idx, String name);

    @Query("SELECT * FROM Thread WHERE parent =:parent AND seeding = 1")
    List<Thread> getSeedingChildren(long parent);

    @Query("SELECT * FROM Thread WHERE seeding = 1 ORDER BY lastModified DESC LIMIT :limit")
    List<Thread> getNewestSeedingThreads(int limit);

    @Query("SELECT * FROM Thread WHERE seeding = 1 AND name LIKE :query")
    List<Thread> getSeedingThreadsByQuery(String query);

    @Query("UPDATE Thread SET progress = :progress WHERE idx = :idx")
    void setProgress(long idx, int progress);

    @Query("UPDATE Thread SET seeding = 1, leaching = 0, progress = 0 WHERE idx = :idx")
    void setSeeding(long idx);

    @Query("UPDATE Thread SET size = :size WHERE idx = :idx")
    void setSize(long idx, long size);
}
