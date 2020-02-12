package threads.server.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import threads.server.R;
import threads.server.work.ConnectPeersWorker;

import static androidx.core.util.Preconditions.checkNotNull;


public class DaemonService extends Service {

    private static final String HIGH_CHANNEL_ID = "HIGH_CHANNEL_ID";
    private static final int NOTIFICATION_ID = 998;
    private static final String TAG = DaemonService.class.getSimpleName();
    private static final String START_DAEMON = "START_DAEMON";
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                BootstrapService.bootstrap(getApplicationContext());
                ConnectPeersWorker.connect(getApplicationContext(), 30);
            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }
        }
    };

    public static void invoke(@NonNull Context context, boolean startDaemon) {
        checkNotNull(context);
        try {
            Intent intent = new Intent(context, DaemonService.class);
            intent.putExtra(START_DAEMON, startDaemon);
            ContextCompat.startForegroundService(context, intent);
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    private static void createChannel(@NonNull Context context) {
        checkNotNull(context);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            try {
                CharSequence name = context.getString(R.string.daemon_channel_name);
                String description = context.getString(R.string.daemon_channel_description);
                int importance = NotificationManager.IMPORTANCE_HIGH;
                NotificationChannel mChannel = new NotificationChannel(HIGH_CHANNEL_ID, name, importance);
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

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        checkNotNull(intent);
        if (intent.getBooleanExtra(START_DAEMON, false)) {

            startForeground(NOTIFICATION_ID, buildNotification());
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            registerReceiver(broadcastReceiver, intentFilter);


        } else {
            Toast.makeText(getApplicationContext(), R.string.daemon_shutdown,
                    Toast.LENGTH_LONG).show();
            try {
                stopForeground(true);
                unregisterReceiver(broadcastReceiver);

            } finally {
                stopSelf();
            }
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        try {
            createChannel(getApplicationContext());
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
        super.onCreate();
    }


    private Notification buildNotification() {

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(),
                HIGH_CHANNEL_ID);

        builder.setContentTitle(getString(R.string.daemon_title));
        builder.setSmallIcon(R.drawable.server_network);
        builder.setPriority(NotificationManager.IMPORTANCE_MAX);

        Intent defaultIntent = new Intent(getApplicationContext(), DaemonService.class);
        defaultIntent.putExtra(START_DAEMON, false);
        int requestID = (int) System.currentTimeMillis();
        PendingIntent defaultPendingIntent = PendingIntent.getService(
                getApplicationContext(), requestID, defaultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(defaultPendingIntent);

        Notification notification = builder.build();

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, notification);
        }
        return notification;
    }


}
