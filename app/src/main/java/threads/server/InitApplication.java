package threads.server;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import threads.iota.EntityService;
import threads.ipfs.ConnMgrConfig;
import threads.ipfs.IPFS;
import threads.ipfs.RoutingConfig;
import threads.server.services.LiteService;
import threads.server.utils.Preferences;
import threads.server.utils.ProgressChannel;

import static androidx.core.util.Preconditions.checkNotNull;

public class InitApplication extends Application {
    private static final String APP_KEY = "AppKey";
    private static final String UPDATE = "UPDATE";
    private static final String TAG = InitApplication.class.getSimpleName();
    private static final String LOGIN_FLAG_KEY = "loginFlagKey";

    public static boolean getLoginFlag(@NonNull Context context) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(
                APP_KEY, Context.MODE_PRIVATE);
        return sharedPref.getBoolean(LOGIN_FLAG_KEY, false);
    }

    public static void setLoginFlag(@NonNull Context context, boolean login) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(
                APP_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(LOGIN_FLAG_KEY, login);
        editor.apply();
    }

    public static void runUpdatesIfNecessary(@NonNull Context context) {
        try {
            int versionCode = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), 0).versionCode;
            SharedPreferences prefs = context.getSharedPreferences(
                    APP_KEY, Context.MODE_PRIVATE);
            if (prefs.getInt(UPDATE, 0) != versionCode) {

                setLoginFlag(context, false); // TODO remove later
                IPFS.deleteConfigFile(context); // TODO remove later
                IPFS.cleanBaseDir(context); // TODO remove later
                IPFS.cleanCacheDir(context); // TODO remove later


                // Experimental Features
                IPFS.setQUICEnabled(context, true);
                IPFS.setPreferTLS(context, true);


                IPFS.setSwarmPort(context, 4001);
                IPFS.setRoutingType(context, RoutingConfig.TypeEnum.dhtclient);


                IPFS.setAutoNATServiceEnabled(context, false);
                IPFS.setRelayHopEnabled(context, false);
                IPFS.setAutoRelayEnabled(context, true);

                IPFS.setConnMgrConfigType(context, ConnMgrConfig.TypeEnum.basic);
                IPFS.setLowWater(context, 50);
                IPFS.setHighWater(context, 200);
                IPFS.setGracePeriod(context, "10s");


                Preferences.setConnectionTimeout(context, 45);
                EntityService.setTangleTimeout(context, 60);

                IPFS.setMDNSEnabled(context, true);

                IPFS.setRandomSwarmPort(context, true);


                setDontShowAgain(context, LiteService.PIN_SERVICE_KEY, false);

                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt(UPDATE, versionCode);
                editor.apply();
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    public static boolean getDontShowAgain(@NonNull Context context, @NonNull String key) {
        checkNotNull(context);
        checkNotNull(key);
        SharedPreferences sharedPref = context.getSharedPreferences(
                APP_KEY, Context.MODE_PRIVATE);
        return sharedPref.getBoolean(key, false);
    }

    public static void setDontShowAgain(@NonNull Context context, @NonNull String key, boolean value) {
        checkNotNull(context);
        checkNotNull(key);
        SharedPreferences sharedPref = context.getSharedPreferences(
                APP_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(key, value);
        editor.apply();

    }

    @Override
    public void onCreate() {
        super.onCreate();

        runUpdatesIfNecessary(getApplicationContext());

        ProgressChannel.createProgressChannel(getApplicationContext());
    }


}
