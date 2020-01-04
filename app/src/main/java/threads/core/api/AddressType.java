package threads.core.api;

import androidx.annotation.NonNull;

import org.iota.jota.utils.Constants;
import org.iota.jota.utils.TrytesConverter;

import threads.iota.IOTA;
import threads.ipfs.api.PID;

import static androidx.core.util.Preconditions.checkNotNull;

public enum AddressType {
    INBOX, PEER, NOTIFICATION;

    @NonNull
    public static String getAddress(@NonNull PID pid, @NonNull AddressType addressType) {
        checkNotNull(pid);
        checkNotNull(addressType);
        String address = TrytesConverter.asciiToTrytes(pid.getPid());
        checkNotNull(address);
        switch (addressType) {
            case PEER:
                return IOTA.addChecksum(
                        address.substring(0, Constants.ADDRESS_LENGTH_WITHOUT_CHECKSUM));
            case INBOX:
                return IOTA.addChecksum(
                        address.substring(address.length() - Constants.ADDRESS_LENGTH_WITHOUT_CHECKSUM));
            case NOTIFICATION:
                StringBuilder notification = new StringBuilder();
                notification.append(address.substring(address.length() -
                        Constants.ADDRESS_LENGTH_WITHOUT_CHECKSUM));
                notification = notification.reverse();
                return IOTA.addChecksum(notification.toString());
        }
        throw new RuntimeException("Not supported address type");
    }
}
