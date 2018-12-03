package threads.server.daemon;

import java.util.List;

import threads.iota.ITransaction;
import threads.iota.model.Hash;

public interface ITransactionStorage extends ITransaction {
    int SIZE = 1604;
    long SUPPLY = 2779530283277761L; // = (3^33 - 1) / 2
    int SIGNATURE_MESSAGE_FRAGMENT_TRINARY_OFFSET = 0, SIGNATURE_MESSAGE_FRAGMENT_TRINARY_SIZE = 6561;
    int ADDRESS_TRINARY_OFFSET = SIGNATURE_MESSAGE_FRAGMENT_TRINARY_OFFSET + SIGNATURE_MESSAGE_FRAGMENT_TRINARY_SIZE, ADDRESS_TRINARY_SIZE = 243;
    int VALUE_TRINARY_OFFSET = ADDRESS_TRINARY_OFFSET + ADDRESS_TRINARY_SIZE, VALUE_TRINARY_SIZE = 81, VALUE_USABLE_TRINARY_SIZE = 33;
    int OBSOLETE_TAG_TRINARY_OFFSET = VALUE_TRINARY_OFFSET + VALUE_TRINARY_SIZE, OBSOLETE_TAG_TRINARY_SIZE = 81;
    int TIMESTAMP_TRINARY_OFFSET = OBSOLETE_TAG_TRINARY_OFFSET + OBSOLETE_TAG_TRINARY_SIZE, TIMESTAMP_TRINARY_SIZE = 27;
    int CURRENT_INDEX_TRINARY_OFFSET = TIMESTAMP_TRINARY_OFFSET + TIMESTAMP_TRINARY_SIZE, CURRENT_INDEX_TRINARY_SIZE = 27;
    int LAST_INDEX_TRINARY_OFFSET = CURRENT_INDEX_TRINARY_OFFSET + CURRENT_INDEX_TRINARY_SIZE, LAST_INDEX_TRINARY_SIZE = 27;
    int BUNDLE_TRINARY_OFFSET = LAST_INDEX_TRINARY_OFFSET + LAST_INDEX_TRINARY_SIZE, BUNDLE_TRINARY_SIZE = 243;
    int TRUNK_TRANSACTION_TRINARY_OFFSET = BUNDLE_TRINARY_OFFSET + BUNDLE_TRINARY_SIZE, TRUNK_TRANSACTION_TRINARY_SIZE = 243;
    int BRANCH_TRANSACTION_TRINARY_OFFSET = TRUNK_TRANSACTION_TRINARY_OFFSET + TRUNK_TRANSACTION_TRINARY_SIZE, BRANCH_TRANSACTION_TRINARY_SIZE = 243;
    int TAG_TRINARY_OFFSET = BRANCH_TRANSACTION_TRINARY_OFFSET + BRANCH_TRANSACTION_TRINARY_SIZE, TAG_TRINARY_SIZE = 81;
    int ATTACHMENT_TIMESTAMP_TRINARY_OFFSET = TAG_TRINARY_OFFSET + TAG_TRINARY_SIZE, ATTACHMENT_TIMESTAMP_TRINARY_SIZE = 27;
    int ATTACHMENT_TIMESTAMP_LOWER_BOUND_TRINARY_OFFSET = ATTACHMENT_TIMESTAMP_TRINARY_OFFSET + ATTACHMENT_TIMESTAMP_TRINARY_SIZE, ATTACHMENT_TIMESTAMP_LOWER_BOUND_TRINARY_SIZE = 27;
    int ATTACHMENT_TIMESTAMP_UPPER_BOUND_TRINARY_OFFSET = ATTACHMENT_TIMESTAMP_LOWER_BOUND_TRINARY_OFFSET + ATTACHMENT_TIMESTAMP_LOWER_BOUND_TRINARY_SIZE, ATTACHMENT_TIMESTAMP_UPPER_BOUND_TRINARY_SIZE = 27;
    int ESSENCE_TRINARY_OFFSET = ADDRESS_TRINARY_OFFSET, ESSENCE_TRINARY_SIZE = ADDRESS_TRINARY_SIZE + VALUE_TRINARY_SIZE + OBSOLETE_TAG_TRINARY_SIZE + TIMESTAMP_TRINARY_SIZE + CURRENT_INDEX_TRINARY_SIZE + LAST_INDEX_TRINARY_SIZE;
    int PREFILLED_SLOT = 1; // means that we know only hash of the tx, the rest is unknown yet: only another tx references that hash
    int FILLED_SLOT = -1; //  knows the hash only coz another tx references that hash
    int TAG_SIZE_IN_BYTES = 17; // = ceil(81 TRITS / 5 TRITS_PER_BYTE)
    int NONCE_TRINARY_OFFSET = ATTACHMENT_TIMESTAMP_UPPER_BOUND_TRINARY_OFFSET + ATTACHMENT_TIMESTAMP_UPPER_BOUND_TRINARY_SIZE, NONCE_TRINARY_SIZE = 81;
    int TRINARY_SIZE = NONCE_TRINARY_OFFSET + NONCE_TRINARY_SIZE;


    String getHash();

    Hash getTransactionHash();

    byte[] getBytes();

    String getAddress();

    long getArrivalTime();

    void setArrivalTime(long time);

    String getObsoleteTag();

    String getBundle();

    String getTrunk();

    String getBranch();

    String getTag();

    String getSignatureFragments();

    long getAttachmentTimestamp();

    long getAttachmentTimestampLowerBound();

    long getAttachmentTimestampUpperBound();

    long getValue();

    int getValidity();

    void setValidity(int validity);

    long getCurrentIndex();

    long getTimestamp();

    long getLastIndex();

    boolean isSolid();

    void setSolid(boolean solid);

    int getSnapshot();

    void setSnapshot(int index);

    long getHeight();

    void setHeight(long height);

    String getSender();

    void setSender(String sender);

    byte[] getTrits();

    Hash getBundleHash();

    List<Hash> getApprovers();

    Hash getTrunkTransactionHash();

    Hash getBranchTransactionHash();

    Hash getAddressHash();

    int getWeightMagnitude();

}
