package threads.core;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import java.util.Calendar;
import java.util.Date;

import threads.core.events.EVENTS;

import static androidx.core.util.Preconditions.checkArgument;
import static androidx.core.util.Preconditions.checkNotNull;

public class Preferences {


    public static final String EXCEPTION = "EXCEPTION";
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

    private static final String AEC_KEY = "aecKey";
    private static final String AGC_KEY = "agcKey";
    private static final String HNS_KEY = "hnsKey";


    private static final String DEBUG_MODE_KEY = "debugModeKey";
    private static final String REPORT_MODE_KEY = "reportModeKey";


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
        // todo context
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


}
