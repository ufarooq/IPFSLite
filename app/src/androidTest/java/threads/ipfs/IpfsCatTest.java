package threads.ipfs;

import android.content.Context;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.j256.simplemagic.ContentInfo;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNull;


@RunWith(AndroidJUnit4.class)
public class IpfsCatTest {

    private static String TAG = IpfsCatTest.class.getSimpleName();
    private static Context context;

    @BeforeClass
    public static void setup() {
        context = ApplicationProvider.getApplicationContext();
    }

    @Test
    public void cat_test() throws Exception {

        IPFS ipfs = TestEnv.getTestInstance(context);
        CID cid = CID.create("Qmaisz6NMhDB51cCvNWa1GMS7LU1pAxdF4Ld6Ft9kZEP2a");
        long time = System.currentTimeMillis();
        List<PID> provs = ipfs.dhtFindProviders(cid, 10, 45);
        for (PID prov : provs) {
            Log.e(TAG, "Provider " + prov.getPid());
        }
        Log.e(TAG, "Time Providers : " + (System.currentTimeMillis() - time) + " [ms]");

        time = System.currentTimeMillis();
        List<LinkInfo> res = ipfs.ls(cid, 10, false);
        Log.e(TAG, "Time : " + (System.currentTimeMillis() - time) + " [ms]");
        assertNotNull(res);
        assertTrue(res.isEmpty());

        time = System.currentTimeMillis();
        byte[] content = ipfs.loadData(cid, 10);

        Log.e(TAG, "Time : " + (System.currentTimeMillis() - time) + " [ms]");

        assertNotNull(content);


        time = System.currentTimeMillis();
        ipfs.rm(cid);
        Log.e(TAG, "Time : " + (System.currentTimeMillis() - time) + " [ms]");

    }


    @Test
    public void cat_not_exist() throws Exception {


        IPFS ipfs = TestEnv.getTestInstance(context);
        CID cid = CID.create("QmUNLLsPACCz1vLxQVkXqqLX5R1X345qqfHbsf67hvA3Nt");


        byte[] content = ipfs.loadData(cid, 10);

        assertNull(content);

    }


    //@Test
    public void cat_test_local() throws Exception {


        IPFS ipfs = TestEnv.getTestInstance(context);
        CID cid = CID.create("Qme6rRsAb8YCfmQpvDsobZAiWNRefcJw8eFw3WV4pME82V");

        CID local = ipfs.storeText("Moin Moin Moin");
        assertNotNull(local);


        byte[] content = ipfs.getData(cid);

        assertNotNull(content);

    }


    @Test
    public void cat_empty() throws Exception {


        IPFS ipfs = TestEnv.getTestInstance(context);
        CID cid = CID.create("QmUNLLsPACCz1vLxQVkXqqLX5R1X345qqfHbsf67hvA3Nn");
        List<LinkInfo> res = ipfs.ls(cid, 10, false);
        assertNotNull(res);

        assertTrue(res.isEmpty());
        byte[] content = ipfs.loadData(cid, 10);

        assertNotNull(content);
        assertEquals(content.length, 0);

        ipfs.rm(cid);

    }

    @Test
    public void guess_html() throws Exception {


        IPFS ipfs = TestEnv.getTestInstance(context);

        CID cid = ipfs.storeText("<html>moin</html");
        assertNotNull(cid);
        ContentInfo info = ipfs.getContentInfo(cid);
        assertNotNull(info);
        assertEquals(info.getMimeType(), "text/html");
        assertEquals(info.getName(), "html");
    }
}