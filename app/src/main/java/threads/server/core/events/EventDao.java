package threads.server.core.events;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface EventDao {

    @Query("SELECT * FROM Event WHERE  identifier  =:identifier")
    LiveData<Event> getEvent(String identifier);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertEvent(Event event);

    @Delete
    void deleteEvent(Event event);

    @Query("DELETE FROM Event WHERE  identifier =:identifier")
    void deleteEvent(String identifier);

    @Query("SELECT content FROM Event WHERE identifier =:identifier")
    String getContent(String identifier);

    @Query("UPDATE Event SET content =:content  WHERE identifier =:identifier")
    void setContent(String identifier, String content);

    @Query("DELETE FROM Event")
    void clear();
}
