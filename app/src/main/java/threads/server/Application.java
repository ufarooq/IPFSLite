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
import android.util.Log;

import com.google.common.base.Preconditions;

import threads.ipfs.IPFS;
import threads.ipfs.api.CmdListener;
import threads.server.event.EventsDatabase;

public class Application extends android.app.Application {


    public static final String CHANNEL_ID = "SERVER_CHANGE_ID";
    public static final String GROUP_ID = "SERVER_GROUP_ID";
    public static final int QR_CODE_SIZE = 800;
    public static final String SERVER_ONLINE_EVENT = "SERVER_ONLINE_EVENT";
    public static final String SERVER_OFFLINE_EVENT = "SERVER_OFFLINE_EVENT";
    private static final String PID_KEY = "pidKey";
    private static final String PREF_KEY = "prefKey";
    private static final String TAG = Application.class.getSimpleName();

    private static EventsDatabase eventsDatabase;
    private static CmdListener cmdListener;
    private static IPFS ipfs;

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

    public static CmdListener getCmdListener() {
        return cmdListener;
    }

    public static String getDonationsAddress() {
        return BuildConfig.DONATION_ADDRESS;
    }

    public static void setPid(@NonNull Context context, @NonNull String pid) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(pid);
        SharedPreferences sharedPref = context.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(PID_KEY, pid);
        editor.apply();
    }

    public static String getPid(@NonNull Context context) {
        Preconditions.checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
        String pid = sharedPref.getString(PID_KEY, "");
        Log.e(TAG, "PID : " + pid);
        return pid;
    }

    public static void createChannel(@NonNull Context context) {
        try {
            // Create the NotificationChannel
            CharSequence name = context.getString(R.string.channel_name);
            String description = context.getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel mChannel = new NotificationChannel(Application.CHANNEL_ID, name, importance);
            mChannel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(
                    Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(mChannel);
            }

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }


    public static void init() {
        new java.lang.Thread(() -> {
            Application.getEventsDatabase().insertMessage("\nWelcome to IPFS");
            Application.getEventsDatabase().insertMessage("Please feel free to start an IPFS daemon ...\n\n");

        }).start();
    }


    public static String getHtmlAddressLink(@NonNull String name, @NonNull String address, boolean bundle) {
        if (bundle) {
            return "<a 'https://thetangle.org/bundle/" + address + "'><u>" + name + "</u></a>";
        }
        return "<a 'https://thetangle.org/address/" + address + "'><u>" + name + "</u></a>";
    }

    public static String getAddressLink(@NonNull String address, boolean bundle) {
        if (bundle) {
            return "https://thetangle.org/bundle/" + address;
        }
        return "https://thetangle.org/address/" + address;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        Log.e(TAG, "...... end application");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        cmdListener = new ConsoleListener();

        ipfs = new IPFS.Builder().context(getApplicationContext()).listener(cmdListener).build();

        eventsDatabase = Room.inMemoryDatabaseBuilder(this,
                EventsDatabase.class).build();


        init();


        Log.e(TAG, "...... start application");

    }


    private class ConsoleListener implements CmdListener {

        @Override
        public void info(@NonNull String message) {
            new Thread(() -> {
                getEventsDatabase().insertMessage(message);
            }).start();
        }

        @Override
        public void error(@NonNull String message) {
            new Thread(() -> {
                getEventsDatabase().insertMessage(message);
            }).start();
        }
    }
}
