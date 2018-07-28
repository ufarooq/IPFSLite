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

    @Query("SELECT * FROM TransactionStorage WHERE hash =:hash")
    TransactionStorage getTransactionStorage(@NonNull String hash);

    @Query("SELECT * FROM TransactionStorage WHERE address =:address")
    TransactionStorage[] getAddresses(@NonNull String address);

    @Query("DELETE FROM TransactionStorage")
    void clear();

    @Query("SELECT COUNT(hash) FROM TransactionStorage")
    int count();

    @Update(onConflict = OnConflictStrategy.REPLACE)
    void update(@NonNull TransactionStorage transactionStorage);

    @Query("DELETE FROM TransactionStorage WHERE hash =:hash")
    void deleteTransactionStorage(@NonNull String hash);


    @Query("SELECT * FROM TransactionStorage WHERE bundle =:bundle")
    TransactionStorage[] getBundles(@NonNull String bundle);

    @Query("SELECT * FROM TransactionStorage WHERE tag =:tag")
    TransactionStorage[] getTags(@NonNull String tag);

    @Query("SELECT * FROM TransactionStorage WHERE obsoleteTag =:obsoleteTag")
    TransactionStorage[] getObsoleteTags(@NonNull String obsoleteTag);

}
