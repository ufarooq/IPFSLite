package threads.server;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;
import android.support.annotation.NonNull;

@Dao
public interface TransactionStorageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertTransactionStorages(@NonNull TransactionStorage... transactionStorages);

    @Query("SELECT * FROM TransactionStorage WHERE hashID =:hashID")
    TransactionStorage getTransactionStorage(@NonNull String hashID);

    @Query("SELECT * FROM TransactionStorage WHERE address =:hashID")
    TransactionStorage[] getAddresses(@NonNull String hashID);

    @Query("DELETE FROM TransactionStorage")
    void clear();

    @Query("SELECT COUNT(hashID) FROM TransactionStorage")
    int count();

    @Update(onConflict = OnConflictStrategy.REPLACE)
    void update(@NonNull TransactionStorage transactionStorage);

    @Query("DELETE FROM TransactionStorage WHERE hashID =:hashID")
    void deleteTransactionStorage(@NonNull String hashID);


    @Query("SELECT * FROM TransactionStorage WHERE bundle =:bundle")
    TransactionStorage[] getBundles(@NonNull String bundle);

    @Query("SELECT * FROM TransactionStorage WHERE tag =:tag")
    TransactionStorage[] getTags(@NonNull String tag);

    @Query("SELECT * FROM TransactionStorage WHERE obsoleteTag =:obsoleteTag")
    TransactionStorage[] getObsoleteTags(@NonNull String obsoleteTag);

}
