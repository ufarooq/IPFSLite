package threads.server;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.TypeConverters;

import java.util.List;

import threads.core.Converter;
import threads.ipfs.api.PID;

@Dao
public interface ContentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertContent(Content... contents);

    @Delete
    void removeContent(Content content);

    @Query("SELECT * FROM Content WHERE timestamp <= :timestamp")
    List<Content> getContentWithSmallerTimestamp(long timestamp);


    @Query("SELECT * FROM Content WHERE pid = :pid AND finished = :finished AND timestamp <= :timestamp")
    @TypeConverters(Converter.class)
    List<Content> getContents(PID pid, long timestamp, boolean finished);

    @Query("UPDATE Content SET finished = :finished  WHERE cid = :cid")
    void setFinished(String cid, boolean finished);

    @Query("SELECT * FROM Content WHERE cid = :cid")
    Content getContent(String cid);
}
