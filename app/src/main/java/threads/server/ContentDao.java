package threads.server;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ContentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertContent(Content... contents);

    @Query("DELETE FROM Content")
    void clear();

    @Query("DELETE FROM Content WHERE cid = :cid")
    void removeContent(String cid);

    @Delete
    void removeContent(Content content);

    @Update
    void updateContent(Content content);

    @Query("SELECT * FROM Content WHERE timestamp <= :timestamp")
    List<Content> getContentWithSmallerTimestamp(long timestamp);


    @Query("UPDATE Content SET finished = :finished  WHERE cid = :cid")
    void setFinished(String cid, boolean finished);
}
