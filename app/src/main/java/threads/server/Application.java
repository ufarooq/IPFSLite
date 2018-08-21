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

import com.google.gson.Gson;

import java.util.Hashtable;

import threads.iri.room.TangleDatabase;
import threads.iri.tangle.IServerConfig;

public class Application extends android.app.Application {

    public static final String CHANNEL_ID = "IRI_SERVER_CHANGEL_ID";
    private static final String TAG = "Application";
    private static final String TANGLE_DATABASE = "TANGLE_DATABASE";

    private static TangleDatabase tangleDatabase;
    private static MessageDatabase messageDatabase;

    public static MessageDatabase getMessagesDatabase() {
        return messageDatabase;
    }

    public static TangleDatabase getTangleDatabase() {
        return tangleDatabase;
    }

    public static final int QR_CODE_SIZE = 800;
    @NonNull
    private final static Hashtable<String, Bitmap> generalHashtable = new Hashtable<>();

    public static Bitmap getBitmap(@NonNull IServerConfig serverConfig) {

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

    @Override
    public void onTerminate() {
        super.onTerminate();
        Log.e(TAG, "...... end application");
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

    @Override
    public void onCreate() {
        super.onCreate();

        messageDatabase = Room.inMemoryDatabaseBuilder(this, MessageDatabase.class).build();
        tangleDatabase = Room.databaseBuilder(this,
                TangleDatabase.class, TANGLE_DATABASE).fallbackToDestructiveMigration().build();

        new java.lang.Thread(new Runnable() {
            public void run() {
                Application.getMessagesDatabase().insertMessage("\nWelcome to the IRI android daemon.");
                Application.getMessagesDatabase().insertMessage("Please feel free to start the daemon ....\n\n");

            }
        }).start();



        Log.e(TAG, "...... start application");

    }


}
