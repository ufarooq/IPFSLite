package threads.ipfs;

import java.math.BigInteger;

public class Base58 {
    private static final String ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
    private static final BigInteger BASE = BigInteger.valueOf(58);

    public static String encode(final byte[] input) {
        return BaseN.encode(ALPHABET, BASE, input);
    }

    public static byte[] decode(final String input) {
        return BaseN.decode(ALPHABET, BASE, input);
    }
}
