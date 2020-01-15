package threads.ipfs;


import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

@RunWith(AndroidJUnit4.class)
public class IpfsConfigTest {


    private static Context context;

    @BeforeClass
    public static void setup() {
        context = ApplicationProvider.getApplicationContext();
    }


    @Test
    public void config_test() throws Exception {


        IPFS ipfs = TestEnv.getTestInstance(context);


        Config config = ipfs.getConfig();

        RoutingConfig routing = config.getRouting();
        assertEquals(routing.getType(), RoutingConfig.TypeEnum.dhtclient);


        SwarmConfig swarm = config.getSwarm();
        assertEquals(swarm.getConnMgr().getType(), ConnMgrConfig.TypeEnum.basic);
        assertEquals(swarm.getConnMgr().getLowWater(), 50);
        assertEquals(swarm.getConnMgr().getHighWater(), 200);
        assertEquals(swarm.getConnMgr().getGracePeriod(), "10s");
        assertTrue(swarm.isDisableBandwidthMetrics());
        assertFalse(swarm.isDisableNatPortMap());
        assertFalse(swarm.isDisableRelay());
        assertFalse(swarm.isEnableRelayHop());
        assertTrue(swarm.isEnableAutoRelay());
        assertFalse(swarm.isEnableAutoNATService());
        assertTrue(swarm.getAddrFilters().isEmpty());

        ExperimentalConfig experimental = config.getExperimental();
        assertTrue(experimental.isQUIC());
        assertTrue(experimental.isPreferTLS());

        PubsubConfig pubsub = config.getPubsub();
        assertEquals(pubsub.getRouter(), PubsubConfig.RouterEnum.gossipsub);
        assertFalse(pubsub.isDisableSigning());
        assertFalse(pubsub.isStrictSignatureVerification());

        DiscoveryConfig discovery = config.getDiscovery();
        assertTrue(discovery.getMdns().isEnabled());
        assertEquals(discovery.getMdns().getInterval(), 10);

        AddressesConfig addresses = config.getAddresses();
        assertTrue(addresses.getAPI().isEmpty());
        assertTrue(addresses.getGateway().isEmpty());
        assertTrue(!addresses.getSwarm().isEmpty());


        assertTrue(addresses.getAnnounce().isEmpty());
        assertTrue(addresses.getNoAnnounce().isEmpty());
        assertFalse(addresses.getSwarm().isEmpty());

    }
}
