package threads.server.daemon;

import android.arch.persistence.room.RoomDatabase;
import android.support.annotation.NonNull;

import java.util.Iterator;
import java.util.Set;
import java.util.Stack;

import threads.iota.model.Hash;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by remmer on 06.03.18.
 */

@android.arch.persistence.room.Database(entities = {TransactionStorage.class}, version = 3, exportSchema = false)
public abstract class TransactionDatabase extends RoomDatabase {
    private static final String TAG = TransactionDatabase.class.getSimpleName();


    public abstract TransactionStorageDao transactionStorageDao();


    public ITransactionStorage getTransactionStorage(@NonNull String hash) {
        return transactionStorageDao().getTransactionStorage(hash);
    }


    public int getNumberOfStoredTransactions() {
        return transactionStorageDao().count();
    }


    public void insertTransactionStorage(@NonNull ITransactionStorage transactionStorage) {
        transactionStorageDao().insertTransactionStorages((TransactionStorage) transactionStorage);
    }


    public void deleteTransactionStorage(@NonNull String hash) {
        transactionStorageDao().deleteTransactionStorage(hash);
    }


    public void clear() {
        transactionStorageDao().clear();
    }


    public ITransactionStorage[] getBundles(@NonNull String bundle) {
        return transactionStorageDao().getBundles(bundle);
    }


    public ITransactionStorage[] getAddresses(@NonNull String address) {
        return transactionStorageDao().getTransactionStorageByAddress(address);
    }


    public ITransactionStorage[] getTags(@NonNull String tag) {
        return transactionStorageDao().getTags(tag);
    }


    public ITransactionStorage[] getObsoleteTags(@NonNull String tag) {
        return transactionStorageDao().getObsoleteTags(tag);
    }


    public ITransactionStorage fromHash(@NonNull Hash hash) {
        checkNotNull(hash);
        String hashID = Hash.convertToString(hash);
        return getTransactionStorage(hashID);
    }


    public ITransactionStorage fromTrits(@NonNull byte[] trits) {
        checkNotNull(trits);
        return TransactionStorage.createTransactionStorageFromTrits(trits);
    }


    public ITransactionStorage fromBytes(@NonNull byte[] bytes) {
        checkNotNull(bytes);
        return TransactionStorage.createTransactionStorageFromBytes(bytes);
    }


    public void updateTransactionStorage(ITransactionStorage transaction) {
        transactionStorageDao().update((TransactionStorage) transaction);
    }


    public ITransactionStorage getBranchTransactionStorage(ITransactionStorage transaction) {
        return fromHash(transaction.getBranchTransactionHash());

    }


    public ITransactionStorage getTrunkTransactionStorage(ITransactionStorage transaction) {
        return fromHash(transaction.getTrunkTransactionHash());
    }


    public void updateHeights(@NonNull ITransactionStorage transaction) {
        ITransactionStorage trunk = this.getTrunkTransactionStorage(transaction);
        Stack<Hash> transactionViewModels = new Stack<>();
        transactionViewModels.push(transaction.getTransactionHash());
        while (trunk.getHeight() == 0 && !trunk.getHash().equals(Hash.NULL_HASH)) {
            transaction = trunk;
            trunk = this.getTrunkTransactionStorage(transaction);
            transactionViewModels.push(transaction.getTransactionHash());
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


    public boolean existsTransactionStorage(@NonNull Hash hash) {
        checkNotNull(hash);
        return getTransactionStorage(Hash.convertToString(hash)) != null;
    }


}
