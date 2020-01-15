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

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

@RunWith(AndroidJUnit4.class)
public class IpfsRelayTest {

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
    public void relay() throws Exception {

        PubsubConfig pubsubConfig = PubsubConfig.create();
        pubsubConfig.setRouter(PubsubConfig.RouterEnum.floodsub);
        IPFS ipfs = TestEnv.getTestInstance(context);


        // find relay peer

        AtomicBoolean found = new AtomicBoolean(false);
        Peer relay = null;
        while (!found.get()) {
            List<Peer> peers = ipfs.swarmPeers();
            for (Peer peer : peers) {
                if (peer.isRelay()) {
                    Log.e(TAG, peer.toString());
                    found.set(true);
                    relay = peer;
                    break;
                }

            }

            Thread.sleep(1000);
        }


        while (ipfs.swarmConnect(relay, 20)) {
            Log.e(TAG, "New connected");
            while (ipfs.swarmPeer(relay.getPid()) != null) {
                Log.e(TAG, "Still connected");
                Thread.sleep(1000);




                    /*
                    PeerInfo info = ipfs.id();

                    Log.e(TAG, "" + info.toString());


                    List<String> addresses = info.getMultiAddresses();
                    for (String address : addresses) {

                        if (address.contains("p2p-circuit")) {
                            Log.e(TAG, "" + address);

                            String[] res = address.split("p2p-circuit");
                            String moin = res[1].replaceFirst("/ipfs/", "");
                            PeerInfo moinInfo = ipfs.id(PID.create(moin), 10);
                            if (moinInfo != null) {
                                Log.e(TAG, moinInfo.toString());

                                Log.e(TAG, "" + ipfs.swarmPeer(moinInfo.getPID()).toString());
                            }


                        }

                    }*/

            }


        }


    }

    //@Test
    public void relay_specific_user_online() throws Exception {
        PID RELAY_PID = PID.create("QmchgNzyUFyf2wpfDMmpGxMKHA3PkC1f3H2wUgbs21vXoh");
        PID USER_PID = PID.create("QmXm3f7uKuFKK3QUL1V1oJZnpJSYX8c3vdhd94evSQUPCH");


        IPFS ipfs = TestEnv.getTestInstance(context);


        boolean success = ipfs.swarmConnect(RELAY_PID, 10);
        assertFalse(success);

        success = ipfs.swarmConnect(RELAY_PID, 10);
        assertTrue(success);

        success = ipfs.swarmConnect(RELAY_PID, 10);
        assertTrue(success);


        for (int i = 0; i < 10; i++) {
            success = ipfs.relay(RELAY_PID, USER_PID, 10);
            assertTrue(success);
        }


    }

}
