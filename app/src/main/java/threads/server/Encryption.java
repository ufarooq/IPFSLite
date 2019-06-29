package threads.server;

import android.util.Base64;

import androidx.annotation.NonNull;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import static androidx.core.util.Preconditions.checkNotNull;

public class Encryption {

    private static final int RSA_KEY_SIZE = 2048;


    private static String decryptAES(@NonNull String text, @NonNull SecretKey aesKey) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        if (text.isEmpty()) return text;

        Cipher cipher = Cipher.getInstance("AES");
        // decrypt the text
        cipher.init(Cipher.DECRYPT_MODE, aesKey);
        byte[] decode = Base64.decode(text, 16);
        return new String(cipher.doFinal(decode));

    }

    private static String encryptAES(@NonNull String text, @NonNull SecretKey aesKey) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        checkNotNull(text);
        checkNotNull(aesKey);
        if (text.isEmpty()) return text;
        Cipher cipher = Cipher.getInstance("AES");
        // encrypt the text
        cipher.init(Cipher.ENCRYPT_MODE, aesKey);
        byte[] encodedBytes = cipher.doFinal(text.getBytes());
        return Base64.encodeToString(encodedBytes, 16);
    }


    private static KeyPair getRSAKeyPair() throws NoSuchAlgorithmException {

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(RSA_KEY_SIZE);
        return kpg.generateKeyPair();

    }

    private static String encryptRSAToString(@NonNull SecretKey secretKey, @NonNull String publicKey)
            throws NoSuchAlgorithmException, InvalidKeySpecException,
            NoSuchPaddingException, InvalidKeyException,
            BadPaddingException, IllegalBlockSizeException {

        KeyFactory keyFac = KeyFactory.getInstance("RSA");
        KeySpec keySpec = new X509EncodedKeySpec(Base64.decode(publicKey.trim().getBytes(), Base64.DEFAULT));
        Key key = keyFac.generatePublic(keySpec);

        // get an RSA cipher object and print the provider
        final Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWITHSHA-256ANDMGF1PADDING");
        // encrypt the plain text using the public key
        cipher.init(Cipher.ENCRYPT_MODE, key);

        byte[] encryptedBytes = cipher.doFinal(secretKey.getEncoded());
        String encryptedBase64 = new String(Base64.encode(encryptedBytes, Base64.DEFAULT));

        return encryptedBase64.replaceAll("(\\r|\\n)", "");
    }

    private static SecretKeySpec decryptRSAToString(
            @NonNull String encryptedBase64, @NonNull String privateKey)
            throws NoSuchAlgorithmException, InvalidKeySpecException,
            NoSuchPaddingException, InvalidKeyException, BadPaddingException,
            IllegalBlockSizeException {
        checkNotNull(encryptedBase64);
        checkNotNull(privateKey);
        KeyFactory keyFac = KeyFactory.getInstance("RSA");
        KeySpec keySpec = new PKCS8EncodedKeySpec(Base64.decode(privateKey.trim().getBytes(), Base64.DEFAULT));
        Key key = keyFac.generatePrivate(keySpec);

        // get an RSA cipher object and print the provider
        final Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWITHSHA-256ANDMGF1PADDING");
        // encrypt the plain text using the public key
        cipher.init(Cipher.DECRYPT_MODE, key);

        byte[] encryptedBytes = Base64.decode(encryptedBase64, Base64.DEFAULT);
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
        return new SecretKeySpec(decryptedBytes, 0, decryptedBytes.length, "AES");

    }


    public static PrivateKey getPrivateKey(String base64PrivateKey) {
        PrivateKey privateKey = null;
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(
                Base64.decode(base64PrivateKey.getBytes(), Base64.DEFAULT));
        KeyFactory keyFactory = null;
        try {
            keyFactory = KeyFactory.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        try {
            privateKey = keyFactory.generatePrivate(keySpec);
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return privateKey;
    }

    public static PublicKey getPublicKey(String base64PublicKey) {
        PublicKey publicKey = null;
        try {
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(
                    Base64.decode(base64PublicKey.getBytes(), Base64.DEFAULT));
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            publicKey = keyFactory.generatePublic(keySpec);
            return publicKey;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return publicKey;
    }

    public static String decrypt(byte[] data, PrivateKey privateKey) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return new String(cipher.doFinal(data));
    }

    public static String encryptRSA(String data, String publicKey) throws BadPaddingException, IllegalBlockSizeException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, getPublicKey(publicKey));
        byte[] bytes = cipher.doFinal(data.getBytes());
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    public static String decryptRSA(String data, String base64PrivateKey) throws IllegalBlockSizeException, InvalidKeyException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException {
        return decrypt(Base64.decode(data.getBytes(), Base64.DEFAULT), getPrivateKey(base64PrivateKey));
    }


    public static String decrypt(@NonNull String text, @NonNull String key) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        checkNotNull(text);
        checkNotNull(key);
        if (text.isEmpty()) return text;
        if (key.isEmpty()) return text;
        Key aesKey = getKey(key);
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, aesKey);
        byte[] decode = Base64.decode(text, 16);
        return new String(cipher.doFinal(decode));

    }


    public static SecretKeySpec getKey(String password) throws NoSuchAlgorithmException {
        checkNotNull(password);
        byte[] pass = password.getBytes(StandardCharsets.UTF_8);
        MessageDigest sha = MessageDigest.getInstance("SHA-256");

        byte[] key = sha.digest(pass);
        // use only first 128 bit (16 bytes). By default Java only supports AES 128 bit key sizes for encryption.
        // Updated jvm policies are required for 256 bit.
        key = Arrays.copyOf(key, 16);
        return new SecretKeySpec(key, "AES");
    }


}
