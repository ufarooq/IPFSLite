package threads.ipfs;


import android.content.Context;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import threads.ipfs.api.PID;
import threads.ipfs.api.Peer;
import threads.ipfs.api.PeerInfo;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

@RunWith(AndroidJUnit4.class)
public class IpfsFindPeer {
    private static final String TAG = IpfsFindPeer.class.getSimpleName();
    private static final String RELAY_PID = "QmVLnkyetpt7JNpjLmZX21q9F8ZMnhBts3Q53RcAGxWH6U";
    private static final String DUMMY_PID = "QmVLnkyetpt7JNpjLmZX21q9F8ZMnhBts3Q53RcAGxWH6V";

    private static Context context;

    @BeforeClass
    public static void setup() {
        context = ApplicationProvider.getApplicationContext();
    }


    @Test
    public void dummy() {
        assertNotNull(context);
    }

    @Test
    public void no_id() throws Exception {

        IPFS ipfs = TestEnv.getTestInstance(context);

        PeerInfo info = ipfs.id();
        assertNotNull(info);
        assertEquals(info.getProtocolVersion(), "ipfs/0.1.0");
        Log.e(TAG, info.getPID().getPid());

        PID dummy = PID.create(DUMMY_PID);

        PeerInfo dummyInfo = ipfs.id(dummy, 5);
        assertNull(dummyInfo);

    }

    @Test
    public void swarm_connect() throws Exception {

        IPFS ipfs = TestEnv.getTestInstance(context);
        PID pc = PID.create("QmRxoQNy1gNGMM1746Tw8UBNBF8axuyGkzcqb2LYFzwuXd");

        // TIMEOUT not working
        boolean result = ipfs.swarmConnect(pc, 6);
        assertFalse(result);

    }


    //libp@Test
    public void test_find_peer() throws Exception {
        IPFS ipfs = TestEnv.getTestInstance(context);

        PID relay = PID.create(RELAY_PID);
        PID dummy = PID.create(DUMMY_PID);
        PeerInfo peerInfo = ipfs.id(relay, 5);
        assertNotNull(peerInfo);

        PeerInfo dummyInfo = ipfs.id(dummy, 5);
        assertNotNull(dummyInfo);

        ipfs.swarmConnect(relay, 10);


        ipfs.relay(relay, dummy, 10);

        List<Peer> peers = ipfs.swarmPeers();
        assertNotNull(peers);
        for (Peer peer : peers) {

            if (peer.getPid().equals(relay)) {
                Log.e("fds", "Connect with specificRelay PID");
            }

        }


    }

    //@Test
    public void test_local_peer() throws Exception {
        IPFS ipfs = TestEnv.getTestInstance(context);

        PID local = PID.create("Qmf5TsSK8dVm3btzuUrnvS8wfUW6e2vMxMRkzV9rsG6eDa");

        PeerInfo peerInfo = ipfs.id(local, 60);
        assertNotNull(peerInfo);

        boolean result = ipfs.swarmConnect(local, 60);

        assertTrue(result);

        while (ipfs.isConnected(local)) {
            Log.e(TAG, "Peer conntected with : " + local.getPid());
            Thread.sleep(1000);
        }
    }

    //@Test
    public void test_fail_find_peer() throws Exception {
        IPFS ipfs = TestEnv.getTestInstance(context);

        PID relay = PID.create(DUMMY_PID);
        PID dummy = PID.create(DUMMY_PID);
        PeerInfo peerInfo = ipfs.id(relay, 10);
        assertNull(peerInfo);

        Log.e(TAG, "relay_connect");
        ipfs.swarmConnect(relay, 10);


        ipfs.relay(relay, dummy, 10);

        Log.e(TAG, "swarmPeers");

        List<Peer> peers = ipfs.swarmPeers();
        assertNotNull(peers);
        for (Peer peer : peers) {
            Log.e(TAG, peer.toString());

            if (peer.getPid().equals(relay)) {
                Log.e(TAG, "Connect with specificRelay PID");
            }

        }

    }

    @Test
    public void test_swarm_connect() throws Exception {
        IPFS ipfs = TestEnv.getTestInstance(context);

        PID relay = PID.create("QmchgNzyUFyf2wpfDMmpGxMKHA3PkC1f3H2wUgbs21vXoz");


        boolean connected = ipfs.isConnected(relay);
        assertFalse(connected);


        boolean result = ipfs.swarmConnect(relay, 1);
        assertFalse(result);


        relay = PID.create("QmchgNzyUFyf2wpfDMmpGxMKHA3PkC1f3H2wUgbs21vXoz");
        result = ipfs.swarmConnect(relay, 10);
        assertFalse(result);


        relay = PID.create(DUMMY_PID);
        result = ipfs.swarmConnect(relay, 10);
        assertFalse(result);

    }


    @Test
    public void test_find_swarm_peers() throws Exception {
        IPFS ipfs = TestEnv.getTestInstance(context);


        Thread.sleep(30000);


        AtomicBoolean found = new AtomicBoolean(false);

        while (!found.get()) {
            List<Peer> peers = ipfs.swarmPeers();

            assertNotNull(peers);
            Log.e(TAG, "Peers : " + peers.size());
            for (Peer peer : peers) {

                long time = System.currentTimeMillis();
                Log.e(TAG, "isConnected : " + ipfs.isConnected(peer.getPid())
                        + " " + (System.currentTimeMillis() - time));

                Log.e(TAG, peer.toString());

                PeerInfo peerInfo = ipfs.id(peer, 10);
                if (peerInfo != null) {
                    String publicKey = peerInfo.getPublicKey();
                    assertNotNull(publicKey);
                    Log.e(TAG, peerInfo.toString());
                }


                if (ipfs.isConnected(peer.getPid())) {

                    ipfs.protectPeer(peer.getPid(), "hi");
                    boolean stillProtect = ipfs.unProtectPeer(peer.getPid(), "moin");
                    assertTrue(stillProtect);
                    stillProtect = ipfs.unProtectPeer(peer.getPid(), "hi");
                    assertFalse(stillProtect);
                }


                if (peer.isRelay()) {

                    boolean result = ipfs.checkRelay(peer, 10);
                    Log.e(TAG, "Relay : " + result);

                    if (result) {


                        boolean value = ipfs.swarmConnect(peer.getPid(), 10);
                        Log.e(TAG, "Connect : " + value);

                        Log.e(TAG, peer.toString());


                        found.set(true);
                    }
                }
            }


            Thread.sleep(10000);
        }

    }

}
