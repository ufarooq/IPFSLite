package threads.core;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Room;

import java.util.Hashtable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import threads.core.api.EventsDatabase;
import threads.core.api.Message;
import threads.core.api.MessageKind;
import threads.core.api.PeersDatabase;
import threads.core.api.PeersInfoDatabase;
import threads.core.api.ThreadsDatabase;
import threads.iota.EntityService;
import threads.iota.IOTA;
import threads.ipfs.IPFS;
import threads.ipfs.api.AddressesConfig;
import threads.ipfs.api.ConnMgrConfig;
import threads.ipfs.api.DiscoveryConfig;
import threads.ipfs.api.ExperimentalConfig;
import threads.ipfs.api.PubsubConfig;
import threads.ipfs.api.PubsubInfo;
import threads.ipfs.api.PubsubReader;
import threads.ipfs.api.RoutingConfig;
import threads.ipfs.api.SwarmConfig;

import static androidx.core.util.Preconditions.checkNotNull;


public class Singleton {
    private static final String TAG = Singleton.class.getSimpleName();

    private static Singleton SINGLETON = null;
    private final IOTA iota;
    private final ThreadsDatabase threadsDatabase;
    private final EventsDatabase eventsDatabase;
    private final PeersInfoDatabase peersInfoDatabase;
    private final Hashtable<String, Future> topics = new Hashtable<>();
    private final PeersDatabase peersDatabase;
    private final THREADS threads;
    private final EntityService entityService;
    private final ConsoleListener consoleListener = new ConsoleListener();
    private final PubsubReader pubsubReader;

    @Nullable
    private IPFS ipfs = null;
    @Nullable
    private PubsubHandler pubsubHandler;

    private Singleton(@NonNull Context context) {
        checkNotNull(context);

        // TODO bug allow on main thread
        threadsDatabase = Room.databaseBuilder(context,
                ThreadsDatabase.class,
                ThreadsDatabase.class.getSimpleName()).allowMainThreadQueries().fallbackToDestructiveMigration().build();

        eventsDatabase =
                Room.inMemoryDatabaseBuilder(context, EventsDatabase.class).build();

        peersInfoDatabase =
                Room.inMemoryDatabaseBuilder(context, PeersInfoDatabase.class).build();

        peersDatabase = Room.databaseBuilder(context, PeersDatabase.class,
                PeersDatabase.class.getSimpleName()).fallbackToDestructiveMigration().build();

        entityService = EntityService.getInstance(context);

        threads = THREADS.createThreads(
                threadsDatabase, eventsDatabase, peersInfoDatabase, peersDatabase);


        IOTA.Builder iotaBuilder = new IOTA.Builder();
        iotaBuilder.protocol(EntityService.getTangleProtocol(context));
        iotaBuilder.host(EntityService.getTangleHost(context));
        iotaBuilder.port(EntityService.getTanglePort(context));
        iotaBuilder.timeout(EntityService.getTangleTimeout(context));
        iota = iotaBuilder.build();


        pubsubReader = (message) -> {
            if (pubsubHandler != null) {
                pubsubHandler.receive(message);
            }
        };

        int swarmPort = Preferences.getSwarmPort(context);

        if (Preferences.isRandomSwarmPort(context)) {
            swarmPort = Network.nextFreePort();
        }


        Integer quicPort = null;
        if (Preferences.isQUICEnabled(context)) {
            quicPort = swarmPort;
        }

        AddressesConfig addresses = AddressesConfig.create(
                swarmPort, quicPort);

        ExperimentalConfig experimental = ExperimentalConfig.create();
        experimental.setQUIC(Preferences.isQUICEnabled(context));
        experimental.setPreferTLS(Preferences.isPreferTLS(context));


        PubsubConfig pubsubConfig = PubsubConfig.create();
        pubsubConfig.setRouter(Preferences.getPubsubRouter(context));


        SwarmConfig swarmConfig = SwarmConfig.create();
        swarmConfig.setDisableBandwidthMetrics(true);
        swarmConfig.setDisableNatPortMap(false);
        swarmConfig.setDisableRelay(false);
        swarmConfig.setEnableAutoRelay(Preferences.isAutoRelayEnabled(context));
        swarmConfig.setEnableAutoNATService(Preferences.isAutoNATServiceEnabled(context));
        swarmConfig.setEnableRelayHop(Preferences.isRelayHopEnabled(context));

        ConnMgrConfig mgr = swarmConfig.getConnMgr();
        mgr.setGracePeriod(Preferences.getGracePeriod(context));
        mgr.setHighWater(Preferences.getHighWater(context));
        mgr.setLowWater(Preferences.getLowWater(context));
        mgr.setType(Preferences.getConnMgrConfigType(context));

        DiscoveryConfig discoveryConfig = DiscoveryConfig.create();
        discoveryConfig.getMdns().setEnabled(Preferences.isMdnsEnabled(context));


        RoutingConfig routingConfig = RoutingConfig.create();
        routingConfig.setType(Preferences.getRoutingType(context));

        try {
            ipfs = IPFS.getInstance(context, pubsubReader, addresses, experimental,
                    pubsubConfig, discoveryConfig, swarmConfig, routingConfig);

            Preferences.setPID(context, ipfs.getPeerID());
        } catch (Throwable e) {
            Preferences.evaluateException(threads, Preferences.IPFS_INSTALL_FAILURE, e);
        }

    }

    @NonNull
    public static Singleton getInstance(@NonNull Context context) {
        checkNotNull(context);
        if (SINGLETON == null) {
            SINGLETON = new Singleton(context);
        }
        return SINGLETON;
    }

    @Nullable
    public PubsubHandler getPubsubHandler() {
        return pubsubHandler;
    }

    public void setPubsubHandler(@Nullable PubsubHandler pubsubHandler) {
        this.pubsubHandler = pubsubHandler;
    }

    public boolean connectPubsubTopic(@NonNull Context context, @NonNull String topic) {
        checkNotNull(context);
        checkNotNull(topic);

        // check already listen to topic
        if (topics.containsKey(topic)) {
            return true;
        }

        final IPFS ipfs = Singleton.getInstance(context).getIpfs();
        checkNotNull(ipfs, "IPFS not valid");

        ExecutorService executor = Executors.newSingleThreadExecutor();
        topics.put(topic, executor.submit(() -> {
            try {
                ipfs.pubsubSub(topic, true);
            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }
        }));
        return false;
    }

    public PeersDatabase getPeersDatabase() {
        return peersDatabase;
    }

    @NonNull
    public PeersInfoDatabase getPeersInfoDatabase() {
        return peersInfoDatabase;
    }

    @NonNull
    public ConsoleListener getConsoleListener() {
        return consoleListener;
    }

    @NonNull
    public IOTA getIota() {
        return iota;
    }

    @Nullable
    public IPFS getIpfs() {
        return ipfs;
    }

    @NonNull
    public EventsDatabase getEventsDatabase() {
        return eventsDatabase;
    }

    @NonNull
    public THREADS getThreads() {
        return threads;
    }

    @NonNull
    public ThreadsDatabase getThreadsDatabase() {
        return threadsDatabase;
    }

    @NonNull
    public EntityService getEntityService() {
        return entityService;
    }


    public interface PubsubHandler {
        void receive(@NonNull PubsubInfo message);
    }

    public class ConsoleListener {


        public void debug(@NonNull String message) {

            long timestamp = System.currentTimeMillis();
            new Thread(() -> {
                Message em = threads.createMessage(MessageKind.DEBUG, message, timestamp);
                threads.storeMessage(em);
            }).start();

        }


        public void info(@NonNull String message) {

            long timestamp = System.currentTimeMillis();
            new Thread(() -> {
                Message em = threads.createMessage(MessageKind.INFO, message, timestamp);
                threads.storeMessage(em);
            }).start();

        }


        public void error(@NonNull String message) {

            long timestamp = System.currentTimeMillis();
            new Thread(() -> {
                Message em = threads.createMessage(MessageKind.ERROR, message, timestamp);
                threads.storeMessage(em);
            }).start();

        }

    }

}
