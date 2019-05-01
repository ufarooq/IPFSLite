package threads.server;

import android.content.Context;
import android.content.SharedPreferences;

import threads.core.Preferences;
import threads.ipfs.api.ConnMgrConfig;
import threads.ipfs.api.Profile;
import threads.ipfs.api.PubsubConfig;
import threads.share.ConnectService;
import threads.share.RTCCallActivity;

public class Application extends android.app.Application {


    public static final String THREAD_KIND = "THREAD_KIND";

    private static final String APP_KEY = "AppKey";
    private static final String UPDATE = "UPDATE";


    private void runUpdatesIfNecessary() {
        try {
            int versionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
            SharedPreferences prefs = this.getSharedPreferences(
                    Application.APP_KEY, Context.MODE_PRIVATE);
            if (prefs.getInt(Application.UPDATE, 0) != versionCode) {

                Preferences.setConfigChanged(getApplicationContext(), true);
                Preferences.setBinaryUpgrade(getApplicationContext(), true);


                Preferences.setProfile(getApplicationContext(), Profile.LOW_POWER);
                Preferences.setPubsubEnabled(getApplicationContext(), true);

                // Experimental Features
                Preferences.setQUICEnabled(getApplicationContext(), true);
                Preferences.setFilestoreEnabled(getApplicationContext(), false);


                Preferences.setApiPort(getApplicationContext(), 5001);
                Preferences.setGatewayPort(getApplicationContext(), 8080);
                Preferences.setSwarmPort(getApplicationContext(), 4001);


                Preferences.setAutoNATServiceEnabled(getApplicationContext(), false);
                Preferences.setRelayHopEnabled(getApplicationContext(), false);
                Preferences.setAutoRelayEnabled(getApplicationContext(), false);

                Preferences.setPubsubRouter(getApplicationContext(), PubsubConfig.RouterEnum.gossipsub);

                Preferences.setConnMgrConfigType(getApplicationContext(), ConnMgrConfig.TypeEnum.basic);
                Preferences.setLowWater(getApplicationContext(), 30);
                Preferences.setHighWater(getApplicationContext(), 80);
                Preferences.setGracePeriod(getApplicationContext(), "5s");


                ConnectService.setConnectionTimeout(getApplicationContext(), 60);
                ConnectService.setAutoConnectRelay(getApplicationContext(), true);


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
        Preferences.createPublicPrivateKeys(getApplicationContext());


        NotificationSender.createChannel(getApplicationContext());
        RTCCallActivity.createRTCChannel(getApplicationContext());

    }


}
