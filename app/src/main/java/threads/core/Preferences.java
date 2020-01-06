package threads.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.ByteArrayOutputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;

import threads.core.events.EVENTS;
import threads.ipfs.IPFS;
import threads.ipfs.api.CID;
import threads.ipfs.api.ConnMgrConfig;
import threads.ipfs.api.PID;
import threads.ipfs.api.PubsubConfig;
import threads.ipfs.api.RoutingConfig;

import static androidx.core.util.Preconditions.checkArgument;
import static androidx.core.util.Preconditions.checkNotNull;

public class Preferences {


    public static final String EXCEPTION = "EXCEPTION";
    public static final String IPFS_START_FAILURE = "IPFS_START_FAILURE";
    public static final String IPFS_INSTALL_FAILURE = "IPFS_INSTALL_FAILURE";
    public static final String WARNING = "WARNING";
    public static final String INFO = "INFO";
    private static final String PREF_KEY = "prefKey";
    private static final String PID_KEY = "pidKey";
    private static final String TOKEN_KEY = "tokenKey";
    private static final String LOGIN_FLAG_KEY = "loginFlagKey";
    private static final String SWARM_PORT_KEY = "swarmPortKey";
    private static final String TOPIC_KEY = "prefTopicKey";
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
    private static final int QR_CODE_SIZE = 250;

    private static final String TIMEOUT_KEY = "timeoutKey";
    private static final String SWARM_TIMEOUT_KEY = "swarmTimeoutKey";
    private static final String AUDIO_CODEC_KEY = "audioCodecKey";
    private static final String VIDEO_CODEC_KEY = "videoCodecKey";
    private static final String AUDIO_PROCESSING_KEY = "audioProcessingEnabledKey";
    private static final String OPEN_SL_ES_KEY = "openSlEsKey";
    private static final String PREFER_TLS_KEY = "preferTLSKey";
    private static final String AEC_KEY = "aecKey";
    private static final String AGC_KEY = "agcKey";
    private static final String HNS_KEY = "hnsKey";

    private static final String RANDOM_SWARM_KEY = "randomSwarmKey";
    private static final String DEBUG_MODE_KEY = "debugModeKey";
    private static final String REPORT_MODE_KEY = "reportModeKey";
    private static final String MDNS_KEY = "mdnsKey";


    @NonNull
    private final static Hashtable<String, CID> BITMAP_HASH_TABLE = new Hashtable<>();

    @NonNull
    public static String getToken(@NonNull Context context) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(
                PREF_KEY, Context.MODE_PRIVATE);
        return sharedPref.getString(TOKEN_KEY, "");
    }

    public static void setToken(@NonNull Context context, @NonNull String token) {
        checkNotNull(context);
        checkNotNull(token);
        SharedPreferences sharedPref = context.getSharedPreferences(
                PREF_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(TOKEN_KEY, token);
        editor.apply();
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

    @NonNull
    public static String getAudioCodec(@NonNull Context context) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(
                PREF_KEY, Context.MODE_PRIVATE);
        return sharedPref.getString(AUDIO_CODEC_KEY, "opus");
    }

    public static void setAudioCodec(@NonNull Context context, @NonNull String codec) {
        checkNotNull(context);
        checkNotNull(codec);
        SharedPreferences sharedPref = context.getSharedPreferences(
                PREF_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(AUDIO_CODEC_KEY, codec);
        editor.apply();
    }

    @NonNull
    public static String getVideoCodec(@NonNull Context context) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(
                PREF_KEY, Context.MODE_PRIVATE);
        return sharedPref.getString(VIDEO_CODEC_KEY, "VP9");
    }

    public static void setVideoCodec(@NonNull Context context, @NonNull String codec) {
        checkNotNull(context);
        checkNotNull(codec);
        SharedPreferences sharedPref = context.getSharedPreferences(
                PREF_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(VIDEO_CODEC_KEY, codec);
        editor.apply();
    }

    public static boolean isAutomaticGainControlEnabled(@NonNull Context context) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
        return sharedPref.getBoolean(AGC_KEY, true);
    }

    public static void setAutomaticGainControlEnabled(@NonNull Context context, boolean enable) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(AGC_KEY, enable);
        editor.apply();
    }


    public static boolean isHardwareNoiseSuppressorEnabled(@NonNull Context context) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
        return sharedPref.getBoolean(HNS_KEY, true);
    }

    public static void setHardwareNoiseSuppressorEnabled(@NonNull Context context, boolean enable) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(HNS_KEY, enable);
        editor.apply();
    }

    public static boolean isAcousticEchoCancelerEnabled(@NonNull Context context) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
        return sharedPref.getBoolean(AEC_KEY, true);
    }

    public static void setAcousticEchoCancelerEnabled(@NonNull Context context, boolean enable) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(AEC_KEY, enable);
        editor.apply();
    }

    public static boolean isOpenSlESEnabled(@NonNull Context context) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
        return sharedPref.getBoolean(OPEN_SL_ES_KEY, true);
    }

    public static void setOpenSlESEnabled(@NonNull Context context, boolean enable) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(OPEN_SL_ES_KEY, enable);
        editor.apply();
    }

    public static boolean isAudioProcessingEnabled(@NonNull Context context) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
        return sharedPref.getBoolean(AUDIO_PROCESSING_KEY, true);
    }

    public static void setAudioProcessingEnabled(@NonNull Context context, boolean enable) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(AUDIO_PROCESSING_KEY, enable);
        editor.apply();
    }


    public static int getConnectionTimeout(@NonNull Context context) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
        return sharedPref.getInt(TIMEOUT_KEY, 30);
    }

    public static void setConnectionTimeout(@NonNull Context context, int timeout) {
        checkNotNull(context);
        checkArgument(timeout >= 0);
        SharedPreferences sharedPref = context.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(TIMEOUT_KEY, timeout);
        editor.apply();
    }

    public static int getSwarmTimeout(@NonNull Context context) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
        return sharedPref.getInt(SWARM_TIMEOUT_KEY, 5);
    }

    public static void setSwarmTimeout(@NonNull Context context, int timeout) {
        checkNotNull(context);
        checkArgument(timeout >= 0);
        SharedPreferences sharedPref = context.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(SWARM_TIMEOUT_KEY, timeout);
        editor.apply();
    }


    @NonNull
    public static CID getBitmap(@NonNull Context context, @NonNull String hash) {
        checkNotNull(context);
        checkNotNull(hash);
        checkArgument(!hash.isEmpty(), "Hash is empty.");


        if (BITMAP_HASH_TABLE.containsKey(hash)) {
            CID stored = BITMAP_HASH_TABLE.get(hash);
            checkNotNull(stored);
            return stored;
        }

        IPFS ipfs = Singleton.getInstance(context).getIpfs();
        checkNotNull(ipfs, "IPFS is not valid.");

        MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
        try {
            BitMatrix bitMatrix = multiFormatWriter.encode(hash,
                    BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE);
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.createBitmap(bitMatrix);

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] bytes = stream.toByteArray();
            bitmap.recycle();

            CID cid = ipfs.storeData(bytes, true);
            checkNotNull(cid);
            BITMAP_HASH_TABLE.put(hash, cid);
            return cid;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
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


    @NonNull
    public static String getDate(@NonNull Date date) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        Date today = c.getTime();
        c.set(Calendar.MONTH, 0);
        c.set(Calendar.DAY_OF_MONTH, 0);
        Date lastYear = c.getTime();

        if (date.before(today)) {
            if (date.before(lastYear)) {
                return android.text.format.DateFormat.format("dd.MM.yyyy", date).toString();
            } else {
                return android.text.format.DateFormat.format("dd.MMMM", date).toString();
            }
        } else {
            return android.text.format.DateFormat.format("HH:mm", date).toString();
        }
    }

    @NonNull
    public static String getCompactString(@NonNull String title) {
        checkNotNull(title);
        return title.replace("\n", " ");
    }


    public static void event(@NonNull EVENTS events,
                             @NonNull String identifier,
                             @NonNull String content) {
        checkNotNull(events);
        checkNotNull(identifier);
        checkNotNull(content);

        new Thread(() -> {
            events.invokeEvent(identifier, content);
        }).start();
    }

    public static void error(@NonNull EVENTS events,
                             @NonNull String message) {
        checkNotNull(events);
        checkNotNull(message);
        event(events, EXCEPTION, message);
    }


    public static void warning(@NonNull EVENTS events,
                               @NonNull String message) {
        checkNotNull(events);
        checkNotNull(message);
        event(events, WARNING, message);
    }

    public static void info(@NonNull EVENTS events,
                            @NonNull String message) {
        checkNotNull(events);
        checkNotNull(message);
        event(events, INFO, message);
    }

    public static void evaluateException(@NonNull EVENTS events,
                                         @NonNull String eventKey,
                                         @NonNull Throwable e) {
        checkNotNull(events);
        checkNotNull(eventKey);
        checkNotNull(e);

        event(events, eventKey, "" + e.getLocalizedMessage());

    }


    @NonNull
    public static String getDefaultTopic(@NonNull Context context) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(
                PREF_KEY, Context.MODE_PRIVATE);
        return sharedPref.getString(TOPIC_KEY, "pubsub");
    }

    public static void setDefaultTopic(@NonNull Context context, @NonNull String topic) {
        checkNotNull(context);
        checkNotNull(topic);

        SharedPreferences sharedPref = context.getSharedPreferences(
                PREF_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(TOPIC_KEY, topic);
        editor.apply();

    }


    @NonNull
    public static CID getPIDBitmap(@NonNull Context context) {
        checkNotNull(context);
        PID pid = getPID(context);
        checkNotNull(pid);
        String hash = pid.getPid();
        return getBitmap(context, hash);
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


    public static boolean getLoginFlag(@NonNull Context context) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(
                PREF_KEY, Context.MODE_PRIVATE);
        return sharedPref.getBoolean(LOGIN_FLAG_KEY, false);
    }

    public static void setLoginFlag(@NonNull Context context, boolean login) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(
                PREF_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(LOGIN_FLAG_KEY, login);
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

}
