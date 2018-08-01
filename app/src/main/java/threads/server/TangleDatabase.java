package threads.server;

import android.arch.persistence.room.RoomDatabase;
import android.support.annotation.NonNull;

import com.iota.iri.model.Hash;

import java.util.Iterator;
import java.util.Set;
import java.util.Stack;

import threads.iri.IDataStorage;
import threads.iri.ITangle;
import threads.iri.ITransactionStorage;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by remmer on 06.03.18.
 */

@android.arch.persistence.room.Database(entities = {DataStorage.class, TransactionStorage.class}, version = 1, exportSchema = false)
public abstract class TangleDatabase extends RoomDatabase implements ITangle {

    public abstract DataStorageDao storageDao();


    public abstract TransactionStorageDao transactionStorageDao();

    @Override
    public void insertDataStorage(@NonNull IDataStorage storage) {
        storageDao().insertDataStorages((DataStorage) storage);
    }

    @Override
    public IDataStorage getDataStorage(@NonNull String address, int chunkIndex) {
        checkNotNull(address);
        checkArgument(chunkIndex >= 0);
        return storageDao().getDataStorage(address, chunkIndex);
    }

    @Override
    public IDataStorage[] getDataStoragesByAddress(@NonNull String address) {
        checkNotNull(address);
        return storageDao().getDataStoragesByAddress(address);
    }

    @Override
    public void clearDatabase() {
        transactionStorageDao().clear();
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
    public IDataStorage createDataStorage(@NonNull String address, int chunkIndex, byte[] bytes) {
        checkNotNull(address);
        checkArgument(chunkIndex >= 0);
        checkNotNull(bytes);
        return DataStorage.createDataStorage(address, chunkIndex, bytes);
    }

    @Override
    public ITransactionStorage fromHash(@NonNull Hash hash) {
        checkNotNull(hash);
        String hashID = Hash.convertToString(hash);
        return getTransactionStorage(hashID);
    }

    @Override
    public ITransactionStorage fromTrits(@NonNull byte[] trits) {
        checkNotNull(trits);
        return TransactionStorage.createTransactionStorageFromTrits(trits);
    }

    @Override
    public ITransactionStorage fromBytes(@NonNull byte[] bytes) {
        checkNotNull(bytes);
        return TransactionStorage.createTransactionStorageFromBytes(bytes);
    }

    @Override
    public void updateTransactionStorage(ITransactionStorage transaction) {
        transactionStorageDao().update((TransactionStorage) transaction);
    }

    @Override
    public ITransactionStorage getBranchTransactionStorage(ITransactionStorage transaction) {
        return fromHash(transaction.getBranchTransactionHash());

    }

    @Override
    public ITransactionStorage getTrunkTransactionStorage(ITransactionStorage transaction) {
        return fromHash(transaction.getTrunkTransactionHash());
    }

    @Override
    public void updateHeights(@NonNull ITransactionStorage transaction) {
        ITransactionStorage trunk = this.getTrunkTransactionStorage(transaction);
        Stack<Hash> transactionViewModels = new Stack<>();
        transactionViewModels.push(transaction.getHash());
        while (trunk.getHeight() == 0 && !trunk.getHash().equals(Hash.NULL_HASH)) {
            transaction = trunk;
            trunk = this.getTrunkTransactionStorage(transaction);
            transactionViewModels.push(transaction.getHash());
        }
        while (transactionViewModels.size() != 0) {
            transaction = this.fromHash(transactionViewModels.pop());
            long currentHeight = transaction.getHeight();
            if (Hash.NULL_HASH.equals(trunk.getHash()) && trunk.getHeight() == 0
                    && !Hash.NULL_HASH.equals(transaction.getHash())) {
                if (currentHeight != 1L) {
                    transaction.setHeight(1L);
                    updateTransactionStorage(transaction);
                }
            } else if (transaction.getHeight() == 0) {
                long newHeight = 1L + trunk.getHeight();
                if (currentHeight != newHeight) {
                    transaction.setHeight(newHeight);
                    updateTransactionStorage(transaction);
                }
            } else {
                break;
            }
            trunk = transaction;
        }
    }

    @Override
    public void updateSolidTransactions(final Set<Hash> analyzedHashes) {
        Iterator<Hash> hashIterator = analyzedHashes.iterator();
        ITransactionStorage transactionViewModel;
        while (hashIterator.hasNext()) {
            transactionViewModel = fromHash(hashIterator.next());

            updateHeights(transactionViewModel);

            if (!transactionViewModel.isSolid()) {
                transactionViewModel.setSolid(true);
                updateTransactionStorage(transactionViewModel);
            }
        }
    }

    @Override
    public boolean existsTransactionStorage(@NonNull Hash hash) {
        checkNotNull(hash);
        return getTransactionStorage(Hash.convertToString(hash)) != null;
    }

}
