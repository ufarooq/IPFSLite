package threads.server;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

import com.iota.iri.model.Hash;
import com.iota.iri.utils.Converter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import threads.iri.ITransactionStorage;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by paul on 3/2/17 for iri.
 */
@Entity(tableName = "TransactionStorage")
public class TransactionStorage implements ITransactionStorage {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "hashID")
    private final String hashID;
    @ColumnInfo(name = "height")
    private long height = 0;
    @ColumnInfo(name = "sender")
    private String sender = "";
    @ColumnInfo(name = "snapshot")
    private int snapshot;
    @ColumnInfo(name = "validity")
    private int validity = 0;
    @ColumnInfo(name = "arrivalTime")
    private long arrivalTime = 0;
    @ColumnInfo(name = "solid")
    private boolean solid = false;
    @ColumnInfo(name = "value")
    private long value;
    @ColumnInfo(name = "timestamp")
    private long timestamp;
    @ColumnInfo(name = "attachmentTimestamp")
    private long attachmentTimestamp;
    @ColumnInfo(name = "attachmentTimestampLowerBound")
    private long attachmentTimestampLowerBound;
    @ColumnInfo(name = "attachmentTimestampUpperBound")
    private long attachmentTimestampUpperBound;
    @ColumnInfo(name = "currentIndex")
    private long currentIndex;
    @ColumnInfo(name = "lastIndex")
    private long lastIndex;
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    private byte[] bytes;
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    private byte[] trits;
    @NonNull
    @ColumnInfo(name = "address")
    private String address;
    @NonNull
    @ColumnInfo(name = "bundle")
    private String bundle;
    @NonNull
    @ColumnInfo(name = "trunk")
    private String trunk;
    @NonNull
    @ColumnInfo(name = "branch")
    private String branch;
    @NonNull
    @ColumnInfo(name = "obsoleteTag")
    private String obsoleteTag;
    @NonNull
    @ColumnInfo(name = "tag")
    private String tag;
    public TransactionStorage(@NonNull String hashID) {
        checkNotNull(hashID);
        this.hashID = hashID;
    }

    public static ITransactionStorage createTransactionStorage(@NonNull String hash, @NonNull byte[] bytes) {
        checkNotNull(hash);
        checkNotNull(bytes);
        TransactionStorage instance = new TransactionStorage(hash);
        instance.bytes = new byte[SIZE];
        System.arraycopy(bytes, 0, instance.bytes, 0, SIZE);

        instance.trits = new byte[ITransactionStorage.TRINARY_SIZE];
        Converter.getTrits(instance.bytes, instance.trits);

        instance.tag = Converter.trytes(instance.trits, TAG_TRINARY_OFFSET, TAG_TRINARY_SIZE);
        instance.obsoleteTag = Converter.trytes(instance.trits, OBSOLETE_TAG_TRINARY_OFFSET, OBSOLETE_TAG_TRINARY_SIZE);

        instance.address = Hash.convertToString(new Hash(instance.trits, ADDRESS_TRINARY_OFFSET));
        instance.bundle = Hash.convertToString(new Hash(instance.trits, BUNDLE_TRINARY_OFFSET));
        instance.trunk = Hash.convertToString(new Hash(instance.trits, TRUNK_TRANSACTION_TRINARY_OFFSET));
        instance.branch = Hash.convertToString(new Hash(instance.trits, BRANCH_TRANSACTION_TRINARY_OFFSET));

        instance.currentIndex = Converter.longValue(instance.trits, CURRENT_INDEX_TRINARY_OFFSET, CURRENT_INDEX_TRINARY_SIZE);
        instance.lastIndex = Converter.longValue(instance.trits, LAST_INDEX_TRINARY_OFFSET, LAST_INDEX_TRINARY_SIZE);
        instance.value = Converter.longValue(instance.trits, VALUE_TRINARY_OFFSET, VALUE_USABLE_TRINARY_SIZE);
        instance.timestamp = Converter.longValue(instance.trits, TIMESTAMP_TRINARY_OFFSET, TIMESTAMP_TRINARY_SIZE);

        instance.attachmentTimestamp = Converter.longValue(instance.trits, ATTACHMENT_TIMESTAMP_TRINARY_OFFSET, ATTACHMENT_TIMESTAMP_TRINARY_SIZE);
        instance.attachmentTimestampLowerBound = Converter.longValue(instance.trits, ATTACHMENT_TIMESTAMP_LOWER_BOUND_TRINARY_OFFSET, ATTACHMENT_TIMESTAMP_LOWER_BOUND_TRINARY_SIZE);
        instance.attachmentTimestampUpperBound = Converter.longValue(instance.trits, ATTACHMENT_TIMESTAMP_UPPER_BOUND_TRINARY_OFFSET, ATTACHMENT_TIMESTAMP_UPPER_BOUND_TRINARY_SIZE);
        return instance;
    }

    private static byte[] trits(byte[] transactionBytes) {
        byte[] trits = new byte[ITransactionStorage.TRINARY_SIZE];
        if (transactionBytes != null) {
            Converter.getTrits(transactionBytes, trits);
        }
        return trits;
    }

    public byte[] getTrits() {
        return trits;
    }

    public void setTrits(byte[] trits) {
        this.trits = trits;
    }

    public Hash getHash() {
        return Hash.convertToHash(getHashID());
    }

    public long getHeight() {
        return height;
    }

    public void setHeight(long height) {
        this.height = height;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public int getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(int snapshot) {
        this.snapshot = snapshot;
    }

    public long getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(long arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public boolean isSolid() {
        return solid;
    }

    public void setSolid(boolean solid) {
        this.solid = solid;
    }

    public long getValue() {
        return value;
    }

    public void setValue(long value) {
        this.value = value;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getAttachmentTimestamp() {
        return attachmentTimestamp;
    }

    public void setAttachmentTimestamp(long attachmentTimestamp) {
        this.attachmentTimestamp = attachmentTimestamp;
    }

    public long getAttachmentTimestampLowerBound() {
        return attachmentTimestampLowerBound;
    }

    public void setAttachmentTimestampLowerBound(long attachmentTimestampLowerBound) {
        this.attachmentTimestampLowerBound = attachmentTimestampLowerBound;
    }

    public long getAttachmentTimestampUpperBound() {
        return attachmentTimestampUpperBound;
    }

    public void setAttachmentTimestampUpperBound(long attachmentTimestampUpperBound) {
        this.attachmentTimestampUpperBound = attachmentTimestampUpperBound;
    }

    public long getCurrentIndex() {
        return currentIndex;
    }

    public void setCurrentIndex(long currentIndex) {
        this.currentIndex = currentIndex;
    }

    public long getLastIndex() {
        return lastIndex;
    }

    public void setLastIndex(long lastIndex) {
        this.lastIndex = lastIndex;
    }

    @NonNull
    public String getHashID() {
        return hashID;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    @NonNull
    public String getAddress() {
        return address;
    }

    public void setAddress(@NonNull String address) {
        this.address = address;
    }

    @NonNull
    public String getBundle() {
        return bundle;
    }

    public void setBundle(@NonNull String bundle) {
        this.bundle = bundle;
    }

    @NonNull
    public String getTrunk() {
        return trunk;
    }

    public void setTrunk(@NonNull String trunk) {
        this.trunk = trunk;
    }

    @NonNull
    public String getBranch() {
        return branch;
    }

    public void setBranch(@NonNull String branch) {
        this.branch = branch;
    }

    @NonNull
    public String getObsoleteTag() {
        return obsoleteTag;
    }

    public void setObsoleteTag(@NonNull String obsoleteTag) {
        this.obsoleteTag = obsoleteTag;
    }

    @NonNull
    public String getTag() {
        return tag;
    }

    public void setTag(@NonNull String tag) {
        this.tag = tag;
    }

    public int getValidity() {
        return validity;
    }

    public void setValidity(int validity) {
        this.validity = validity;
    }

    public int getWeightMagnitude() {
        return getHash().trailingZeros();
    }

    public Hash getBundleHash() {
        return Hash.convertToHash(getBundle());
    }

    public Hash getTrunkTransactionHash() {
        return Hash.convertToHash(getTrunk());
    }

    public Hash getAddressHash() {
        return Hash.convertToHash(getAddress());
    }

    public byte[] getSignature() {
        return Arrays.copyOfRange(trits,
                ITransactionStorage.SIGNATURE_MESSAGE_FRAGMENT_TRINARY_OFFSET, ITransactionStorage.SIGNATURE_MESSAGE_FRAGMENT_TRINARY_SIZE);
    }

    public byte[] getNonce() {
        byte[] nonce = Converter.allocateBytesForTrits(ITransactionStorage.NONCE_TRINARY_SIZE);
        Converter.bytes(trits, ITransactionStorage.NONCE_TRINARY_OFFSET, nonce, 0, trits.length);
        return nonce;
    }

    // TODO this might be not correct
    public List<Hash> getApprovers() {
        List<Hash> approovers = new ArrayList<>();
        approovers.add(getBranchTransactionHash());
        approovers.add(getTrunkTransactionHash());
        return approovers;
    }

    public Hash getBranchTransactionHash() {
        return Hash.convertToHash(getBranch());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TransactionStorage other = (TransactionStorage) o;
        return Objects.equals(getHashID(), other.getHashID());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getHashID());
    }
}
