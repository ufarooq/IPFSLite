package threads.server;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface MessageDao {

    @Query("DELETE FROM Message")
    void clear();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMessages(Message... messages);

    @Query("SELECT * FROM Message")
    LiveData<List<Message>> getLiveDataMessages();

}
