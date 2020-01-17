package threads.ipfs;

import android.content.Context;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNull;

@RunWith(AndroidJUnit4.class)
public class IpfsPerformance {
    private static final String TAG = IpfsPerformance.class.getSimpleName();
    private static Context context;

    @BeforeClass
    public static void setup() {
        context = ApplicationProvider.getApplicationContext();
    }


    private String getRandomString(int number) {
        return "" + RandomStringUtils.random(number, true, false);
    }


    private byte[] getRandomBytes(int number) {
        return RandomStringUtils.randomAlphabetic(number).getBytes();
    }


    @Test
    public void test_add_cat_small() throws Exception {

        int packetSize = 1000;
        long maxData = 100;

        IPFS ipfs = TestEnv.getTestInstance(context);


        File inputFile = ipfs.getTempCacheFile();
        for (int i = 0; i < maxData; i++) {
            byte[] randomBytes = getRandomBytes(packetSize);
            FileServer.insertRecord(inputFile, i, 0, packetSize, randomBytes);
        }
        long size = inputFile.length();


        Log.e(TAG, "Bytes : " + inputFile.length() / 1000 + "[kb]");
        long now = System.currentTimeMillis();
        CID cid = ipfs.storeFile(inputFile);
        assertNotNull(cid);
        Log.e(TAG, "Add : " + cid.getCid() +
                " Time : " + ((System.currentTimeMillis() - now) / 1000) + "[s]");


        now = System.currentTimeMillis();

        byte[] data = ipfs.getData(cid);


        Log.e(TAG, "Cat : " + cid +
                " Time : " + ((System.currentTimeMillis() - now) / 1000) + "[s]");

        assertEquals(data.length, size);

        File file = ipfs.getTempCacheFile();
        ipfs.storeToFile(file, cid);

        assertEquals(file.length(), size);

        assertTrue(file.delete());
        assertTrue(inputFile.delete());


        ipfs.rm(cid);
        ipfs.gc();


        data = ipfs.getData(cid);
        assertNull(data);

        data = ipfs.getData(cid);
        assertNull(data);

    }


    @Test
    public void test_cmp_files() throws Exception {

        int packetSize = 10000;
        long maxData = 5000;


        IPFS ipfs = TestEnv.getTestInstance(context);

        File inputFile = ipfs.getTempCacheFile();
        for (int i = 0; i < maxData; i++) {
            byte[] randomBytes = getRandomBytes(packetSize);
            FileServer.insertRecord(inputFile, i, 0, packetSize, randomBytes);
        }
        long size = inputFile.length();


        Log.e(TAG, "Bytes : " + inputFile.length() / 1000 + "[kb]");

        CID cid = ipfs.storeFile(inputFile);
        assertNotNull(cid);
        File file = ipfs.getTempCacheFile();
        ipfs.storeToFile(file, cid);

        assertEquals(file.length(), size);

        assertTrue(FileUtils.contentEquals(inputFile, file));

        assertTrue(file.delete());
        assertTrue(inputFile.delete());


        ipfs.rm(cid);
        ipfs.gc();

    }

    @Test
    public void test_add_cat() throws Exception {


        IPFS ipfs = TestEnv.getTestInstance(context);

        int packetSize = 10000;
        long maxData = 10000;


        File inputFile = ipfs.getTempCacheFile();
        for (int i = 0; i < maxData; i++) {
            byte[] randomBytes = getRandomBytes(packetSize);
            FileServer.insertRecord(inputFile, i, 0, packetSize, randomBytes);
        }

        long size = inputFile.length();


        Log.e(TAG, "Bytes : " + inputFile.length() / 1000 + "[kb]");


        long now = System.currentTimeMillis();
        CID cid = ipfs.storeFile(inputFile);
        assertNotNull(cid);
        Log.e(TAG, "Add : " + cid.getCid() +
                " Time : " + ((System.currentTimeMillis() - now) / 1000) + "[s]");


        now = System.currentTimeMillis();
        CID hash58Base_2 = ipfs.storeFile(inputFile);
        assertNotNull(hash58Base_2);
        Log.e(TAG, "Add : " + hash58Base_2.getCid() +
                " Time : " + ((System.currentTimeMillis() - now) / 1000) + "[s]");


        now = System.currentTimeMillis();
        File outputFile1 = ipfs.getTempCacheFile();
        ipfs.storeToFile(outputFile1, cid);
        Log.e(TAG, "Cat : " + cid.getCid() +
                " Time : " + ((System.currentTimeMillis() - now) / 1000) + "[s]");


        now = System.currentTimeMillis();
        File outputFile2 = ipfs.getTempCacheFile();
        ipfs.storeToFile(outputFile2, cid);
        Log.e(TAG, "Cat : " + cid.getCid() +
                " Time : " + ((System.currentTimeMillis() - now) / 1000) + "[s]");


        assertEquals(outputFile1.length(), size);
        assertEquals(outputFile2.length(), size);
        assertTrue(outputFile2.delete());
        assertTrue(outputFile1.delete());
        assertTrue(inputFile.delete());

        ipfs.rm(cid);
        ipfs.gc();

    }


}
