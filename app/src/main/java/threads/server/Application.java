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
import threads.core.api.User;
import threads.core.api.UserStatus;
import threads.core.api.UserType;
import threads.ipfs.IPFS;
import threads.ipfs.api.PID;

import static com.google.common.base.Preconditions.checkNotNull;

public class Application extends android.app.Application {


    private static final String TAG = Application.class.getSimpleName();
    private static final String PREF_KEY = "prefKey";
    private static final String CONFIG_CHANGE_KEY = "configChangeKey";

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

        final THREADS threadsApi = Singleton.getInstance().getThreads();
        final IPFS ipfs = Singleton.getInstance().getIpfs();
        if (ipfs != null) {
            new Thread(() -> {


                PID pid = Preferences.getPID(context);
                checkNotNull(pid);
                User user = threadsApi.getUserByPID(pid);
                if (user == null) {

                    String inbox = Preferences.getInbox(context);
                    checkNotNull(inbox);
                    String publicKey = ipfs.getPublicKey();
                    byte[] image = THREADS.getImage(context,
                            pid.getPid(), R.drawable.server_network);
                    user = threadsApi.createUser(pid, inbox, publicKey,
                            pid.getPid(), UserType.VERIFIED, image, null);
                    user.setStatus(UserStatus.BLOCKED);
                    threadsApi.storeUser(user);
                }

                threadsApi.storeMessage(threadsApi.createMessage(MessageKind.INFO,
                        "\nWelcome to IPFS",
                        System.currentTimeMillis()));

                threadsApi.storeMessage(threadsApi.createMessage(MessageKind.INFO,
                        "Please feel free to start an IPFS daemon ...\n\n"
                        , System.currentTimeMillis()));

                DaemonService.evalUserStatus(threadsApi);

            }).start();
        }
    }

    public static boolean hasConfigChanged(@NonNull Context context) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
        return sharedPref.getBoolean(CONFIG_CHANGE_KEY, false);
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

        try {
            Singleton.getInstance().init(getApplicationContext(), () -> "",
                    null, false, true);
        } catch (Throwable e) {
            Preferences.evaluateException(Preferences.IPFS_INSTALL_FAILURE, e);
        }

        init(getApplicationContext());


    }

}
