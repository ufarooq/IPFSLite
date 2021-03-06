package threads.server;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import org.iota.jota.utils.Constants;
import org.iota.jota.utils.TrytesConverter;

import threads.iota.EntityService;
import threads.iota.IOTA;
import threads.ipfs.ConnMgrConfig;
import threads.ipfs.IPFS;
import threads.ipfs.RoutingConfig;
import threads.server.jobs.JobServiceCleanup;
import threads.server.jobs.JobServicePublisher;
import threads.server.services.LiteService;
import threads.server.work.DownloadContentWorker;

public class InitApplication extends Application {
    private static final String APP_KEY = "AppKey";
    private static final String UPDATE = "UPDATE";
    private static final String TAG = InitApplication.class.getSimpleName();
    private static final String PREF_KEY = "prefKey";
    private static final String TIMEOUT_KEY = "timeoutKey";
    private static final String DOWN_TIMEOUT_KEY = "downTimeoutKey";

    public static int getDownloadTimeout(@NonNull Context context) {

        SharedPreferences sharedPref = context.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
        return sharedPref.getInt(DOWN_TIMEOUT_KEY, 60);
    }

    public static void setDownloadTimeout(@NonNull Context context, int timeout) {

        SharedPreferences sharedPref = context.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(DOWN_TIMEOUT_KEY, timeout);
        editor.apply();
    }

    public static int getConnectionTimeout(@NonNull Context context) {

        SharedPreferences sharedPref = context.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
        return sharedPref.getInt(TIMEOUT_KEY, 15);
    }

    public static void setConnectionTimeout(@NonNull Context context, int timeout) {

        SharedPreferences sharedPref = context.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(TIMEOUT_KEY, timeout);
        editor.apply();
    }

    @NonNull
    public static String getAddress(@NonNull String pid) {

        String address = TrytesConverter.asciiToTrytes(pid);
        return IOTA.addChecksum(address.substring(0, Constants.ADDRESS_LENGTH_WITHOUT_CHECKSUM));

    }

    public static void runUpdatesIfNecessary(@NonNull Context context) {
        try {
            int versionCode = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), 0).versionCode;
            SharedPreferences prefs = context.getSharedPreferences(
                    APP_KEY, Context.MODE_PRIVATE);
            if (prefs.getInt(UPDATE, 0) != versionCode) {

                // Experimental Features
                IPFS.setQUICEnabled(context, true);
                IPFS.setPreferTLS(context, true);
                IPFS.setPubSubEnabled(context, false);

                IPFS.setSwarmPort(context, 4001);
                IPFS.setRoutingType(context, RoutingConfig.TypeEnum.dhtclient);


                IPFS.setAutoNATServiceEnabled(context, true);
                IPFS.setRelayHopEnabled(context, false);
                IPFS.setAutoRelayEnabled(context, true);

                IPFS.setConnMgrConfigType(context, ConnMgrConfig.TypeEnum.basic);
                IPFS.setLowWater(context, 50);
                IPFS.setHighWater(context, 200);
                IPFS.setGracePeriod(context, "10s");


                InitApplication.setConnectionTimeout(context, 45);
                InitApplication.setDownloadTimeout(context, 180);

                EntityService.setTangleTimeout(context, 60);

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

        SharedPreferences sharedPref = context.getSharedPreferences(
                APP_KEY, Context.MODE_PRIVATE);
        return sharedPref.getBoolean(key, false);
    }

    public static void setDontShowAgain(@NonNull Context context, @NonNull String key, boolean value) {

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

        // periodic jobs
        JobServicePublisher.publish(getApplicationContext());

        JobServiceCleanup.cleanup(getApplicationContext());


        DownloadContentWorker.createChannel(getApplicationContext());
    }


}
