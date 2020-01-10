package threads.ipfs;


import android.content.Context;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

import threads.ipfs.api.CID;
import threads.ipfs.api.PID;
import threads.ipfs.api.PeerInfo;

import static junit.framework.TestCase.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class IpfsSwarmWriterTest {

    private static final String TAG = IpfsSwarmWriterTest.class.getSimpleName();

    private static Context context;

    @BeforeClass
    public static void setup() {
        context = ApplicationProvider.getApplicationContext();
    }


    private static String getRandomString(int number) {
        return "" + RandomStringUtils.randomAscii(number);
    }


    @Test
    public void dummy() {
        assertNotNull(context);
    }

    //@Test
    public void write() throws Exception {
        PID RELAY_PID = PID.create("QmWFhiem9PnRAm9pBHQYvRqQcGAeJ2VfSFhD3JKdytiWKG");

        IPFS ipfs = TestEnv.getTestInstance(context);
        Log.e(TAG, "Connecting to RELAY ...");
        boolean success = ipfs.swarmConnect(RELAY_PID, 10);
        Log.e(TAG, "Connecting to RELAY done " + success);


        PeerInfo info = ipfs.id();
        Log.e(TAG, "PID : " + info.getPID().getPid());


        File file = createRandomFile();

        CID hash58Base = ipfs.addFile(file, false);
        Log.e(TAG, "CID : " + hash58Base.getCid());


        try {
            while (true) {
                info = ipfs.id();
                Log.e(TAG, info.toString());
                Thread.sleep(60000);
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage());
        }
    }

    private File createRandomFile() throws Exception {


        int packetSize = 1000;
        long maxData = 10;

        IPFS ipfs = TestEnv.getTestInstance(context);

        File inputFile = ipfs.getTempCacheFile();
        String randomString = getRandomString(packetSize);
        for (int i = 0; i < maxData; i++) {
            FileServer.insertRecord(inputFile, i, 0, packetSize, randomString.getBytes());
        }

        return inputFile;
    }
}
