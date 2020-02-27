package threads.server.core.peers;

import androidx.annotation.NonNull;

import org.iota.jota.utils.Constants;
import org.iota.jota.utils.TrytesConverter;

import threads.iota.IOTA;

public enum AddressType {
    INBOX, PEER, NOTIFICATION;

    @NonNull
    public static String getAddress(@NonNull String pid) {

        String address = TrytesConverter.asciiToTrytes(pid);
        return IOTA.addChecksum(address.substring(0, Constants.ADDRESS_LENGTH_WITHOUT_CHECKSUM));

    }
}
