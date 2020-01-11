package threads.ipfs;


import android.content.Context;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;

import threads.ipfs.api.CID;
import threads.ipfs.api.LinkInfo;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertNotEquals;

@RunWith(AndroidJUnit4.class)
public class IpfsAddTest {

    private static final String TAG = IpfsAddTest.class.getSimpleName();
    private static Context context;

    @BeforeClass
    public static void setup() {
        context = ApplicationProvider.getApplicationContext();
    }

    private byte[] getRandomBytes(int number) {
        return RandomStringUtils.randomAlphabetic(number).getBytes();
    }

    @Test
    public void add_wrap_test() throws Exception {

        IPFS ipfs = TestEnv.getTestInstance(context);

        int packetSize = 1000;
        long maxData = 1000;
        File inputFile = ipfs.getTempCacheFile();
        for (int i = 0; i < maxData; i++) {
            byte[] randomBytes = getRandomBytes(packetSize);
            FileServer.insertRecord(inputFile, i, 0, packetSize, randomBytes);
        }
        long size = inputFile.length();

        Log.e(TAG, "Bytes : " + inputFile.length() / 1000 + "[kb]");

        CID hash58Base = ipfs.streamFile(inputFile, true);
        assertNotNull(hash58Base);

        List<LinkInfo> links = ipfs.ls(hash58Base, 10, true);
        assertNotNull(links);
        assertEquals(links.size(), 4);

        byte[] bytes = ipfs.getData(hash58Base, 10, true);
        assertNotNull(bytes);
        assertEquals(bytes.length, size);

        IOUtils.contentEquals(new ByteArrayInputStream(bytes), new FileInputStream(inputFile));


    }

    @Test
    public void add_test() throws Exception {

        int packetSize = 1000;
        long maxData = 1000;
        IPFS ipfs = TestEnv.getTestInstance(context);

        File inputFile = ipfs.getTempCacheFile();
        for (int i = 0; i < maxData; i++) {
            byte[] randomBytes = getRandomBytes(packetSize);
            FileServer.insertRecord(inputFile, i, 0, packetSize, randomBytes);
        }
        long size = inputFile.length();

        Log.e(TAG, "Bytes : " + inputFile.length() / 1000 + "[kb]");

        CID hash58Base = ipfs.streamFile(inputFile, true);
        assertNotNull(hash58Base);

        List<LinkInfo> links = ipfs.ls(hash58Base, 10, true);
        assertNotNull(links);
        assertEquals(links.size(), 4);
        assertNotEquals(links.get(0).getCid(), hash58Base);

        byte[] bytes = ipfs.getData(hash58Base, 30, true);
        assertNotNull(bytes);
        assertEquals(bytes.length, size);

        IOUtils.contentEquals(new ByteArrayInputStream(bytes), new FileInputStream(inputFile));

    }


    @Test
    public void add_wrap_small_test() throws Exception {

        int packetSize = 200;
        long maxData = 1000;
        IPFS ipfs = TestEnv.getTestInstance(context);

        File inputFile = ipfs.getTempCacheFile();
        for (int i = 0; i < maxData; i++) {
            byte[] randomBytes = getRandomBytes(packetSize);
            FileServer.insertRecord(inputFile, i, 0, packetSize, randomBytes);
        }
        long size = inputFile.length();


        Log.e(TAG, "Bytes : " + inputFile.length() / 1000 + "[kb]");

        CID hash58Base = ipfs.streamFile(inputFile, true);
        assertNotNull(hash58Base);

        List<LinkInfo> links = ipfs.ls(hash58Base, 10, true);
        assertNotNull(links);
        assertEquals(links.size(), 0);

        byte[] bytes = ipfs.getData(hash58Base, 10, true);
        assertNotNull(bytes);
        assertEquals(bytes.length, size);


        IOUtils.contentEquals(new ByteArrayInputStream(bytes), new FileInputStream(inputFile));


    }

    @Test
    public void add_small_test() throws Exception {

        int packetSize = 200;
        long maxData = 1000;
        IPFS ipfs = TestEnv.getTestInstance(context);

        File inputFile = ipfs.getTempCacheFile();
        for (int i = 0; i < maxData; i++) {
            byte[] randomBytes = getRandomBytes(packetSize);
            FileServer.insertRecord(inputFile, i, 0, packetSize, randomBytes);
        }
        long size = inputFile.length();

        Log.e(TAG, "Bytes : " + inputFile.length() / 1000 + "[kb]");

        CID hash58Base = ipfs.streamFile(inputFile, true);
        assertNotNull(hash58Base);

        List<LinkInfo> links = ipfs.ls(hash58Base, 10, true);
        assertNotNull(links);
        assertEquals(links.size(), 0);

        byte[] bytes = ipfs.getData(hash58Base, 10, true);
        assertNotNull(bytes);
        assertEquals(bytes.length, size);

        IOUtils.contentEquals(new ByteArrayInputStream(bytes), new FileInputStream(inputFile));


    }
}
