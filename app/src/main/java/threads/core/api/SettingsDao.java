package threads.core.api;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface SettingsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertSettings(Settings... settings);

    @Query("SELECT * FROM Settings WHERE id =:id")
    Settings getSettings(String id);

    @Query("SELECT * FROM Settings WHERE id =:id")
    LiveData<Settings> getLiveDataSettings(String id);

    @Query("DELETE FROM Settings")
    void clear();

    @Update(onConflict = OnConflictStrategy.REPLACE)
    void updateSettings(Settings settings);
}
