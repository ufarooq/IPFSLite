package threads.server;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.arch.persistence.room.Room;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import threads.ipfs.IPFS;
import threads.ipfs.api.CmdListener;
import threads.ipfs.api.Profile;

import static com.google.common.base.Preconditions.checkNotNull;

public class Application extends android.app.Application {


    public static final String CHANNEL_ID = "CHANNEL_ID";
    public static final String GROUP_ID = "GROUP_ID";

    public static final String SERVER_ONLINE_EVENT = "SERVER_ONLINE_EVENT";
    public static final String SERVER_OFFLINE_EVENT = "SERVER_OFFLINE_EVENT";
    private static final String PID_KEY = "pidKey";
    private static final String PROFILE_KEY = "profileKey";
    private static final String PREF_KEY = "prefKey";
    private static final String TAG = Application.class.getSimpleName();

    private static EventsDatabase eventsDatabase;

    private static IPFS ipfs;

    @Nullable
    public static IPFS getIpfs() {
        return ipfs;
    }

    public static EventsDatabase getEventsDatabase() {
        return eventsDatabase;
    }


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

    public static void setProfile(@NonNull Context context, @NonNull Profile profile) {
        checkNotNull(context);
        checkNotNull(profile);
        SharedPreferences sharedPref = context.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(PROFILE_KEY, profile.name());
        editor.apply();
    }

    public static Profile getProfile(@NonNull Context context) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
        String profile = sharedPref.getString(PROFILE_KEY, Profile.LOW_POWER.name());
        return Profile.valueOf(profile);
    }

    public static void setPid(@NonNull Context context, @NonNull String pid) {
        checkNotNull(context);
        checkNotNull(pid);
        SharedPreferences sharedPref = context.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(PID_KEY, pid);
        editor.apply();
    }

    public static String getPid(@NonNull Context context) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
        String pid = sharedPref.getString(PID_KEY, "");
        Log.e(TAG, "PID : " + pid);
        return pid;
    }

    public static void createChannel(@NonNull Context context) {
        checkNotNull(context);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            try {
                CharSequence name = context.getString(R.string.channel_name);
                String description = context.getString(R.string.channel_description);
                int importance = NotificationManager.IMPORTANCE_HIGH;
                NotificationChannel mChannel = new NotificationChannel(Application.CHANNEL_ID, name, importance);
                mChannel.setDescription(description);

                NotificationManager notificationManager = (NotificationManager) context.getSystemService(
                        Context.NOTIFICATION_SERVICE);
                if (notificationManager != null) {
                    notificationManager.createNotificationChannel(mChannel);
                }

            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }
        }
    }


    public static void init() {
        new java.lang.Thread(() -> {
            Application.getEventsDatabase().insertMessage(MessageKind.INFO, "\nWelcome to IPFS");
            Application.getEventsDatabase().insertMessage(MessageKind.INFO, "Please feel free to start an IPFS daemon ...\n\n");

        }).start();
    }


    @Override
    public void onTerminate() {
        super.onTerminate();
        Log.e(TAG, "...... end application");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        CmdListener cmdListener = new ConsoleListener();

        try {
            ipfs = new IPFS.Builder().context(getApplicationContext()).listener(cmdListener).build();
        } catch (Throwable e) {
            new Thread(() -> {
                getEventsDatabase().insertMessage(MessageKind.ERROR,
                        "Installation problems : " + e.getLocalizedMessage());
            }).start();
        }
        eventsDatabase = Room.inMemoryDatabaseBuilder(this, EventsDatabase.class).build();

        init();


        Log.e(TAG, "...... start application");

    }


    private class ConsoleListener implements CmdListener {

        @Override
        public void info(@NonNull String message) {
            new Thread(() -> {
                getEventsDatabase().insertMessage(MessageKind.INFO, message);
            }).start();
        }

        @Override
        public void error(@NonNull String message) {
            new Thread(() -> {
                getEventsDatabase().insertMessage(MessageKind.ERROR, message);
            }).start();
        }

        @Override
        public void cmd(@NonNull String message) {
            new Thread(() -> {
                getEventsDatabase().insertMessage(MessageKind.CMD, message);
            }).start();
        }
    }
}
