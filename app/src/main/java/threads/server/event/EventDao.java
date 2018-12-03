package threads.server.event;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

@Dao
public interface EventDao {

    @Query("SELECT * FROM Event WHERE  identifier  =:identifier")
    LiveData<Event> getEvent(String identifier);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertEvent(Event event);

    @Delete
    void deleteEvent(Event event);

    @Query("DELETE FROM Event")
    void clear();
}
