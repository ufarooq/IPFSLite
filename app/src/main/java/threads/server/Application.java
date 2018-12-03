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

import threads.iota.event.EventsDatabase;
import threads.iota.server.Certificate;
import threads.iota.server.Server;
import threads.iota.server.ServerDatabase;
import threads.server.daemon.IThreadsServer;
import threads.server.daemon.ThreadsServer;
import threads.server.daemon.TransactionDatabase;

public class Application extends android.app.Application {


    public static final String CHANNEL_ID = "IRI_SERVER_CHANGEL_ID";
    public static final String GROUP_ID = "IRI_SERVER_GROUP_ID";
    public static final int QR_CODE_SIZE = 800;
    private static final String TAG = Application.class.getSimpleName();
    private static IThreadsServer threadsServer;
    private static EventsDatabase eventsDatabase;
    private static ServerDatabase serverDatabase;


    public static EventsDatabase getEventsDatabase() {
        return eventsDatabase;
    }

    public static IThreadsServer getThreadsServer() {
        return threadsServer;
    }

    public static String getDonationsAddress() {
        return BuildConfig.DONATION_ADDRESS;
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

    public static void init() {
        new java.lang.Thread(new Runnable() {
            public void run() {
                Application.getCertificate();
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
        serverDatabase = Room.databaseBuilder(this,
                ServerDatabase.class,
                ServerDatabase.class.getSimpleName()).fallbackToDestructiveMigration().build();

        TransactionDatabase transactionDatabase = Room.databaseBuilder(this,
                TransactionDatabase.class,
                TransactionDatabase.class.getSimpleName()).fallbackToDestructiveMigration().build();


        eventsDatabase = Room.inMemoryDatabaseBuilder(this,
                EventsDatabase.class).build();

        threadsServer = ThreadsServer.createThreadServer(this,
                transactionDatabase, eventsDatabase);

        init();


        Log.e(TAG, "...... start application");

    }
}
