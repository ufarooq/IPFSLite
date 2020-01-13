package threads.ipfs;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;

import mobile.Listener;
import mobile.Node;
import mobile.Reader;
import mobile.Stream;
import mobile.Writer;
import threads.ipfs.api.AddressesConfig;
import threads.ipfs.api.CID;
import threads.ipfs.api.Config;
import threads.ipfs.api.ConnMgrConfig;
import threads.ipfs.api.DiscoveryConfig;
import threads.ipfs.api.Encryption;
import threads.ipfs.api.ExperimentalConfig;
import threads.ipfs.api.LinkInfo;
import threads.ipfs.api.PID;
import threads.ipfs.api.Peer;
import threads.ipfs.api.PeerInfo;
import threads.ipfs.api.PubsubConfig;
import threads.ipfs.api.PubsubInfo;
import threads.ipfs.api.PubsubReader;
import threads.ipfs.api.RoutingConfig;
import threads.ipfs.api.SwarmConfig;
import threads.share.Network;

import static androidx.core.util.Preconditions.checkArgument;
import static androidx.core.util.Preconditions.checkNotNull;

public class IPFS implements Listener {
    private static final String RANDOM_SWARM_KEY = "randomSwarmKey";
    private static final String PREF_KEY = "prefKey";
    private static final String PID_KEY = "pidKey";
    private static final String SWARM_PORT_KEY = "swarmPortKey";
    private static final String MDNS_KEY = "mdnsKey";
    private static final String QUIC_KEY = "quicKey";
    private static final String PUBSUB_KEY = "pubsubKey";
    private static final String NAT_SERVICE_KEY = "noFetchKey";
    private static final String ENABLE_AUTO_RELAY_KEY = "enableAutoRelayKey";
    private static final String RELAY_HOP_KEY = "relayHopKey";
    private static final String CONN_MGR_CONFIG_TYPE_KEY = "connMgrConfigTypeKey";
    private static final String ROUTING_TYPE_KEY = "routingTypeKey";
    private static final String ROUTER_ENUM_KEY = "routerEnumKey";
    private static final String HIGH_WATER_KEY = "highWaterKey";
    private static final String LOW_WATER_KEY = "lowWaterKey";
    private static final String GRACE_PERIOD_KEY = "gracePeriodKey";
    private static final String PREFER_TLS_KEY = "preferTLSKey";
    private static final int BLOCK_SIZE = 64000; // 64kb
    private static final String P2P_CIRCUIT = "p2p-circuit";
    private static final String CONFIG_FILE_NAME = "config";
    private static final String CACHE = "cache";
    private static final long TIMEOUT = 30000L;
    private static final String TAG = IPFS.class.getSimpleName();
    private static IPFS INSTANCE = null;
    @Nullable
    private static PubsubHandler HANDLER = null;
    private final File baseDir;
    private final File cacheDir;
    private final Node node;
    private final PID pid;
    private final PubsubReader pubsubReader;
    private final ContentInfoUtil util;
    private final Hashtable<String, Future> topics = new Hashtable<>();
    private Gson gson = new Gson();


    private IPFS(@NonNull Builder builder) throws Exception {
        checkNotNull(builder);
        this.baseDir = builder.context.getFilesDir();
        checkNotNull(this.baseDir);
        checkArgument(this.baseDir.isDirectory());
        this.cacheDir = builder.context.getCacheDir();
        checkNotNull(this.cacheDir);
        checkArgument(this.cacheDir.isDirectory());
        this.util = new ContentInfoUtil(builder.context);
        this.pubsubReader = builder.pubsubReader;

        boolean init = !existConfigFile();

        node = new Node(this, this.baseDir.getAbsolutePath());

        if (init) {
            node.init();
        }

        Config config = getConfig();

        configTune(config,
                builder.addresses,
                builder.experimental,
                builder.pubsub,
                builder.discovery,
                builder.swarm,
                builder.routing);

        pid = config.getIdentity().getPID();
    }

    public static void setPID(@NonNull Context context, @NonNull PID pid) {
        checkNotNull(context);
        checkNotNull(pid);
        checkArgument(!pid.getPid().isEmpty());
        SharedPreferences sharedPref = context.getSharedPreferences(
                PREF_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(PID_KEY, pid.getPid());
        editor.apply();
    }

    @Nullable
    public static PID getPID(@NonNull Context context) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(
                PREF_KEY, Context.MODE_PRIVATE);
        String pid = sharedPref.getString(PID_KEY, "");
        if (pid.isEmpty()) {
            return null;
        }
        return PID.create(pid);
    }

    public static void deleteConfigFile(@NonNull Context context) {
        checkNotNull(context);
        try {
            File dir = context.getFilesDir();
            if (dir.exists() && dir.isDirectory()) {
                File config = new File(dir, CONFIG_FILE_NAME);
                if (config.exists()) {
                    checkArgument(config.delete());
                }
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    @Nullable
    public static PubsubHandler getPubsubHandler() {
        return HANDLER;
    }

    public static void setPubsubHandler(@Nullable PubsubHandler pubsubHandler) {
        HANDLER = pubsubHandler;
    }

    public static void setSwarmPort(@NonNull Context context, int port) {
        checkNotNull(context);
        checkArgument(port > 0);
        SharedPreferences sharedPref = context.getSharedPreferences(
                PREF_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(SWARM_PORT_KEY, port);
        editor.apply();
    }

    public static int getSwarmPort(@NonNull Context context) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(
                PREF_KEY, Context.MODE_PRIVATE);
        return sharedPref.getInt(SWARM_PORT_KEY, 4001);
    }

    public static boolean isRandomSwarmPort(@NonNull Context context) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
        return sharedPref.getBoolean(RANDOM_SWARM_KEY, false);
    }

    public static void setRandomSwarmPort(@NonNull Context context, boolean enable) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(RANDOM_SWARM_KEY, enable);
        editor.apply();
    }

    public static boolean isQUICEnabled(@NonNull Context context) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
        return sharedPref.getBoolean(QUIC_KEY, false);

    }

    public static void setQUICEnabled(@NonNull Context context, boolean enable) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(QUIC_KEY, enable);
        editor.apply();
    }

    public static boolean isPreferTLS(@NonNull Context context) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
        return sharedPref.getBoolean(PREFER_TLS_KEY, false);
    }

    public static void setPreferTLS(@NonNull Context context, boolean enable) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(PREFER_TLS_KEY, enable);
        editor.apply();
    }

    public static PubsubConfig.RouterEnum getPubsubRouter(@NonNull Context context) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(
                PREF_KEY, Context.MODE_PRIVATE);
        return PubsubConfig.RouterEnum.valueOf(
                sharedPref.getString(ROUTER_ENUM_KEY, PubsubConfig.RouterEnum.floodsub.name()));
    }

    public static void setPubsubRouter(@NonNull Context context,
                                       @NonNull PubsubConfig.RouterEnum routerEnum) {
        checkNotNull(context);
        checkNotNull(routerEnum);
        SharedPreferences sharedPref = context.getSharedPreferences(
                PREF_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(ROUTER_ENUM_KEY, routerEnum.name());
        editor.apply();
    }

    @NonNull
    public static RoutingConfig.TypeEnum getRoutingType(@NonNull Context context) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(
                PREF_KEY, Context.MODE_PRIVATE);
        return RoutingConfig.TypeEnum.valueOf(
                sharedPref.getString(ROUTING_TYPE_KEY, RoutingConfig.TypeEnum.dhtclient.name()));
    }

    public static void setRoutingType(@NonNull Context context,
                                      @NonNull RoutingConfig.TypeEnum routingType) {
        checkNotNull(context);
        checkNotNull(routingType);
        SharedPreferences sharedPref = context.getSharedPreferences(
                PREF_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(ROUTING_TYPE_KEY, routingType.name());
        editor.apply();
    }

    @NonNull
    public static ConnMgrConfig.TypeEnum getConnMgrConfigType(@NonNull Context context) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(
                PREF_KEY, Context.MODE_PRIVATE);
        return ConnMgrConfig.TypeEnum.valueOf(
                sharedPref.getString(CONN_MGR_CONFIG_TYPE_KEY, ConnMgrConfig.TypeEnum.basic.name()));
    }

    public static void setConnMgrConfigType(@NonNull Context context, ConnMgrConfig.TypeEnum typeEnum) {
        checkNotNull(context);
        checkNotNull(typeEnum);
        SharedPreferences sharedPref = context.getSharedPreferences(
                PREF_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(CONN_MGR_CONFIG_TYPE_KEY, typeEnum.name());
        editor.apply();
    }

    public static void setLowWater(@NonNull Context context, int lowWater) {
        checkNotNull(context);
        checkArgument(lowWater > 0);
        SharedPreferences sharedPref = context.getSharedPreferences(
                PREF_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(LOW_WATER_KEY, lowWater);
        editor.apply();
    }

    public static int getLowWater(@NonNull Context context) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(
                PREF_KEY, Context.MODE_PRIVATE);
        return sharedPref.getInt(LOW_WATER_KEY, 20);
    }

    public static boolean isAutoRelayEnabled(@NonNull Context context) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
        return sharedPref.getBoolean(ENABLE_AUTO_RELAY_KEY, false);

    }

    public static void setAutoRelayEnabled(@NonNull Context context, boolean enable) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(ENABLE_AUTO_RELAY_KEY, enable);
        editor.apply();
    }

    public static boolean isPubsubEnabled(@NonNull Context context) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
        return sharedPref.getBoolean(PUBSUB_KEY, true);
    }

    public static void setPubsubEnabled(@NonNull Context context, boolean enable) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(PUBSUB_KEY, enable);
        editor.apply();
    }

    public static boolean isMdnsEnabled(@NonNull Context context) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
        return sharedPref.getBoolean(MDNS_KEY, true);

    }

    public static void setMdnsEnabled(@NonNull Context context, boolean enable) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(MDNS_KEY, enable);
        editor.apply();
    }

    public static boolean isAutoNATServiceEnabled(@NonNull Context context) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(
                PREF_KEY, Context.MODE_PRIVATE);
        return sharedPref.getBoolean(NAT_SERVICE_KEY, false);
    }

    public static void setAutoNATServiceEnabled(@NonNull Context context, boolean natService) {
        checkNotNull(context);

        SharedPreferences sharedPref = context.getSharedPreferences(
                PREF_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(NAT_SERVICE_KEY, natService);
        editor.apply();
    }

    public static boolean isRelayHopEnabled(@NonNull Context context) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(
                PREF_KEY, Context.MODE_PRIVATE);
        return sharedPref.getBoolean(RELAY_HOP_KEY, false);
    }

    public static void setRelayHopEnabled(@NonNull Context context, boolean relayHop) {
        checkNotNull(context);

        SharedPreferences sharedPref = context.getSharedPreferences(
                PREF_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(RELAY_HOP_KEY, relayHop);
        editor.apply();
    }

    @NonNull
    public static String getGracePeriod(@NonNull Context context) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(
                PREF_KEY, Context.MODE_PRIVATE);
        return sharedPref.getString(GRACE_PERIOD_KEY, "30s");
    }

    public static void setGracePeriod(@NonNull Context context, @NonNull String gracePeriod) {
        checkNotNull(context);
        checkNotNull(gracePeriod);
        SharedPreferences sharedPref = context.getSharedPreferences(
                PREF_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(GRACE_PERIOD_KEY, gracePeriod);
        editor.apply();

    }

    public static void setHighWater(@NonNull Context context, int highWater) {
        checkNotNull(context);
        checkArgument(highWater > 0);
        SharedPreferences sharedPref = context.getSharedPreferences(
                PREF_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(HIGH_WATER_KEY, highWater);
        editor.apply();
    }

    public static int getHighWater(@NonNull Context context) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(
                PREF_KEY, Context.MODE_PRIVATE);
        return sharedPref.getInt(HIGH_WATER_KEY, 40);
    }

    @NonNull
    public static IPFS getInstance(@NonNull Context context) {
        if (INSTANCE == null) {
            synchronized (IPFS.class) {
                if (INSTANCE == null) {
                    PubsubReader pubsubReader = (message) -> {
                        if (HANDLER != null) {
                            HANDLER.receive(message);
                        }
                    };
                    int swarmPort = getSwarmPort(context);

                    if (isRandomSwarmPort(context)) {
                        swarmPort = Network.nextFreePort();
                    }


                    Integer quicPort = null;
                    if (isQUICEnabled(context)) {
                        quicPort = swarmPort;
                    }

                    AddressesConfig addresses = AddressesConfig.create(
                            swarmPort, quicPort);

                    ExperimentalConfig experimental = ExperimentalConfig.create();
                    experimental.setQUIC(isQUICEnabled(context));
                    experimental.setPreferTLS(isPreferTLS(context));


                    PubsubConfig pubsubConfig = PubsubConfig.create();
                    pubsubConfig.setRouter(getPubsubRouter(context));


                    SwarmConfig swarmConfig = SwarmConfig.create();
                    swarmConfig.setDisableBandwidthMetrics(true);
                    swarmConfig.setDisableNatPortMap(false);
                    swarmConfig.setDisableRelay(false);
                    swarmConfig.setEnableAutoRelay(isAutoRelayEnabled(context));
                    swarmConfig.setEnableAutoNATService(isAutoNATServiceEnabled(context));
                    swarmConfig.setEnableRelayHop(isRelayHopEnabled(context));

                    ConnMgrConfig mgr = swarmConfig.getConnMgr();
                    mgr.setGracePeriod(getGracePeriod(context));
                    mgr.setHighWater(getHighWater(context));
                    mgr.setLowWater(getLowWater(context));
                    mgr.setType(getConnMgrConfigType(context));

                    DiscoveryConfig discoveryConfig = DiscoveryConfig.create();
                    discoveryConfig.getMdns().setEnabled(isMdnsEnabled(context));


                    RoutingConfig routingConfig = RoutingConfig.create();
                    routingConfig.setType(getRoutingType(context));


                    try {
                        INSTANCE = new Builder().
                                context(context).
                                pubsubReader(pubsubReader).
                                addresses(addresses).
                                experimental(experimental).
                                pubsub(pubsubConfig).
                                discovery(discoveryConfig).
                                swarm(swarmConfig).
                                routing(routingConfig).build();

                        IPFS.setPID(context, INSTANCE.getPeerID());

                        boolean pubSubEnabled = IPFS.isPubsubEnabled(context);
                        INSTANCE.daemon(pubSubEnabled);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return INSTANCE;
    }

    public static String getDeviceName() {
        try {
            String manufacturer = Build.MANUFACTURER;
            String model = Build.MODEL;
            if (model.startsWith(manufacturer)) {
                return capitalize(model);
            }
            return capitalize(manufacturer) + " " + model;
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
        return "";
    }

    private static String capitalize(String str) {
        if (TextUtils.isEmpty(str)) {
            return str;
        }
        char[] arr = str.toCharArray();
        boolean capitalizeNext = true;
        String phrase = "";
        for (char c : arr) {
            if (capitalizeNext && Character.isLetter(c)) {
                phrase = phrase.concat("" + Character.toUpperCase(c));
                capitalizeNext = false;
                continue;
            } else if (Character.isWhitespace(c)) {
                capitalizeNext = true;
            }
            phrase = phrase.concat("" + c);
        }
        return phrase;
    }

    public boolean connectPubsubTopic(@NonNull Context context, @NonNull String topic) {
        checkNotNull(context);
        checkNotNull(topic);

        // check already listen to topic
        if (topics.containsKey(topic)) {
            return true;
        }


        ExecutorService executor = Executors.newSingleThreadExecutor();
        topics.put(topic, executor.submit(() -> {
            try {
                pubsubSub(topic, true);
            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }
        }));
        return false;
    }

    @NonNull
    public PID getPid() {
        return pid;
    }

    public boolean relay(@NonNull Peer relay, @NonNull PID pid, int timeout) {
        checkNotNull(relay);
        checkNotNull(pid);
        checkArgument(timeout > 0);
        return swarmConnect(relayAddress(relay, pid), timeout);

    }

    @NonNull
    private File getConfigFile() {
        return new File(baseDir, CONFIG_FILE_NAME);
    }

    private boolean existConfigFile() {
        return getConfigFile().exists();
    }

    @NonNull
    public String config_show() throws Exception {
        return getConfigAsString();
    }

    @NonNull
    public String getConfigAsString() throws Exception {
        checkArgument(existConfigFile(), "Config file does not exist.");

        return FileUtils.readFileToString(getConfigFile(), StandardCharsets.UTF_8);

    }

    @NonNull
    public String getPrivateKey() {
        return node.getPrivateKey();
    }

    @NonNull
    public String getPublicKey() {
        return node.getPublicKey();
    }

    public List<PID> dhtFindProvs(@NonNull CID cid, int numProvs, int timeout) {
        checkNotNull(cid);
        checkArgument(timeout > 0);
        List<PID> providers = new ArrayList<>();
        try {
            node.dhtFindProvs(cid.getCid(), (pid) ->
                            providers.add(PID.create(pid))
                    , numProvs, true, timeout);
        } catch (Throwable e) {
            evaluateException(e);
        }
        return providers;
    }

    public void dhtPublish(@NonNull CID cid, boolean recursive, int timeout) {
        checkNotNull(cid);
        checkArgument(timeout > 0);
        try {
            if (checkDaemonRunning()) {
                node.dhtProvide(cid.getCid(), recursive, true, timeout);
            }
        } catch (Throwable e) {
            evaluateException(e);
        }
    }

    @Nullable
    public PeerInfo id(@NonNull Peer peer, int timeout) {
        checkNotNull(peer);
        checkArgument(timeout > 0);
        return id(peer.getPid(), timeout);
    }

    private boolean checkDaemonRunning() {
        boolean result = isDaemonRunning();
        if (!result) {
            Log.e(TAG, "Daemon not running");
        }
        return result;
    }

    @Nullable
    public PeerInfo id(@NonNull PID pid, int timeout) {
        checkNotNull(pid);
        checkArgument(timeout > 0);
        try {
            if (checkDaemonRunning()) {
                String json = node.idWithTimeout(pid.getPid(), timeout);
                Map map = gson.fromJson(json, Map.class);
                return PeerInfo.create(map);
            }
        } catch (Throwable e) {
            evaluateException(e);
        }

        return null;

    }

    @NonNull
    public PID getPeerID() {
        checkNotNull(pid);
        return pid;
    }

    @NonNull
    public PeerInfo id() {
        try {
            String json = node.id();
            Map map = gson.fromJson(json, Map.class);
            checkNotNull(map);
            return PeerInfo.create(map);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public boolean checkRelay(@NonNull Peer peer, int timeout) {
        checkNotNull(peer);
        checkArgument(timeout > 0);
        AtomicBoolean success = new AtomicBoolean(false);
        try {
            PID pid = PID.create("QmWATWQ7fVPP2EFGu71UkfnqhYXDYH566qy47CnJDgvs8u"); // DUMMY

            String address = relayAddress(peer, pid);
            node.swarmConnect(address, timeout);

        } catch (Throwable e) {
            String line = e.getLocalizedMessage();
            if (line != null) {
                if (line.contains("HOP_NO_CONN_TO_DST")) {
                    success.set(true);
                }
            }
        }
        return success.get();
    }

    public void pubsubPub(@NonNull final String topic, @NonNull final String message, int timeout) {
        checkNotNull(topic);
        checkNotNull(message);
        checkArgument(timeout > 0);
        if (checkDaemonRunning()) {
            try {
                byte[] data = Base64.encodeBase64(message.getBytes());
                node.pubsubPub(topic, data);
                Thread.sleep(timeout);
            } catch (Throwable e) {
                evaluateException(e);
            }

        }
    }

    public void logs() throws Exception {
        node.logs();
    }

    public void pubsubSub(@NonNull String topic, boolean discover) throws Exception {
        checkNotNull(topic);

        if (checkDaemonRunning()) {
            node.pubsubSub(topic, discover);
        }

    }

    @NonNull
    public String relayAddress(@NonNull Peer relay, @NonNull PID pid) {
        checkNotNull(relay);
        checkNotNull(pid);
        return relayAddress(relay.getMultiAddress(), relay.getPid(), pid);
    }

    @NonNull
    public String relayAddress(@NonNull String address, @NonNull PID relay, @NonNull PID pid) {
        checkNotNull(address);
        checkNotNull(relay);
        checkNotNull(pid);
        return address + "/" + Style.p2p.name() + "/" + relay.getPid() +
                "/" + P2P_CIRCUIT + "/" + Style.p2p.name() + "/" + pid.getPid();
    }

    public boolean specificRelay(@NonNull PID relay,
                                 @NonNull PID pid,
                                 int timeout) {
        checkNotNull(relay);
        checkNotNull(pid);
        checkArgument(timeout > 0);
        boolean result = false;

        if (swarmConnect(relay, timeout)) {
            Peer peer = swarmPeer(relay);
            if (peer != null) {
                return swarmConnect(relayAddress(peer, pid), timeout);
            }
        }

        return result;
    }

    public boolean relay(@NonNull PID relay, @NonNull PID pid, int timeout) {
        checkNotNull(relay);
        checkNotNull(pid);
        checkArgument(timeout > 0);
        return specificRelay(relay, pid, timeout);

    }

    public boolean blockStat(@NonNull CID cid, boolean offline) {
        checkNotNull(cid);
        try {
            if (checkDaemonRunning()) {
                String res = node.blockStat(cid.getCid(), offline);
                if (res != null && !res.isEmpty()) {
                    return true;
                }
            }
        } catch (Throwable e) {
            evaluateException(e);
        }
        return false;
    }

    public void blockRm(@NonNull CID cid) {
        checkNotNull(cid);
        try {
            if (checkDaemonRunning()) {
                node.blockRm(cid.getCid());
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    public boolean arbitraryRelay(@NonNull PID pid, int timeout) {
        checkNotNull(pid);
        checkArgument(timeout > 0);
        return swarmConnect("/" + P2P_CIRCUIT + "/" +
                Style.p2p.name() + "/" + pid.getPid(), timeout);
    }

    public boolean swarmConnect(@NonNull PID pid, int timeout) {
        checkNotNull(pid);
        checkArgument(timeout > 0);
        return swarmConnect("/" + Style.p2p.name() + "/" +
                pid.getPid(), timeout);
    }

    public boolean swarmConnect(@NonNull Peer peer, int timeout) {
        checkNotNull(peer);
        checkArgument(timeout > 0);
        String ma = peer.getMultiAddress() + "/" + Style.p2p.name() + "/" + peer.getPid();
        return swarmConnect(ma, timeout);

    }

    private void evaluateException(@NonNull Throwable e) {
        String msg = "" + e.getLocalizedMessage();
        if (!msg.startsWith("context deadline exceeded")) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    public boolean swarmConnect(@NonNull String multiAddress, int timeout) {
        checkNotNull(multiAddress);
        checkArgument(timeout > 0);
        try {
            if (checkDaemonRunning()) {
                return node.swarmConnect(multiAddress, timeout);
            }
        } catch (Throwable e) {
            evaluateException(e);
        }
        return false;
    }

    public boolean unProtectPeer(@NonNull PID pid, @NonNull String tag) {
        checkNotNull(pid);
        checkNotNull(tag);
        try {
            return node.unProtectPeer(pid.getPid(), tag);
        } catch (Throwable e) {
            evaluateException(e);
        }
        return false;
    }

    public void protectPeer(@NonNull PID pid, @NonNull String tag) {
        checkNotNull(pid);
        checkNotNull(tag);
        try {
            node.protectPeer(pid.getPid(), tag);
        } catch (Throwable e) {
            evaluateException(e);
        }
    }

    public boolean isConnected(@NonNull PID pid) {
        checkNotNull(pid);
        try {
            return node.isConnected(pid.getPid());
        } catch (Throwable e) {
            evaluateException(e);
        }
        return false;
    }

    @Nullable
    public Peer swarmPeer(@NonNull PID pid) {
        checkNotNull(pid);

        try {
            String json = node.swarmPeer(pid.getPid());
            if (json != null && !json.isEmpty()) {
                Map map = gson.fromJson(json, Map.class);
                if (map != null) {
                    return Peer.create(map);
                }
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
        return null;
    }

    @NonNull
    public List<Peer> swarmPeers() {
        List<Peer> peers = swarm_peers();
        peers.sort(Peer::compareTo);
        return peers;
    }

    @NonNull
    private List<Peer> swarm_peers() {

        List<Peer> peers = new ArrayList<>();
        try {
            if (checkDaemonRunning()) {

                String json = node.swarmPeers();
                if (json != null && !json.isEmpty()) {
                    Map map = gson.fromJson(json, Map.class);
                    if (map != null) {

                        Object object = map.get("Peers");
                        if (object instanceof List) {
                            List list = (List) object;
                            if (!list.isEmpty()) {
                                for (Object entry : list) {
                                    if (entry instanceof Map) {
                                        Map peer = (Map) entry;
                                        peers.add(Peer.create(peer));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
        return peers;
    }

    public void logBaseDir() {
        try {
            File[] files = baseDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    Log.e(TAG, "" + file.length() + " " + file.getAbsolutePath());
                    if (file.isDirectory()) {
                        File[] children = file.listFiles();
                        if (children != null) {
                            for (File child : children) {
                                Log.e(TAG, "" + child.length() + " " + child.getAbsolutePath());
                            }
                        }
                    }
                }
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    public void cleanCacheDir() {
        try {
            File[] files = getCacheDir().listFiles();
            if (files != null) {
                for (File file : files) {
                    checkArgument(file.delete());
                }
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    public boolean swarmDisconnect(@NonNull PID pid) {
        checkNotNull(pid);
        if (checkDaemonRunning()) {
            try {
                return node.swarmDisconnect("/" + Style.p2p.name() + "/" + pid.getPid());
            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }
        }
        return false;
    }

    public void rm(@NonNull CID cid) {
        checkNotNull(cid);
        try {
            if (checkDaemonRunning()) {
                node.rm(cid.getCid());
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    @NonNull
    public List<String> pubsubPeers() {
        List<String> peers = new ArrayList<>();
        try {
            if (checkDaemonRunning()) {
                node.pubsubPeers((peer) -> peers.add(peer));
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
        return peers;
    }

    @NonNull
    public List<PID> pubsubPeers(@NonNull String topic) {
        checkNotNull(topic);
        List<PID> pidList = new ArrayList<>();

        try {
            if (checkDaemonRunning()) {
                node.pubsubPeers((peer) -> pidList.add(PID.create(peer)));
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
        return pidList;
    }

    @NonNull
    public Config getConfig() throws Exception {
        String result = getConfigAsString();
        Gson gson = new Gson();
        return gson.fromJson(result, Config.class);
    }

    public void saveConfig(@NonNull Config config) throws Exception {
        checkNotNull(config);

        File file = getConfigFile();

        try (FileWriter writer = new FileWriter(file)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(config, writer);
        }

    }

    private void configTune(@NonNull Config config,
                            @Nullable AddressesConfig addresses,
                            @Nullable ExperimentalConfig experimental,
                            @Nullable PubsubConfig pubsub,
                            @Nullable DiscoveryConfig discovery,
                            @Nullable SwarmConfig swarm,
                            @Nullable RoutingConfig routing) throws Exception {
        checkNotNull(config);
        checkArgument(existConfigFile(), "Config file does not exist.");


        boolean changedConfig = false;
        if (experimental != null) {
            changedConfig = true;
            config.setExperimental(experimental);
        }
        if (pubsub != null) {
            changedConfig = true;
            config.setPubsub(pubsub);
        }
        if (addresses != null) {
            changedConfig = true;
            config.setAddresses(addresses);
        }
        if (discovery != null) {
            changedConfig = true;
            config.setDiscovery(discovery);
        }
        if (swarm != null) {
            changedConfig = true;
            config.setSwarm(swarm);
        }
        if (routing != null) {
            changedConfig = true;
            config.setRouting(routing);
        }
        if (changedConfig) {
            saveConfig(config);
        }
    }

    private void daemon(boolean pubsub) throws Exception {

        AtomicBoolean failure = new AtomicBoolean(false);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        AtomicReference<String> exception = new AtomicReference<>("");
        executor.submit(() -> {
            try {
                String topic = pid.getPid();
                node.daemon(topic, pubsub);
            } catch (Throwable e) {
                failure.set(true);
                exception.set("" + e.getLocalizedMessage());
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }
        });
        long time = 0L;
        while (!node.getRunning() && time <= TIMEOUT && !failure.get()) {
            time = time + 100L;
            Thread.sleep(100);
        }
        if (failure.get()) {
            throw new RuntimeException(exception.get());
        }
    }

    @Nullable
    public CID storeData(@NonNull byte[] data) throws Exception {

        checkNotNull(data);
        return storeData(data, true);
    }

    @Nullable
    public CID storeText(@NonNull String text, @NonNull String key) throws Exception {
        checkNotNull(text);
        checkNotNull(key);
        return storeText(text, key, true);
    }

    @Nullable
    private CID storeStream(@NonNull InputStream inputStream) throws Exception {
        checkNotNull(inputStream);

        return storeStream(inputStream, true);
    }

    @Nullable
    public CID storeData(@NonNull byte[] data, boolean offline) throws Exception {
        checkNotNull(data);
        checkArgument(data.length > 0);
        try (InputStream inputStream = new ByteArrayInputStream(data)) {
            return storeStream(inputStream, offline);
        }
    }

    @Nullable
    public CID storeText(@NonNull String content, @NonNull String key, boolean offline) throws Exception {
        checkNotNull(key);
        checkNotNull(content);
        checkArgument(!content.isEmpty());
        try (InputStream inputStream = new ByteArrayInputStream(content.getBytes())) {
            return storeInputStream(inputStream, key, offline);
        }
    }

    @Nullable
    public List<LinkInfo> ls(@NonNull CID cid, int timeout, boolean offline) {
        checkNotNull(cid);
        checkArgument(timeout > 0);


        List<LinkInfo> infos = new ArrayList<>();
        try {
            if (checkDaemonRunning()) {
                node.ls(cid.getCid(), (json) -> {

                    try {
                        Map map = gson.fromJson(json, Map.class);
                        checkNotNull(map);
                        LinkInfo info = LinkInfo.create(map);
                        infos.add(info);
                    } catch (Throwable e) {
                        evaluateException(e);
                    }

                }, timeout, offline);

            }
        } catch (Throwable e) {
            evaluateException(e);
            return null;
        }
        return infos;
    }

    @Nullable
    private CID stream(@NonNull InputStream inputStream, boolean offline) {
        checkNotNull(inputStream);


        String res = "";
        try {
            Writer writer = getWriter(offline);
            res = writer.stream(new WriterStream(writer, inputStream));
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }

        if (!res.isEmpty()) {
            return CID.create(res);
        }
        return null;
    }

    @Nullable
    public CID streamFile(@NonNull File target, boolean offline) {
        checkNotNull(target);


        try {
            return stream(new FileInputStream(target), offline);
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
        return null;
    }

    @NonNull
    public Reader getReader(@NonNull CID cid, boolean offline) throws Exception {
        return node.getReader(cid.getCid(), offline);
    }

    @NonNull
    public Writer getWriter(boolean offline) throws Exception {
        return node.getWriter(offline);
    }

    private boolean stream(@NonNull OutputStream outputStream,
                           @NonNull Progress progress,
                           @NonNull CID cid,
                           @NonNull String key,
                           boolean offline,
                           int timeout,
                           long size) {
        checkNotNull(outputStream);
        checkNotNull(progress);
        checkNotNull(cid);
        checkNotNull(key);
        checkArgument(timeout > 0);

        if (offline && key.isEmpty()) {
            return streamFast(outputStream, progress, cid);
        } else {
            return streamSlow(outputStream, progress, cid, key, offline, timeout, size);
        }
    }

    private boolean streamSlow(@NonNull OutputStream outputStream,
                               @NonNull Progress progress,
                               @NonNull CID cid,
                               @NonNull String key,
                               boolean offline,
                               int timeout,
                               long size) {
        checkNotNull(outputStream);
        checkNotNull(progress);
        checkNotNull(cid);
        checkNotNull(key);
        checkArgument(timeout > 0);
        final AtomicInteger atomicProgress = new AtomicInteger(0);
        int totalRead = 0;
        try (InputStream inputStream = stream(cid, key, timeout, offline)) {

            byte[] data = new byte[BLOCK_SIZE];
            progress.setProgress(0);
            int bytesRead = 1;


            // Read data with timeout
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Callable<Integer> readTask = () ->
                    inputStream.read(data);
            while (bytesRead >= 0) {
                Future<Integer> future = executor.submit(readTask);
                bytesRead = future.get(timeout, TimeUnit.SECONDS);
                if (bytesRead >= 0) {
                    outputStream.write(data, 0, bytesRead);
                    totalRead += bytesRead;
                    if (size > 0) {
                        int percent = (int) ((totalRead * 100.0f) / size);
                        if (atomicProgress.getAndSet(percent) < percent) {
                            progress.setProgress(percent);
                        }
                    }
                }

            }

        } catch (Throwable e) {
            return false;
        }
        if (size > 0) {
            return (totalRead == size);
        }
        return true;

    }

    private boolean streamFast(@NonNull OutputStream outputStream,
                               @NonNull Progress progress,
                               @NonNull CID cid) {
        checkNotNull(outputStream);
        checkNotNull(progress);
        checkNotNull(cid);

        int totalRead = 0;
        try {
            Reader fileReader = getReader(cid, true);
            long size = fileReader.getSize();
            final AtomicInteger atomicProgress = new AtomicInteger(0);

            try {
                progress.setProgress(0);
                fileReader.load(BLOCK_SIZE);

                long bytesRead = fileReader.getRead();


                while (bytesRead > 0) {
                    outputStream.write(fileReader.getData(), 0, (int) bytesRead);
                    totalRead += bytesRead;
                    if (size > 0) {
                        int percent = (int) ((totalRead * 100.0f) / size);
                        if (atomicProgress.getAndSet(percent) < percent) {
                            progress.setProgress(percent);
                        }
                    }
                    fileReader.load(BLOCK_SIZE);
                    bytesRead = fileReader.getRead();
                }
            } finally {
                fileReader.close();
            }

        } catch (Throwable e) {
            return false;
        }

        return true;
    }

    public boolean storeToFile(@NonNull File file,
                               @NonNull CID cid,
                               @NonNull Progress progress,
                               boolean offline, int timeout,
                               long size) {
        checkNotNull(file);
        checkNotNull(cid);
        checkNotNull(progress);
        checkArgument(timeout > 0);

        // make sure file path exists
        try {
            if (!file.exists()) {
                File parent = file.getParentFile();
                checkNotNull(parent);
                if (!parent.exists()) {
                    checkArgument(parent.mkdirs());
                }
                checkArgument(file.createNewFile());
            }
        } catch (Throwable e) {
            evaluateException(e);
            return false;
        }

        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            return stream(outputStream, progress, cid, "", offline, timeout, size);
        } catch (Throwable e) {
            evaluateException(e);
            return false;
        }
    }


    public void storeToOutputStream(@NonNull CID cid, @NonNull OutputStream os) throws Exception {
        checkNotNull(cid);
        checkNotNull(os);
        Reader reader = getReader(cid, true);
        try {
            int size = 262158;
            reader.load(size);
            long read = reader.getRead();
            while (read > 0) {
                byte[] bytes = reader.getData();
                os.write(bytes, 0, bytes.length);

                reader.load(size);
                read = reader.getRead();
            }
        } finally {
            reader.close();
        }

    }

    @NonNull
    private InputStream stream(@NonNull CID cid, int timeout, boolean offline) throws Exception {
        checkArgument(timeout > 0);

        PipedOutputStream pos = new PipedOutputStream();
        PipedInputStream pis = new PipedInputStream(pos);

        List<LinkInfo> info = ls(cid, timeout, offline);
        if (info == null) {
            throw new TimeoutException("timeout " + timeout + " [s]");
        }

        final AtomicBoolean close = new AtomicBoolean(false);
        new Thread(() -> {
            try {

                node.getStream(cid.getCid(), new Stream() {
                    @Override
                    public boolean close() {
                        return close.get();
                    }


                    @Override
                    public long read(byte[] bytes) {
                        try {
                            pos.write(bytes);
                        } catch (Throwable e) {
                            close.set(true);
                            // Ignore exception might be on pipe is closed
                            Log.e(TAG, "" + e.getLocalizedMessage(), e);
                        }
                        return -1;
                    }

                }, offline);

            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            } finally {
                try {
                    pos.close();
                } catch (Throwable e) {
                    // Ignore exception might be on pipe is closed
                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
                }
            }
        }).start();


        return pis;


    }

    public void storeToFile(@NonNull File file, @NonNull CID cid) {
        checkNotNull(file);
        checkNotNull(cid);

        if (!checkDaemonRunning()) {
            return;
        }

        // make sure file path exists
        try {
            if (!file.exists()) {
                File parent = file.getParentFile();
                checkNotNull(parent);
                if (!parent.exists()) {
                    checkArgument(parent.mkdirs());
                }
                checkArgument(file.createNewFile());
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

        try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            stream(fileOutputStream, cid);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

    }

    private void stream(@NonNull OutputStream outputStream, @NonNull CID cid) {
        checkNotNull(outputStream);
        checkNotNull(cid);

        int blockSize = 4096;
        try {
            Reader fileReader = getReader(cid, true);

            try {

                fileReader.load(blockSize);

                long bytesRead = fileReader.getRead();


                while (bytesRead > 0) {

                    outputStream.write(fileReader.getData(), 0, (int) bytesRead);

                    fileReader.load(blockSize);
                    bytesRead = fileReader.getRead();
                }
            } finally {
                fileReader.close();
            }

        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

    }

    @Nullable
    public ContentInfo getContentInfo(@NonNull CID cid, @NonNull String key,
                                      int timeout, boolean offline) {
        checkNotNull(cid);
        checkNotNull(key);
        checkArgument(timeout > 0);
        try {
            try (InputStream inputStream = new BufferedInputStream(
                    stream(cid, key, timeout, offline))) {

                return util.findMatch(inputStream);

            }
        } catch (Throwable e) {
            evaluateException(e);
        }
        return null;
    }

    @Nullable
    public ContentInfo getContentInfo(@NonNull File file) {
        checkNotNull(file);

        try {
            return util.findMatch(file);
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
        return null;
    }

    @NonNull
    public File getCacheDir() {
        File cacheDir = new File(this.cacheDir, CACHE);
        if (!cacheDir.exists()) {
            checkArgument(cacheDir.mkdir());
        }
        return cacheDir;
    }

    @NonNull
    public File getTempCacheFile() throws IOException {
        return File.createTempFile("temp", ".cid", getCacheDir());
    }

    @Nullable
    public CID storeStream(@NonNull InputStream inputStream, boolean offline) throws Exception {
        checkNotNull(inputStream);

        if (!checkDaemonRunning()) {
            return null;
        }

        return stream(inputStream, offline);
    }

    @Nullable
    private CID storeInputStream(@NonNull InputStream inputStream,
                                 @NonNull String key,
                                 boolean offline) throws Exception {
        checkNotNull(inputStream);
        checkNotNull(key);

        if (!checkDaemonRunning()) {
            return null;
        }


        if (!key.isEmpty()) {
            Key aesKey = Encryption.getKey(key);
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, aesKey);
            CipherInputStream cipherStream = new CipherInputStream(inputStream, cipher);
            return stream(cipherStream, offline);
        } else {
            return stream(inputStream, offline);
        }

    }

    @Nullable
    public String getText(@NonNull CID cid, @NonNull String key, int timeout, boolean offline) {
        checkNotNull(cid);
        checkNotNull(key);
        checkArgument(timeout > 0);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            boolean success = stream(outputStream, new Progress() {
                @Override
                public void setProgress(int percent) {

                }
            }, cid, key, offline, timeout, -1);
            if (success) {
                return new String(outputStream.toByteArray());
            } else {
                return null;
            }
        } catch (Throwable e) {
            evaluateException(e);
            return null;
        }
    }

    @Nullable
    public byte[] getData(@NonNull CID cid, int timeout, boolean offline) {
        checkNotNull(cid);
        checkArgument(timeout > 0);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            boolean success = stream(outputStream, new Progress() {
                @Override
                public void setProgress(int percent) {

                }
            }, cid, "", offline, timeout, -1);
            if (success) {
                return outputStream.toByteArray();
            } else {
                return null;
            }
        } catch (Throwable e) {
            evaluateException(e);
            return null;
        }

    }

    @NonNull
    public InputStream getStream(@NonNull CID cid, int timeout, boolean offline) throws Exception {
        checkNotNull(cid);
        checkArgument(timeout > 0);
        return stream(cid, "", timeout, offline);
    }

    @NonNull
    private InputStream stream(@NonNull CID cid, @NonNull String key, int timeout,
                               boolean offline) throws Exception {
        checkNotNull(cid);
        checkArgument(timeout > 0);

        if (key.isEmpty()) {
            return stream(cid, timeout, offline);
        } else {
            Key aesKey = Encryption.getKey(key);
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, aesKey);
            return new CipherInputStream(stream(cid, timeout, offline), cipher);
        }
    }

    @NonNull
    public String version() throws Exception {
        String json = node.version();
        Map map = gson.fromJson(json, Map.class);
        checkNotNull(map);
        String version = (String) map.get("Version");
        checkNotNull(version);
        return version;
    }

    public boolean swarmDisconnect(@NonNull Peer peer) {
        checkNotNull(peer);
        return swarmDisconnect(peer.getPid());
    }

    public void gc() {
        if (checkDaemonRunning()) {
            try {
                node.repoGC();
            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }
        }
    }

    public boolean isDaemonRunning() {
        return node.getRunning();
    }

    @Override
    public void error(String message) {
        if (message != null && !message.isEmpty()) {
            Log.e(TAG, "" + message);
        }
    }

    @Override
    public void info(String message) {
        if (message != null && !message.isEmpty()) {
            Log.i(TAG, "" + message);
        }
    }

    @Override
    public void log(String message) {
        if (message != null && !message.isEmpty()) {
            Log.d(TAG, "" + message);
        }
    }

    @Override
    public void pubsub(final String message, final byte[] data) {

        try {
            PubsubInfo pubsub = PubsubInfo.create(message, data);

            if (pubsubReader != null) {
                pubsubReader.receive(pubsub);
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }

    }

    @Override
    public void verbose(String s) {
        Log.i(TAG, "" + s);
    }

    public enum Style {
        ipfs, ipns, p2p
    }

    public interface PubsubHandler {
        void receive(@NonNull PubsubInfo message);
    }

    public interface Progress {
        /**
         * Setter for percent
         *
         * @param percent Value between 0-100 percent
         */
        void setProgress(int percent);
    }

    private static class Builder {

        private Context context;
        private AddressesConfig addresses = null;
        private ExperimentalConfig experimental = null;
        private PubsubConfig pubsub = null;
        private DiscoveryConfig discovery = null;
        private SwarmConfig swarm = null;
        private RoutingConfig routing = null;
        private PubsubReader pubsubReader = null;

        IPFS build() throws Exception {
            checkNotNull(context);
            return new IPFS(this);
        }


        Builder context(@NonNull Context context) {
            checkNotNull(context);
            this.context = context;
            return this;
        }

        Builder pubsubReader(@NonNull PubsubReader pubsubReader) {
            this.pubsubReader = pubsubReader;
            return this;
        }


        Builder experimental(@Nullable ExperimentalConfig experimental) {
            this.experimental = experimental;
            return this;
        }

        Builder addresses(@Nullable AddressesConfig addresses) {
            this.addresses = addresses;
            return this;
        }

        Builder pubsub(@Nullable PubsubConfig pubsub) {
            this.pubsub = pubsub;
            return this;
        }

        Builder discovery(@Nullable DiscoveryConfig discovery) {
            this.discovery = discovery;
            return this;
        }

        Builder swarm(@Nullable SwarmConfig swarm) {
            this.swarm = swarm;
            return this;
        }

        Builder routing(@Nullable RoutingConfig routing) {
            this.routing = routing;
            return this;
        }

    }

    private class WriterStream implements mobile.WriterStream {
        public final InputStream mInputStream;
        private final Writer mWriter;

        public WriterStream(Writer writer, InputStream inputStream) {
            this.mWriter = writer;
            this.mInputStream = inputStream;
        }


        @Override
        public void load(long size) {
            try {
                byte[] data = new byte[(int) size];

                int read = mInputStream.read(data);
                mWriter.setWritten(read);
                mWriter.setData(data);
            } catch (Throwable e) {
                // TODO maybe close
                Log.e(TAG, "" + e.getLocalizedMessage());
            }

        }
    }
}
