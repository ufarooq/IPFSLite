package threads.server.services;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RawRes;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import threads.ipfs.PID;
import threads.server.R;
import threads.server.core.events.EVENTS;
import threads.server.core.peers.PEERS;
import threads.server.core.peers.User;
import threads.server.utils.Network;
import threads.server.work.ConnectUserWorker;
import threads.server.work.ConnectionWorker;


public class LiteService {

    public static final String PIN_SERVICE_KEY = "pinServiceKey";
    private static final String TAG = LiteService.class.getSimpleName();

    private static final String APP_KEY = "AppKey";
    private static final String PIN_SERVICE_TIME_KEY = "pinServiceTimeKey";
    private static final String GATEWAY_KEY = "gatewayKey";
    private static final String AUTO_DOWNLOAD_KEY = "autoDownloadKey";
    private static final String SEND_NOTIFICATIONS_ENABLED_KEY = "sendNotificationKey";
    private static final String RECEIVE_NOTIFICATIONS_ENABLED_KEY = "receiveNotificationKey";


    public static boolean isAutoDownload(@NonNull Context context) {
        Objects.requireNonNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(
                APP_KEY, Context.MODE_PRIVATE);
        return sharedPref.getBoolean(AUTO_DOWNLOAD_KEY, true);
    }

    public static void setAutoDownload(@NonNull Context context, boolean automaticDownload) {
        Objects.requireNonNull(context);

        SharedPreferences sharedPref = context.getSharedPreferences(
                APP_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(AUTO_DOWNLOAD_KEY, automaticDownload);
        editor.apply();

    }

    @NonNull
    public static String getGateway(@NonNull Context context) {
        Objects.requireNonNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(
                APP_KEY, Context.MODE_PRIVATE);
        return sharedPref.getString(GATEWAY_KEY, "https://ipfs.io");
    }

    public static void setGateway(@NonNull Context context, @NonNull String gateway) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(gateway);
        SharedPreferences sharedPref = context.getSharedPreferences(
                APP_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(GATEWAY_KEY, gateway);
        editor.apply();

    }

    public static int getPublishServiceTime(@NonNull Context context) {
        Objects.requireNonNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE);
        return sharedPref.getInt(PIN_SERVICE_TIME_KEY, 6);
    }

    public static void setPublisherServiceTime(@NonNull Context context, int timeout) {
        Objects.requireNonNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(PIN_SERVICE_TIME_KEY, timeout);
        editor.apply();
    }


    public static boolean isSendNotificationsEnabled(@NonNull Context context) {
        Objects.requireNonNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE);
        return sharedPref.getBoolean(SEND_NOTIFICATIONS_ENABLED_KEY, true);
    }

    public static void setSendNotificationsEnabled(@NonNull Context context, boolean enable) {
        Objects.requireNonNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(SEND_NOTIFICATIONS_ENABLED_KEY, enable);
        editor.apply();
    }


    public static boolean isReceiveNotificationsEnabled(@NonNull Context context) {
        Objects.requireNonNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE);
        return sharedPref.getBoolean(RECEIVE_NOTIFICATIONS_ENABLED_KEY, true);
    }

    public static void setReceiveNotificationsEnabled(@NonNull Context context, boolean enable) {
        Objects.requireNonNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(RECEIVE_NOTIFICATIONS_ENABLED_KEY, enable);
        editor.apply();
    }


    public static void connectPeer(@NonNull Context context, @NonNull PID user, boolean addMessage) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(user);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                PEERS peers = PEERS.getInstance(context);
                EVENTS events = EVENTS.getInstance(context);

                if (!peers.existsUser(user)) {

                    User newUser = peers.createUser(user, user.getPid());
                    newUser.setBlocked(false);
                    peers.storeUser(newUser);

                    if (addMessage) {
                        events.warning(context.getString(R.string.added_pid_to_peers, user));
                    }

                } else {
                    events.warning(context.getString(R.string.peer_exists_with_pid));
                    return;
                }

                peers.setUserDialing(user.getPid(), Network.isConnected(context));

                ConnectionWorker.connect(context, false);// TODO looks like a chain here
                ConnectUserWorker.connect(context, user.getPid());
            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }
        });
    }

    public static boolean isNightNode(@NonNull Context context) {
        int nightModeFlags =
                context.getResources().getConfiguration().uiMode &
                        Configuration.UI_MODE_NIGHT_MASK;
        switch (nightModeFlags) {
            case Configuration.UI_MODE_NIGHT_YES:
                return true;
            case Configuration.UI_MODE_NIGHT_UNDEFINED:
            case Configuration.UI_MODE_NIGHT_NO:
                return false;
        }
        return false;
    }

    @NonNull
    public static String loadRawData(@NonNull Context context, @RawRes int id) {
        Objects.requireNonNull(context);

        try (InputStream inputStream = context.getResources().openRawResource(id)) {
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

                IOUtils.copy(inputStream, outputStream);
                return new String(outputStream.toByteArray());

            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
            return "";
        }
    }

}
