package threads.ipfs;

import android.content.Context;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import threads.ipfs.api.Encryption;
import threads.ipfs.api.PeerInfo;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class EncryptionTest {

    private static Context context;

    @BeforeClass
    public static void setup() {
        context = ApplicationProvider.getApplicationContext();
    }


    @Test
    public void testPublicKey() throws Exception {
        IPFS ipfs = TestEnv.getTestInstance(context);


        String publicKey = ipfs.getPublicKey();
        Log.e(EncryptionTest.class.getSimpleName(), publicKey);
        assertNotNull(publicKey);
        PeerInfo peerInfo = ipfs.id();
        assertNotNull(peerInfo);
        assertEquals(publicKey, peerInfo.getPublicKey());
    }

    @Test
    public void encryptData() throws Exception {
        IPFS ipfs = TestEnv.getTestInstance(context);
        String publicKey = ipfs.getPublicKey();
        String privateKey = ipfs.getPrivateKey();

        String seed = getRandomString(50);
        String encrypt = Encryption.encryptRSA(seed, publicKey);
        String result = Encryption.decryptRSA(encrypt, privateKey);
        assertEquals(seed, result);

    }

    @Test
    public void encryptDataId() throws Exception {
        IPFS ipfs = TestEnv.getTestInstance(context);
        String publicKey = ipfs.id().getPublicKey();
        String privateKey = ipfs.getPrivateKey();
        for (int i = 0; i < 20; i++) {
            String seed = getRandomString(100);
            String encrypt = Encryption.encryptRSA(seed, publicKey);
            String result = Encryption.decryptRSA(encrypt, privateKey);
            assertEquals(seed, result);
        }
    }


    @Test
    public void generateKey() throws NoSuchAlgorithmException, IllegalBlockSizeException, InvalidKeyException, BadPaddingException, NoSuchPaddingException {

        String password = Encryption.generateAESKey();
        String data = getRandomString(100);
        String encrpt = Encryption.encrypt(data, password);
        String decrypt = Encryption.decrypt(encrpt, password);
        assertEquals(data, decrypt);

    }

    private String getRandomString(int number) {
        return "" + RandomStringUtils.random(number);

    }


}
