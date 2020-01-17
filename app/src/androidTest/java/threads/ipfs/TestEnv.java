package threads.ipfs;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicInteger;

public class TestEnv {
    private static final String TAG = TestEnv.class.getSimpleName();
    public static Hashtable<String, AtomicInteger> COUNTER = new Hashtable<>();

    public static IPFS getTestInstance(@NonNull Context context) throws Exception {


        IPFS.deleteConfigFile(context); // TODO remove later


        // Experimental Features
        IPFS.setQUICEnabled(context, true);
        IPFS.setPreferTLS(context, true);


        IPFS.setSwarmPort(context, 4001);
        IPFS.setRoutingType(context, RoutingConfig.TypeEnum.dhtclient);


        IPFS.setAutoNATServiceEnabled(context, false);
        IPFS.setRelayHopEnabled(context, false);
        IPFS.setAutoRelayEnabled(context, true);

        IPFS.setPubsubEnabled(context, true);
        IPFS.setPubsubRouter(context, PubsubConfig.RouterEnum.gossipsub);

        IPFS.setConnMgrConfigType(context, ConnMgrConfig.TypeEnum.basic);
        IPFS.setLowWater(context, 50);
        IPFS.setHighWater(context, 200);
        IPFS.setGracePeriod(context, "10s");

        IPFS.setMdnsEnabled(context, true);

        IPFS.setRandomSwarmPort(context, true);


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

        long time = System.currentTimeMillis();
        IPFS.setPubsubEnabled(context, true);
        IPFS ipfs = IPFS.getInstance(context);

        Log.e(TAG, "Time Daemon : " + (System.currentTimeMillis() - time));
        if (ipfs.isDaemonRunning()) {
            ipfs.gc();
            ipfs.cleanCacheDir();
            ipfs.logBaseDir();
        }
        return ipfs;
    }
}
