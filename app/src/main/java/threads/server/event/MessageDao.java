package threads.server.event;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface MessageDao {

    @Query("DELETE FROM Message")
    void clear();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMessages(Message... messages);

    @Query("SELECT * FROM Message")
    LiveData<List<Message>> getLiveDataMessages();

}
