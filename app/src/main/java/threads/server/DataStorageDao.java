package threads.server;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.support.annotation.NonNull;

@Dao
public interface DataStorageDao {
    @Query("DELETE FROM DataStorage")
    void clear();

    @Query("SELECT * FROM DataStorage WHERE address =:address AND chunkIndex =:chunkIndex")
    DataStorage getDataStorage(@NonNull String address, @NonNull int chunkIndex);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertDataStorages(DataStorage... storages);

    @Query("SELECT * FROM DataStorage WHERE address =:address")
    DataStorage[] getDataStoragesByAddress(@NonNull String address);


}
