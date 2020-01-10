package threads.ipfs;

import android.content.Context;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import threads.ipfs.api.CID;
import threads.ipfs.api.LinkInfo;
import threads.ipfs.api.PID;
import threads.ipfs.api.Peer;
import threads.ipfs.api.PeerInfo;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;


@RunWith(AndroidJUnit4.class)
public class IpfsTest {
    private static String TAG = IpfsTest.class.getSimpleName();
    private static Context context;

    @BeforeClass
    public static void setup() {
        context = ApplicationProvider.getApplicationContext();
    }


    @Test
    public void test_versionAndPID() throws Exception {
        IPFS ipfs = TestEnv.getTestInstance(context);


        String version = ipfs.version();
        assertNotNull(version);
        assertFalse(version.isEmpty());

        PID pid = ipfs.getPeerID();
        Log.e(TAG, pid.getPid());


        PeerInfo info = ipfs.id(pid, 10);
        assertNotNull(info);
        assertEquals(pid.getPid(), info.getPID().getPid());

        assertNotNull(pid);
        assertEquals(version, "ipfs-lite/0.5.0");


        String config = ipfs.getConfigAsString();
        assertNotNull(config);


    }

    @Test
    public void test_swarm_disconnect() throws Exception {


        IPFS ipfs = TestEnv.getTestInstance(context);


        Thread.sleep(5000);


        List<Peer> peers = ipfs.swarmPeers();
        if (!peers.isEmpty()) {
            Peer peer = peers.get(0);
            Log.e(TAG, "Peer : " + peer.toString());

            ipfs.protectPeer(peer.getPid(), "a");
            ipfs.protectPeer(peer.getPid(), "b");
            ipfs.protectPeer(peer.getPid(), "c");
            assertTrue(ipfs.unProtectPeer(peer.getPid(), "c"));
            assertTrue(ipfs.unProtectPeer(peer.getPid(), "b"));
            assertFalse(ipfs.unProtectPeer(peer.getPid(), "a"));
            ipfs.swarmDisconnect(peer);
            Thread.sleep(5000);
            assertFalse(ipfs.isConnected(peer.getPid()));
        }

    }

    @Test
    public void streamTest() throws Exception {
        IPFS ipfs = TestEnv.getTestInstance(context);

        String test = "Moin";
        CID cid = ipfs.storeText(test, "", true);
        assertNotNull(cid);
        byte[] bytes = ipfs.getData(cid, 20, true);
        assertNotNull(bytes);
        assertEquals(test, new String(bytes));

        CID fault = CID.create(ipfs.getPeerID().getPid());

        bytes = ipfs.getData(fault, 20, false);
        assertNull(bytes);


    }

    @Test
    public void test_timeout_cat() throws Exception {

        String notValid = "QmaFuc7VmzwT5MAx3EANZiVXRtuWtTwALjgaPcSsZ2Jdip";
        IPFS ipfs = TestEnv.getTestInstance(context);

        byte[] bytes = ipfs.getData(CID.create(notValid), 10, false);

        assertNull(bytes);

    }


    @Test
    public void test_add_cat() throws Exception {
        IPFS ipfs = TestEnv.getTestInstance(context);

        String content = "Moin";
        CID hash58Base = ipfs.storeData(content.getBytes(), false);
        assertNotNull(hash58Base);
        Log.e(TAG, hash58Base.getCid());

        byte[] fileContents = ipfs.getData(hash58Base, 10, true);
        assertNotNull(fileContents);
        assertEquals(content, new String(fileContents));

        ipfs.rm(hash58Base);

        ipfs.gc();


    }


    @Test
    public void test_ls_timeout() throws Exception {

        IPFS ipfs = TestEnv.getTestInstance(context);

        List<LinkInfo> links = ipfs.ls(
                CID.create("QmXm3f7uKuFKK3QUL1V1oJZnpJSYX8c3vdhd94evSQUPCH"), 20,
                false);
        assertNull(links);

    }

    @Test
    public void test_ls_small() throws Exception {

        IPFS ipfs = TestEnv.getTestInstance(context);


        CID cid = ipfs.storeText("hallo", "", true);
        assertNotNull(cid);
        List<LinkInfo> links = ipfs.ls(cid, 20, true);
        assertNotNull(links);
        assertEquals(links.size(), 0);
        links = ipfs.ls(cid, 20, false);
        assertNotNull(links);
        assertEquals(links.size(), 0);
    }
}
