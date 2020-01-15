package threads.ipfs;


import android.content.Context;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static junit.framework.TestCase.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class IpfsAutoRelayTest {

    private static String TAG = IpfsAutoRelayTest.class.getSimpleName();
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
    public void testAutoRelay() throws Exception {

        ExperimentalConfig experimentalConfig = ExperimentalConfig.create();
        experimentalConfig.setQUIC(true);

        SwarmConfig swarmConfig = SwarmConfig.create();
        swarmConfig.setEnableAutoRelay(true);

        ConnMgrConfig mgr = swarmConfig.getConnMgr();
        mgr.setGracePeriod("10s");
        mgr.setHighWater(60);
        mgr.setLowWater(30);
        mgr.setType(ConnMgrConfig.TypeEnum.basic);

        IPFS ipfs = TestEnv.getTestInstance(context);


        ipfs.config_show();


        AtomicInteger counter = new AtomicInteger(0);
        while (counter.incrementAndGet() < 40) {


            boolean result = ipfs.swarmConnect("/ip4/2.207.31.127/tcp/4001/ipfs/QmPdjjouL3gJHEAJxosF964QQYG6AXCsVoeXktaKkN6i51", 30000);
            //assertTrue(result);


            Peer peer = ipfs.swarmPeer(PID.create("QmPdjjouL3gJHEAJxosF964QQYG6AXCsVoeXktaKkN6i51"));


            //assertNotNull(peer);

            if (peer != null) {
                Log.e(TAG, "" + peer.toString());
            }


            ipfs.swarmConnect("/ip4/18.224.32.43/tcp/4001/ipfs/QmaXbbcs7LRFuEoQcxfXqziZATzS68WT5DgFjYFgn3YYLX", 10000);


            ipfs.swarmPeers();

            PeerInfo info = ipfs.id();

            Log.e(TAG, "" + info.toString());


            List<String> addresses = info.getMultiAddresses();
            for (String address : addresses) {

                if (address.contains("p2p-circuit")) {
                    Log.e(TAG, "" + address);

                    String[] res = address.split("p2p-circuit");
                    // TODO maybe ipfs is not valid anymore
                    String moin = res[1].replaceFirst("/ipfs/", "");
                    PeerInfo moinInfo = ipfs.id(PID.create(moin), 10);
                    if (moinInfo != null) {
                        Log.e(TAG, moinInfo.toString());

                        Log.e(TAG, "" + ipfs.swarmPeer(moinInfo.getPID()).toString());
                    }


                }

            }


            Thread.sleep(10000);
        }


    }
}
