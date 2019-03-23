package threads.server;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import threads.core.Preferences;
import threads.core.Singleton;
import threads.ipfs.api.Profile;
import threads.share.ConnectService;

import static com.google.common.base.Preconditions.checkNotNull;

public class Application extends android.app.Application {

    public static final String APP_KEY = "AppKey";
    public static final String UPDATE = "UPDATE";
    public static final String THREAD_KIND = "THREAD_KIND";


    private static void init(@NonNull Context context) {
        checkNotNull(context);
        new Thread(() -> {
            try {

                Service.cleanStates(context);
                Service.createHost(context);
                Service.checkPeers(context);
            } catch (Throwable e) {
                Preferences.evaluateException(Preferences.EXCEPTION, e);
            }
        }).start();

    }

    private void runUpdatesIfNecessary() {
        try {
            int versionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
            SharedPreferences prefs = this.getSharedPreferences(
                    Application.APP_KEY, Context.MODE_PRIVATE);
            if (prefs.getInt(Application.UPDATE, 0) != versionCode) {

                Preferences.setConfigChanged(getApplicationContext(), true);
                Preferences.setBinaryUpgrade(getApplicationContext(), true);


                Preferences.setProfile(getApplicationContext(), Profile.LOW_POWER);

                Preferences.setApiPort(getApplicationContext(), 5001);
                Preferences.setGatewayPort(getApplicationContext(), 8080);
                Preferences.setSwarmPort(getApplicationContext(), 4001);

                Preferences.setPubsubEnabled(getApplicationContext(), true);
                Preferences.setAutoRelayEnabled(getApplicationContext(), true);
                Preferences.setQUICEnabled(getApplicationContext(), true);

                ConnectService.setConnectionTimeout(getApplicationContext(), 60);
                ConnectService.setAutoRelay(getApplicationContext(), false);

                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt(Application.UPDATE, versionCode);
                editor.apply();
            }
        } catch (Throwable e) {
            // ignore exception
        }
    }


    @Override
    public void onCreate() {
        super.onCreate();

        runUpdatesIfNecessary();

        Preferences.setDaemonRunning(getApplicationContext(), false);


        NotificationSender.createChannel(getApplicationContext());

        try {
            Singleton.getInstance().init(getApplicationContext(), () -> "",
                    null, true);

            Preferences.setConfigChanged(getApplicationContext(), false);
            Preferences.setBinaryUpgrade(getApplicationContext(), false);
        } catch (Throwable e) {
            Preferences.evaluateException(Preferences.IPFS_INSTALL_FAILURE, e);
        }


        Service.startDaemon(getApplicationContext());

        init(getApplicationContext());

    }


}
