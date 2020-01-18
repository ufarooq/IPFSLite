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

        // Experimental Features
        IPFS.setQUICEnabled(context, true);
        IPFS.setPreferTLS(context, true);


        IPFS.setSwarmPort(context, 4001);
        IPFS.setRoutingType(context, RoutingConfig.TypeEnum.dhtclient);


        IPFS.setAutoNATServiceEnabled(context, false);
        IPFS.setRelayHopEnabled(context, false);
        IPFS.setAutoRelayEnabled(context, true);

        IPFS.setPubSubEnabled(context, true);
        IPFS.setPubSubRouter(context, PubSubConfig.RouterEnum.gossipsub);

        IPFS.setConnMgrConfigType(context, ConnMgrConfig.TypeEnum.basic);
        IPFS.setLowWater(context, 50);
        IPFS.setHighWater(context, 200);
        IPFS.setGracePeriod(context, "10s");

        IPFS.setMDNSEnabled(context, true);

        IPFS.setRandomSwarmPort(context, true);


        IPFS.setPubSubHandler(new IPFS.PubSubHandler() {
            @Override
            public void receive(@NonNull PubSubInfo message) {
                AtomicInteger value = COUNTER.get(message.getTopic());
                if (value != null) {
                    value.incrementAndGet();
                } else {
                    COUNTER.put(message.getTopic(), new AtomicInteger(1));
                }

            }
        });

        long time = System.currentTimeMillis();
        IPFS.setPubSubEnabled(context, true);
        IPFS ipfs = IPFS.getInstance(context);

        Log.e(TAG, "Time Daemon : " + (System.currentTimeMillis() - time));
        if (ipfs.isDaemonRunning()) {
            ipfs.gc();
            ipfs.logBaseDir();
        }
        return ipfs;
    }
}
