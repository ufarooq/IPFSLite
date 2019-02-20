package threads.server;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.util.Log;

import androidx.annotation.NonNull;
import threads.core.IThreadsAPI;
import threads.core.Preferences;
import threads.core.Singleton;
import threads.core.api.MessageKind;

import static com.google.common.base.Preconditions.checkNotNull;

public class Application extends android.app.Application {


    private static final String QUIC_KEY = "quicKey";
    private static final String PUBSUB_KEY = "pubsubKey";
    private static final String PREF_KEY = "prefKey";
    private static final String TAG = Application.class.getSimpleName();


    public static boolean isConnected(@NonNull Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) return false;

        Network network = connectivityManager.getActiveNetwork();
        if (network == null) return false;

        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
        return capabilities != null
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);

    }


    public static void init() {
        new Thread(() -> {

            IThreadsAPI threadsApi = Singleton.getInstance().getThreadsAPI();


            threadsApi.storeMessage(threadsApi.createMessage(MessageKind.INFO,
                    "\nWelcome to IPFS",
                    System.currentTimeMillis()));

            threadsApi.storeMessage(threadsApi.createMessage(MessageKind.INFO,
                    "Please feel free to start an IPFS daemon ...\n\n"
                    , System.currentTimeMillis()));

            DaemonService.evalUserStatus(threadsApi);

        }).start();
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
        return sharedPref.getBoolean(PUBSUB_KEY, false);
    }

    public static void setPubsubEnabled(@NonNull Context context, boolean enable) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(PUBSUB_KEY, enable);
        editor.apply();
    }


    @Override
    public void onTerminate() {
        super.onTerminate();
        Log.e(TAG, "...... end application");
    }

    @Override
    public void onCreate() {
        super.onCreate();


        Log.e(TAG, "...... start application");
        NotificationSender.createChannel(getApplicationContext());

        try {
            Singleton.getInstance().init(getApplicationContext(), () -> "",
                    null, false, true);
        } catch (Throwable e) {
            Preferences.evaluateException(Preferences.IPFS_INSTALL_FAILURE, e);
        }


        init();


    }

}
