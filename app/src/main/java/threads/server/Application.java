package threads.server;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.arch.persistence.room.Room;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Log;

import threads.core.IAesKey;
import threads.core.IThreadsAPI;
import threads.core.api.ThreadsDatabase;
import threads.iri.IThreadsServer;
import threads.iri.daemon.ThreadsServer;
import threads.iri.daemon.TransactionDatabase;
import threads.iri.event.EventsDatabase;
import threads.iri.server.Certificate;
import threads.iri.server.Server;
import threads.iri.server.ServerDatabase;
import threads.iri.server.ServerVisibility;
import threads.iri.tangle.Pair;

public class Application extends android.app.Application {


    public static final String CHANNEL_ID = "IRI_SERVER_CHANGEL_ID";
    private static final String TAG = Application.class.getSimpleName();

    private static TransactionDatabase transactionDatabase;
    private static IThreadsServer threadsServer;
    private static EventsDatabase eventsDatabase;
    private static ServerDatabase serverDatabase;
    private static IThreadsAPI ttApi;
    private static ThreadsDatabase threadsDatabase;
    private static IAesKey aesKey;

    public static IAesKey getAesKey() {
        return aesKey;
    }

    public static IThreadsAPI getThreadsAPI() {
        return ttApi;
    }

    public static ServerDatabase getServerDatabase() {
        return serverDatabase;
    }

    public static EventsDatabase getEventsDatabase() {
        return eventsDatabase;
    }

    public static IThreadsServer getThreadsServer() {
        return threadsServer;
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

    public static Certificate getCertificate() {
        Certificate certificate = serverDatabase.getCertificate();
        if (certificate == null) {
            certificate = Server.createCertificate();
            serverDatabase.insertCertificate(certificate);
        }
        return certificate;
    }

    public static Server getDefaultThreadsServer() {

        Pair<String, ServerVisibility> pair = IThreadsServer.getIPv6HostAddress();
        Certificate certificate = getCertificate();
        return Server.createServer(IThreadsServer.HTTPS_PROTOCOL,
                pair.first, String.valueOf(IThreadsServer.TCP_PORT), certificate.getShaHash(), Server.getDefaultServerAlias());
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        Log.e(TAG, "...... end application");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        aesKey = new AesKey();
        serverDatabase = Room.inMemoryDatabaseBuilder(this, ServerDatabase.class).build();
        eventsDatabase = Room.inMemoryDatabaseBuilder(this,
                EventsDatabase.class).build();
        transactionDatabase = Room.databaseBuilder(this,
                TransactionDatabase.class,
                TransactionDatabase.class.getSimpleName()).fallbackToDestructiveMigration().build();
        threadsServer = ThreadsServer.createThreadServer(this, transactionDatabase, eventsDatabase);

        threadsDatabase = Room.databaseBuilder(this,
                ThreadsDatabase.class, ThreadsDatabase.class.getSimpleName()).fallbackToDestructiveMigration().build();

        ttApi = IThreadsAPI.createThreadsAPI(threadsDatabase, eventsDatabase);

        initMessageDatabase();


        Log.e(TAG, "...... start application");

    }

    private class AesKey implements IAesKey {

        @Override
        public String getAesKey() {
            return BuildConfig.ApiAesKey;
        }

    }
}
