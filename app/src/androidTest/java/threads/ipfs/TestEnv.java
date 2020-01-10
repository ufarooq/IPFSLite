package threads.ipfs;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicInteger;

import threads.ipfs.api.PubsubInfo;

public class TestEnv {
    private static final String TAG = TestEnv.class.getSimpleName();
    public static Hashtable<String, AtomicInteger> COUNTER = new Hashtable<>();

    public static IPFS getTestInstance(@NonNull Context context) throws Exception {


        IPFS.setPubsubHandler(new IPFS.PubsubHandler() {
            @Override
            public void receive(@NonNull PubsubInfo message) {
                AtomicInteger value = COUNTER.get(message.getTopic());
                if (value != null) {
                    value.incrementAndGet();
                } else {
                    COUNTER.put(message.getTopic(), new AtomicInteger(1));
                }

            }
        });
        IPFS ipfs = IPFS.getInstance(context);


        if (!ipfs.isDaemonRunning()) {

            long time = System.currentTimeMillis();
            ipfs.daemon(true);
            Log.e(TAG, "Time Daemon : " + (System.currentTimeMillis() - time));
        } else {
            ipfs.gc();
            ipfs.cleanCacheDir();
            ipfs.logBaseDir();
        }
        return ipfs;
    }
}
