package threads.ipfs;


import android.content.Context;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.TestCase.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class IpfsSwarmReaderTest {


    private static final String TAG = IpfsSwarmReaderTest.class.getSimpleName();

    private static Context context;

    @BeforeClass
    public static void setup() {
        context = ApplicationProvider.getApplicationContext();
    }


    @Test
    public void dummy() {
        assertNotNull(context);
    }

    //@Test
    public void read() throws Exception {
        PID RELAY_PID = PID.create("QmWFhiem9PnRAm9pBHQYvRqQcGAeJ2VfSFhD3JKdytiWKG");
        PID pid = PID.create("QmNckvjTSaobsMFFbozbNRHiHp3EykHgf3khaiGu1wzLs2");
        String cid = "QmaFuc7VmzwT5MAx3EANZiVXRtuWtTwALjgaPcSsZ2J2ip";

        IPFS ipfs = TestEnv.getTestInstance(context);

        Log.e(TAG, "Connecting to RELAY ...");
        boolean success = ipfs.swarmConnect(RELAY_PID, 10);

        Log.e(TAG, "Connecting to RELAY done " + success);

        long now = System.currentTimeMillis();


        byte[] data = ipfs.getData(CID.create(cid), 10, false);

        Log.e(TAG, "Bytes : " + data.length / 1000 + "[kb]" +
                " Time : " + ((System.currentTimeMillis() - now) / 1000) + "[s]");


    }

}
