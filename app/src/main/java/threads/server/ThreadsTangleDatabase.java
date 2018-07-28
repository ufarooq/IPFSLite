package threads.server;

import android.arch.persistence.room.RoomDatabase;
import android.support.annotation.NonNull;

import threads.iri.IThreadsTangle;
import threads.iri.ITransactionStorage;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by remmer on 06.03.18.
 */

@android.arch.persistence.room.Database(entities = {DataStorage.class, TransactionStorage.class}, version = 1, exportSchema = false)
public abstract class ThreadsTangleDatabase extends RoomDatabase implements IThreadsTangle {

    public abstract DataStorageDao storageDao();


    public abstract TransactionStorageDao transactionStorageDao();

    @Override
    public void insertDataStorage(@NonNull IDataStorage storage) {
        storageDao().insertDataStorages((DataStorage) storage);
    }

    @Override
    public IDataStorage getDataStorage(@NonNull String address, int chunkIndex) {
        checkNotNull(address);
        checkArgument(chunkIndex > 0);
        return storageDao().getDataStorage(address, chunkIndex);
    }

    @Override
    public IDataStorage[] getDataStoragesByAddress(@NonNull String address) {
        checkNotNull(address);
        return storageDao().getDataStoragesByAddress(address);
    }

    @Override
    public void clearDatabase() {

        storageDao().clear();
    }


    @Override
    public ITransactionStorage getTransactionStorage(@NonNull String hash) {
        return transactionStorageDao().getTransactionStorage(hash);
    }

    @Override
    public int getNumberOfStoredTransactions() {
        return transactionStorageDao().count();
    }

    @Override
    public void insertTransactionStorage(@NonNull ITransactionStorage transactionStorage) {
        transactionStorageDao().insertTransactionStorages((TransactionStorage) transactionStorage);
    }

    @Override
    public void deleteTransactionStorage(@NonNull String hash) {
        transactionStorageDao().deleteTransactionStorage(hash);
    }

    @Override
    public void shutdown() {
        //  Nothing to do yet
    }

    @Override
    public void clear() {
        transactionStorageDao().clear();
    }

    @Override
    public ITransactionStorage[] getBundles(@NonNull String bundle) {
        return transactionStorageDao().getBundles(bundle);
    }

    @Override
    public ITransactionStorage[] getAddresses(@NonNull String address) {
        return transactionStorageDao().getAddresses(address);
    }

    @Override
    public ITransactionStorage[] getTags(@NonNull String tag) {
        return transactionStorageDao().getTags(tag);
    }

    @Override
    public ITransactionStorage[] getObsoleteTags(@NonNull String tag) {
        return transactionStorageDao().getObsoleteTags(tag);
    }

    @Override
    public ITransactionStorage createTransactionStorage(@NonNull String hash, @NonNull byte[] bytes) {
        return TransactionStorage.createTransactionStorage(hash, bytes);
    }
}
