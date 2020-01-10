package threads.ipfs;

import android.content.Context;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.gson.Gson;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import threads.ipfs.api.PID;

import static androidx.core.util.Preconditions.checkNotNull;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;


@RunWith(AndroidJUnit4.class)
public class IpfsPubsubTest {

    private static final String TAG = IpfsPubsubTest.class.getSimpleName();
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
    public void test_simple() throws Exception {

        IPFS ipfs = TestEnv.getTestInstance(context);


        final String topic = "üüüüüüü";
        String text = "Hello Moin Zehn Elf äöö dsa";
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future future = executor.submit(() -> {
            try {
                ipfs.pubsubSub(topic, false);
            } catch (Throwable e) {
                Log.e(TAG, e.getLocalizedMessage(), e);
            }
        });

        Thread.sleep(2000);
        ipfs.pubsubPub(topic, text, 100);
        Thread.sleep(2000);


        List<String> peers = ipfs.pubsubPeers();
        assertNotNull(peers);


        future.cancel(true);
        assertTrue(TestEnv.COUNTER.get(topic).get() == 1);


    }


    @Test
    public void test_json() throws Exception {

        IPFS ipfs = TestEnv.getTestInstance(context);
        String topic = "moin33";
        Gson gson = new Gson();
        HashMap<String, String> map = new HashMap<>();
        map.put("moin", "zehn düp");
        map.put("elf", "zwölf a / zud");
        String text = gson.toJson(map) + System.lineSeparator();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future future = executor.submit(() -> {
            try {
                ipfs.pubsubSub(topic, false);
            } catch (Throwable e) {
                Log.e(TAG, e.getLocalizedMessage(), e);
            }
        });
        Thread.sleep(2000);
        ipfs.pubsubPub(topic, text, 100);
        Thread.sleep(2000);
        List<String> peers = ipfs.pubsubPeers();
        checkNotNull(peers);
        assertTrue(!peers.isEmpty());

        assertTrue(ipfs.isConnected(PID.create(peers.get(0))));
        future.cancel(true);
        assertTrue(TestEnv.COUNTER.get(topic).get() == 1);


    }


    @Test
    public void test_stress() throws Exception {

        IPFS ipfs = TestEnv.getTestInstance(context);


        String topic = "zeit";

        int maxMessages = 500;
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future future = executor.submit(() -> {
            try {
                ipfs.pubsubSub(topic, false);
            } catch (Throwable e) {
                Log.e(TAG, e.getLocalizedMessage(), e);
            }
        });

        Thread.sleep(1000);
        for (int i = 0; i < maxMessages; i++) {
            ipfs.pubsubPub(topic, "" + i, 30);
        }
        Thread.sleep(2000);


        List<String> peers = ipfs.pubsubPeers();
        assertNotNull(peers);


        future.cancel(true);
        assertEquals(TestEnv.COUNTER.get(topic).get(), maxMessages);


    }


    @Test
    public void default_pubsub_test() throws Exception {

        IPFS ipfs = TestEnv.getTestInstance(context);


        String topic = ipfs.getPid().getPid();

        int maxMessages = 50;


        Thread.sleep(1000);
        for (int i = 0; i < maxMessages; i++) {
            ipfs.pubsubPub(topic, "" + i, 30);
        }
        Thread.sleep(2000);


        List<String> peers = ipfs.pubsubPeers();
        assertNotNull(peers);


        assertEquals(TestEnv.COUNTER.get(topic).get(), maxMessages);


    }
}
