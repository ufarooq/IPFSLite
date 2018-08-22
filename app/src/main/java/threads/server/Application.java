package threads.server;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.arch.persistence.room.Room;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.gson.Gson;

import java.util.Hashtable;

import threads.iri.ITangleDaemon;
import threads.iri.room.TangleDatabase;
import threads.iri.server.ServerConfig;

import static com.google.common.base.Preconditions.checkNotNull;

public class Application extends android.app.Application {

    public static final String LOCALHOST = "localhost";
    public static final String TANGLE_PROTOCOL = "https";
    public static final String TANGLE_HOST = LOCALHOST;
    public static final String TANGLE_PORT = String.valueOf(ITangleDaemon.TCP_DAEMON_PORT);
    public static final String TANGLE_CERT = "";
    public static final String CHANNEL_ID = "IRI_SERVER_CHANGEL_ID";
    public static final int QR_CODE_SIZE = 800;
    private static final String TAG = "Application";
    private static final String TANGLE_DATABASE = "TANGLE_DATABASE";
    @NonNull
    private final static Hashtable<String, Bitmap> generalHashtable = new Hashtable<>();
    public static boolean TANGLE_LOCAL_POW = false;
    private static TangleDatabase tangleDatabase;
    private static DaemonDatabase daemonDatabase;

    public static DaemonDatabase getDaemonDatabase() {
        return daemonDatabase;
    }

    public static TangleDatabase getTangleDatabase() {
        return tangleDatabase;
    }

    public static boolean isNetworkAvailable(@NonNull Context context) {
        checkNotNull(context);
        try {
            ConnectivityManager connectivityManager
                    = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
            throw e;
        }
    }

    public static ServerConfig getServerConfig(@NonNull Context context) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences("SERVERCONFIG", Context.MODE_PRIVATE);
        String protocol = sharedPref.getString("protocol", TANGLE_PROTOCOL);
        String host = sharedPref.getString("host", TANGLE_HOST);
        String port = sharedPref.getString("port", TANGLE_PORT);
        boolean isLocalPow = sharedPref.getBoolean("TANGLE_LOCAL_POW", TANGLE_LOCAL_POW);
        String cert = sharedPref.getString("cert", TANGLE_CERT);
        return ServerConfig.createServerConfig(protocol, host, port, cert, isLocalPow);
    }

    public static void setServerConfig(@NonNull Context context, @NonNull ServerConfig serverConfig) {
        checkNotNull(context);
        checkNotNull(serverConfig);
        SharedPreferences sharedPref = context.getSharedPreferences("SERVERCONFIG", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("protocol", serverConfig.getProtocol());
        editor.putString("host", serverConfig.getHost());
        editor.putString("port", serverConfig.getPort());
        editor.putString("cert", serverConfig.getCert());
        editor.putBoolean("TANGLE_LOCAL_POW", serverConfig.isLocalPow());

        editor.apply();
    }

    public static Bitmap getBitmap(@NonNull ServerConfig serverConfig) {

        String qrCode = "";
        Gson gson = new Gson();

        qrCode = gson.toJson(serverConfig);

        if (generalHashtable.containsKey(qrCode)) {
            return generalHashtable.get(qrCode);
        }
        try {


            Bitmap bitmap = net.glxn.qrgen.android.QRCode.from(qrCode).
                    withSize(Application.QR_CODE_SIZE, Application.QR_CODE_SIZE).bitmap();


            generalHashtable.put(qrCode, bitmap);
            return bitmap;
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
        return null;
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
                Application.getDaemonDatabase().insertMessage("\nWelcome to the IRI android daemon.");
                Application.getDaemonDatabase().insertMessage("Please feel free to start the daemon ....\n\n");

            }
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

        daemonDatabase = Room.inMemoryDatabaseBuilder(this, DaemonDatabase.class).build();
        tangleDatabase = Room.databaseBuilder(this,
                TangleDatabase.class, TANGLE_DATABASE).fallbackToDestructiveMigration().build();


        initMessageDatabase();


        Log.e(TAG, "...... start application");

    }


}
