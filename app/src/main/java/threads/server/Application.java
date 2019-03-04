package threads.server;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.util.Log;

import androidx.annotation.NonNull;
import threads.core.Preferences;
import threads.core.Singleton;
import threads.core.THREADS;
import threads.core.api.MessageKind;
import threads.ipfs.IPFS;
import threads.ipfs.api.Profile;

import static com.google.common.base.Preconditions.checkNotNull;

public class Application extends android.app.Application {


    public static final int CON_TIMEOUT = 30;
    private static final String TAG = Application.class.getSimpleName();
    private static final String PREF_KEY = "prefKey";
    private static final String CONFIG_CHANGE_KEY = "configChangeKey";
    private static final String AUTO_CONNECTED_KEY = "autoConnectedKey";

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


    private static void init(@NonNull Context context) {
        checkNotNull(context);

        final THREADS threads = Singleton.getInstance().getThreads();
        final IPFS ipfs = Singleton.getInstance().getIpfs();
        if (ipfs != null) {
            new Thread(() -> {

                try {

                    Service.createHost(context, ipfs);
                    Service.createRelay(context, ipfs);

                    threads.storeMessage(threads.createMessage(MessageKind.INFO,
                            "\nWelcome to IPFS",
                            System.currentTimeMillis()));

                    threads.storeMessage(threads.createMessage(MessageKind.INFO,
                            "Please feel free to start an IPFS daemon ...\n\n"
                            , System.currentTimeMillis()));


                    Service.connectUsers(context);
                } catch (Throwable e) {
                    Preferences.evaluateException(Preferences.EXCEPTION, e);
                }

            }).start();
        }
    }

    public static boolean isAutoConnected(@NonNull Context context) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
        return sharedPref.getBoolean(AUTO_CONNECTED_KEY, false);
    }

    public static void setAutoConnected(@NonNull Context context, boolean enable) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(AUTO_CONNECTED_KEY, enable);
        editor.apply();
    }

    public static boolean hasConfigChanged(@NonNull Context context) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
        return sharedPref.getBoolean(CONFIG_CHANGE_KEY, true);
    }

    public static void setConfigChanged(@NonNull Context context, boolean enable) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(CONFIG_CHANGE_KEY, enable);
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

        Application.setConfigChanged(getApplicationContext(), true);
        Application.setAutoConnected(getApplicationContext(), true);

        Preferences.setDaemonRunning(getApplicationContext(), false);
        Preferences.setProfile(getApplicationContext(), Profile.LOW_POWER);
        Preferences.setPubsubEnabled(getApplicationContext(), true);
        Preferences.setAutoRelayEnabled(getApplicationContext(), true);

        //Preferences.setRelay(getApplicationContext(),
        //        PID.create("QmchgNzyUFyf2wpfDMmpGxMKHA3PkC1f3H2wUgbs21vXoh"));
        //Preferences.setRelay(getApplicationContext(), null);

        try {
            Singleton.getInstance().init(getApplicationContext(), () -> "",
                    null, false, true);
        } catch (Throwable e) {
            Preferences.evaluateException(Preferences.IPFS_INSTALL_FAILURE, e);
        }

        init(getApplicationContext());


    }


}
