package threads.ipfs;

import android.content.Context;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import threads.ipfs.api.Peer;

import static junit.framework.TestCase.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class IpfsSwarmTest {

    private static String TAG = IpfsSwarmTest.class.getSimpleName();
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
    public void test_find_peers() throws Exception {

        IPFS ipfs = TestEnv.getTestInstance(context);


        java.lang.Thread.sleep(10000);

        List<Peer> foundPeers = new ArrayList<>();
        while (foundPeers.size() < 3) {


            List<Peer> peers = ipfs.swarmPeers();
            for (Peer peer : peers) {

                if (peer.isFloodSub() || peer.isMeshSub()) {
                    Log.e(TAG, "Peer " + peer.toString());


                    if (!foundPeers.contains(peer)) {
                        foundPeers.add(peer);
                    }


                    Log.e(TAG, "Connect to peer : " + ipfs.swarmConnect(peer, 2));
                }


            }


            Thread.sleep(5000);


        }


        for (Peer peer : foundPeers) {
            Log.e(TAG, "Found peer : " + peer.toString());
        }
    }
}
