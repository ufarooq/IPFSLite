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

public class Application extends android.app.Application {

    public static final String CHANNEL_ID = "IRI_SERVER_CHANGEL_ID";
    private static final String TAG = "Application";
    private static final String TANGLE_DATABASE = "TANGLE_DATABASE";

    private static TangleDatabase database;

    public static TangleDatabase getThreadsTangleDatabase() {
        return database;
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

        database = Room.databaseBuilder(this,
                TangleDatabase.class, TANGLE_DATABASE).fallbackToDestructiveMigration().build();

        Log.e(TAG, "...... start application");

    }


}
