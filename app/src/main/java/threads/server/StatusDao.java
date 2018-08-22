package threads.server;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;
import android.support.annotation.NonNull;

@Dao
public interface StatusDao {

    @Query("DELETE FROM Status")
    void clear();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertStatus(@NonNull Status status);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    void updateStatus(@NonNull Status status);

    @Query("SELECT * FROM Status WHERE uid =:uid")
    Status getStatus(@NonNull String uid);

    @Query("SELECT * FROM Status WHERE uid =:uid")
    LiveData<Status> getLiveDataStatus(@NonNull String uid);
}
