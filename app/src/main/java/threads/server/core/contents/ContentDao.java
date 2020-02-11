package threads.server.core.contents;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;


@Dao
public interface ContentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertContent(Content... contents);

    @Delete
    void removeContent(Content content);

    @Query("SELECT * FROM Content WHERE timestamp <= :timestamp")
    List<Content> getContentWithSmallerTimestamp(long timestamp);

    @Query("SELECT * FROM Content WHERE pid = :pid AND finished = 0")
    List<Content> getContents(String pid);

    @Query("UPDATE Content SET finished = :finished  WHERE cid = :cid")
    void setFinished(String cid, boolean finished);

    @Query("SELECT * FROM Content WHERE cid = :cid")
    Content getContent(String cid);

    @Query("UPDATE Content SET timestamp = :timestamp  WHERE cid = :cid")
    void setTimestamp(String cid, long timestamp);
}
