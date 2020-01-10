package threads.ipfs;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.BeforeClass;
import org.junit.Test;

import threads.ipfs.api.AddressesConfig;
import threads.ipfs.api.Config;
import threads.ipfs.api.DiscoveryConfig;
import threads.ipfs.api.MDNSConfig;
import threads.ipfs.api.RoutingConfig;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

public class MobileWriterTest {

    private static final String TAG = MobileWriterTest.class.getSimpleName();

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
    public void newConfig() throws Exception {


        AddressesConfig addresses =
                AddressesConfig.create(4001, null);
        addresses.clearAnnounce();
        addresses.clearNoAnnounce();
        addresses.addNoAnnounce(
                "/ip4/10.0.0.0/ipcidr/8",
                "/ip4/100.64.0.0/ipcidr/10",
                "/ip4/169.254.0.0/ipcidr/16",
                "/ip4/172.16.0.0/ipcidr/12",
                "/ip4/192.0.0.0/ipcidr/24",
                "/ip4/192.0.0.0/ipcidr/29",
                "/ip4/192.0.0.8/ipcidr/32",
                "/ip4/192.0.0.170/ipcidr/32",
                "/ip4/192.0.0.171/ipcidr/32",
                "/ip4/192.0.2.0/ipcidr/24",
                "/ip4/192.168.0.0/ipcidr/16",
                "/ip4/198.18.0.0/ipcidr/15",
                "/ip4/198.51.100.0/ipcidr/24",
                "/ip4/203.0.113.0/ipcidr/24",
                "/ip4/240.0.0.0/ipcidr/4");


        DiscoveryConfig discoveryConfig = DiscoveryConfig.create();
        MDNSConfig mdnsConfig = discoveryConfig.getMdns();
        mdnsConfig.setEnabled(false);
        mdnsConfig.setInterval(0);


        IPFS ipfs = TestEnv.getTestInstance(context);

        String result = ipfs.config_show();
        assertNotNull(result);

        Config config = ipfs.getConfig();
        assertNotNull(config);

        RoutingConfig routingConfig = RoutingConfig.create();
        routingConfig.setType(RoutingConfig.TypeEnum.dht);
        config.setRouting(routingConfig);
        ipfs.saveConfig(config);

        config = ipfs.getConfig();
        RoutingConfig routing = config.getRouting();
        assertEquals(routing.getType(), RoutingConfig.TypeEnum.dht);


        routingConfig = RoutingConfig.create();
        routingConfig.setType(RoutingConfig.TypeEnum.dhtclient);
        config.setRouting(routingConfig);
        ipfs.saveConfig(config);

        result = ipfs.config_show();
        assertNotNull(result);

        config = ipfs.getConfig();
        assertNotNull(config);

        routing = config.getRouting();
        assertEquals(routing.getType(), RoutingConfig.TypeEnum.dhtclient);
    }

}
