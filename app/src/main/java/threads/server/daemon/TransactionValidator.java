package threads.server.daemon;

import threads.iota.crypto.Curl;

/**
 * Created by paul on 4/17/17.
 */
public class TransactionValidator {

    private static long MAX_TIMESTAMP_FUTURE = 2 * 60 * 60;


    public static void runValidation(ITransactionStorage transactionViewModel, final int minWeightMagnitude) {

        for (int i = ITransactionStorage.VALUE_TRINARY_OFFSET + ITransactionStorage.VALUE_USABLE_TRINARY_SIZE; i < ITransactionStorage.VALUE_TRINARY_OFFSET + ITransactionStorage.VALUE_TRINARY_SIZE; i++) {
            if (transactionViewModel.getTrits()[i] != 0) {
                throw new RuntimeException("Invalid transaction getValue");
            }
        }

        int weightMagnitude = transactionViewModel.getWeightMagnitude();
        if (weightMagnitude < minWeightMagnitude) {
            throw new RuntimeException("Invalid transaction hash");
        }

        if (transactionViewModel.getValue() != 0 && transactionViewModel.getAddressHash().trits()[Curl.HASH_LENGTH - 1] != 0) {
            throw new RuntimeException("Invalid transaction address");
        }
    }

    public static ITransactionStorage validateTrits(TransactionDatabase tangle, final byte[] trits, int minWeightMagnitude) {
        ITransactionStorage transactionViewModel = tangle.fromTrits(trits);
        runValidation(transactionViewModel, minWeightMagnitude);
        return transactionViewModel;
    }


}
