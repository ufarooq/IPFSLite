package threads.server;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.arch.persistence.room.Room;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.internal.Preconditions;

import threads.core.IThreadsAPI;
import threads.core.ThreadsAPI;
import threads.core.api.ThreadsDatabase;
import threads.iri.ITangleDaemon;
import threads.iri.daemon.TangleDaemon;
import threads.iri.event.EventsDatabase;
import threads.iri.server.Server;
import threads.iri.server.ServerDatabase;
import threads.iri.server.ServerVisibility;
import threads.iri.tangle.ITangleServer;
import threads.iri.tangle.Pair;
import threads.iri.tangle.TangleDatabase;
import threads.iri.tangle.TangleServer;
import threads.iri.tangle.TangleUtils;

import static com.google.common.base.Preconditions.checkNotNull;

public class Application extends android.app.Application {

    public static final String TANGLE_HOST = "nodes.thetangle.org";
    public static final String TANGLE_PROTOCOL = "https";
    public static final String TANGLE_PORT = "443";
    public static final String TANGLE_LINK = "";
    public static final String TANGLE_CERT = "";
    public static final String TANGLE_ALIAS = "";

    public static final String APPLICATION_AES_KEY = "f2YSXkXvJfp5j45Q8OT+uA==";
    private static final String ACCOUNT_ADDRESS_KEY = "ACCOUNT_ADDRESS_KEY";
    private static final String SERVER_CONFIG_KEY = "SERVER_CONFIG_KEY";

    public static final String CHANNEL_ID = "IRI_SERVER_CHANGEL_ID";
    private static final String THREADS_DATABASE = "THREADS_DATABASE";
    private static final String TAG = "Application";
    private static final String TANGLE_DATABASE = "TANGLE_DATABASE";
    private static final String SERVER_DATABASE = "SERVER_DATABASE";


    private static TangleDatabase tangleDatabase;
    private static ITangleDaemon tangleDaemon;
    private static EventsDatabase eventsDatabase;
    private static ServerDatabase serverDatabase;
    private static IThreadsAPI ttApi;
    private static ThreadsDatabase threadsDatabase;

    public static IThreadsAPI getThreadsAPI() {
        return ttApi;
    }

    public static ServerDatabase getServerDatabase() {
        return serverDatabase;
    }
    public static EventsDatabase getEventsDatabase() {
        return eventsDatabase;
    }

    public static ITangleDaemon getTangleDaemon() {
        return tangleDaemon;
    }

    public static TangleDatabase getTangleDatabase() {
        return tangleDatabase;
    }

    public static ITangleServer getTangleServer(@NonNull Context context) {
        Preconditions.checkNotNull(context);
        ITangleServer tangleServer = TangleServer.getTangleServer(Application.getServerDatabase(),
                Application.getDefaultServer(context));
        return tangleServer;
    }

    public static Server getDefaultServer(@NonNull Context context) {
        Preconditions.checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(SERVER_CONFIG_KEY, Context.MODE_PRIVATE);
        String protocol = sharedPref.getString("protocol", TANGLE_PROTOCOL);
        String host = sharedPref.getString("host", TANGLE_HOST);
        String port = sharedPref.getString("port", TANGLE_PORT);
        String cert = sharedPref.getString("cert", TANGLE_CERT);
        String link = sharedPref.getString("link", TANGLE_LINK);
        String alias = sharedPref.getString("alias", TANGLE_ALIAS);
        return Server.createServer(protocol, host, port, cert, link, alias);
    }


    public static void setDefaultServer(@NonNull Context context, @NonNull Server server) {
        Preconditions.checkNotNull(server);
        SharedPreferences sharedPref = context.getSharedPreferences(SERVER_CONFIG_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("protocol", server.getProtocol());
        editor.putString("host", server.getHost());
        editor.putString("port", server.getPort());
        editor.putString("cert", server.getCert());
        editor.putString("link", server.getLink());
        editor.putString("alias", server.getAlias());

        editor.apply();
    }




    public static void createChannel(@NonNull Context context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
                notificationManager.createNotificationChannel(mChannel);
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage());
        }
    }

    public static Bitmap getBitmap(@NonNull Context context, int resource) {

        Drawable drawable = context.getDrawable(resource);
        Canvas canvas = new Canvas();

        Bitmap bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        drawable.setBounds(0, 0, 64, 64);

        drawable.draw(canvas);
        return bitmap;
    }

    public static void initMessageDatabase() {
        new java.lang.Thread(new Runnable() {
            public void run() {
                Application.getEventsDatabase().insertMessage("\nWelcome to the IRI android daemon.");
                Application.getEventsDatabase().insertMessage("Please feel free to start the daemon ....\n\n");

            }
        }).start();
    }

    public static Server getDaemonServer(@NonNull Context context) {
        checkNotNull(context);
        ITangleDaemon tangleDaemon = getTangleDaemon();
        Pair<Server, ServerVisibility> pair = ITangleDaemon.getDaemonServer(
                context, tangleDaemon);
        return pair.first;
    }

    private static void setAccountAddress(@NonNull Context context, @NonNull String account) {
        checkNotNull(context);
        checkNotNull(account);
        try {
            // encrypt seed and add to preferences
            SharedPreferences sharedPref = context.getSharedPreferences(
                    Application.class.getName(), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(ACCOUNT_ADDRESS_KEY, account);
            editor.apply();
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    public static String getAccountAddress(@NonNull Context context) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(
                Application.class.getName(), Context.MODE_PRIVATE);

        String accountAddress = sharedPref.getString(ACCOUNT_ADDRESS_KEY, "");
        if (accountAddress.isEmpty()) {
            accountAddress = TangleUtils.generateSeed();
            setAccountAddress(context, accountAddress);
        }
        return accountAddress;
    }
    @Override
    public void onTerminate() {
        super.onTerminate();
        Log.e(TAG, "...... end application");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        serverDatabase = Room.inMemoryDatabaseBuilder(this, ServerDatabase.class).build();
        eventsDatabase = Room.inMemoryDatabaseBuilder(this,
                EventsDatabase.class).build();
        tangleDaemon = TangleDaemon.getInstance(this, eventsDatabase);
        threadsDatabase = Room.databaseBuilder(this,
                ThreadsDatabase.class, THREADS_DATABASE).fallbackToDestructiveMigration().build();
        tangleDatabase = Room.databaseBuilder(this,
                TangleDatabase.class, TANGLE_DATABASE).fallbackToDestructiveMigration().build();
        ttApi = ThreadsAPI.getThreadsAPI(threadsDatabase, eventsDatabase);

        initMessageDatabase();


        Log.e(TAG, "...... start application");

    }


}
