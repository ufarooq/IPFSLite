package threads.core.api;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MessageDao {

    @Query("DELETE FROM Message")
    void clear();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMessages(Message... messages);

    @Query("SELECT * FROM Message")
    Message[] getMessages();

    @Query("SELECT * FROM Message")
    LiveData<List<Message>> getLiveDataMessages();

    @Delete
    void deleteMessage(Message message);
}
