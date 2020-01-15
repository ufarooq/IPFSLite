package threads.ipfs;

import android.util.Base64;

import androidx.annotation.NonNull;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import static androidx.core.util.Preconditions.checkArgument;
import static androidx.core.util.Preconditions.checkNotNull;

public class Encryption {
    private static final String AES = "AES";
    private static final String RSA = "RSA";
    private static final int AES_KEY_SIZE = 128;


    public static String generateAESKey() throws NoSuchAlgorithmException {
        SecretKey aesKey = Encryption.getAESKey();
        return new String(Base64.encode(aesKey.getEncoded(), Base64.DEFAULT));
    }

    private static SecretKey getAESKey() throws NoSuchAlgorithmException {
        KeyGenerator generator = KeyGenerator.getInstance(AES);
        generator.init(AES_KEY_SIZE);
        return generator.generateKey();
    }


    public static String encrypt(@NonNull String text, @NonNull String key)
            throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        checkNotNull(text);
        checkNotNull(key);
        if (text.isEmpty()) return text;
        if (key.isEmpty()) return text;
        Key aesKey = getKey(key);
        Cipher cipher = Cipher.getInstance(AES);
        cipher.init(Cipher.ENCRYPT_MODE, aesKey);
        byte[] encodedBytes = cipher.doFinal(text.getBytes());
        return Base64.encodeToString(encodedBytes, 16);
    }


    private static PrivateKey getPrivateKey(String base64PrivateKey)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        checkNotNull(base64PrivateKey);

        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(
                Base64.decode(base64PrivateKey.getBytes(), Base64.DEFAULT));
        KeyFactory keyFactory = KeyFactory.getInstance(RSA);

        return keyFactory.generatePrivate(keySpec);
    }

    private static PublicKey getPublicKey(String base64PublicKey)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        checkNotNull(base64PublicKey);

        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(
                Base64.decode(base64PublicKey.getBytes(), Base64.DEFAULT));
        KeyFactory keyFactory = KeyFactory.getInstance(RSA);
        return keyFactory.generatePublic(keySpec);

    }

    private static String decrypt(byte[] data, PrivateKey privateKey) throws NoSuchPaddingException,
            NoSuchAlgorithmException, InvalidKeyException,
            BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return new String(cipher.doFinal(data));
    }

    public static String encryptRSA(String data, String publicKey) throws BadPaddingException,
            IllegalBlockSizeException, InvalidKeyException,
            NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeySpecException {
        checkNotNull(data);
        checkNotNull(publicKey);
        checkArgument(!data.isEmpty());
        checkArgument(!publicKey.isEmpty());
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, getPublicKey(publicKey));
        byte[] bytes = cipher.doFinal(data.getBytes());
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    public static String decryptRSA(String data, String base64PrivateKey) throws
            IllegalBlockSizeException, InvalidKeyException,
            BadPaddingException, NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidKeySpecException {
        checkNotNull(data);
        checkNotNull(base64PrivateKey);
        checkArgument(!data.isEmpty());
        checkArgument(!base64PrivateKey.isEmpty());

        return decrypt(Base64.decode(data.getBytes(), Base64.DEFAULT),
                getPrivateKey(base64PrivateKey));
    }


    public static String decrypt(@NonNull String text, @NonNull String key) throws
            NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        checkNotNull(text);
        checkNotNull(key);
        if (text.isEmpty()) return text;
        if (key.isEmpty()) return text;
        Key aesKey = getKey(key);
        Cipher cipher = Cipher.getInstance(AES);
        cipher.init(Cipher.DECRYPT_MODE, aesKey);
        byte[] decode = Base64.decode(text, 16);
        return new String(cipher.doFinal(decode));

    }


    public static SecretKeySpec getKey(String key) throws NoSuchAlgorithmException {
        checkNotNull(key);
        byte[] pass = key.getBytes(StandardCharsets.UTF_8);
        MessageDigest sha = MessageDigest.getInstance("SHA-256");

        byte[] data = sha.digest(pass);
        // use only first 128 bit (16 bytes). By default Java only supports AES 128 bit key sizes for encryption.
        // Updated jvm policies are required for 256 bit.
        data = Arrays.copyOf(data, 16);
        return new SecretKeySpec(data, AES);
    }


}
