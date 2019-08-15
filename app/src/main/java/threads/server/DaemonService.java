package threads.server;

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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import threads.core.GatewayService;
import threads.core.Network;

import static androidx.core.util.Preconditions.checkNotNull;


public class DaemonService extends Service {
    public static final AtomicBoolean DAEMON_RUNNING = new AtomicBoolean(false);

    private static final String HIGH_CHANNEL_ID = "HIGH_CHANNEL_ID";
    private static final int NOTIFICATION_ID = 998;
    private static final String TAG = DaemonService.class.getSimpleName();
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                if (Network.isConnected(context)) {
                    JobServicePeers.peers(getApplicationContext());
                }
            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }
        }
    };


    private Future workService;
    private Future slowService;
    private Future fastService;
    private Future pinService;


    public static void invoke(@NonNull Context context) {
        checkNotNull(context);
        try {
            Intent intent = new Intent(context, DaemonService.class);
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
        if (DAEMON_RUNNING.get()) {

            startForeground(NOTIFICATION_ID, buildNotification());
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            registerReceiver(broadcastReceiver, intentFilter);
            run();
        } else {
            try {
                stopForeground(true);
                unregisterReceiver(broadcastReceiver);

                if (workService != null) {
                    workService.cancel(true);
                }
                if (slowService != null) {
                    slowService.cancel(true);
                }
                if (fastService != null) {
                    fastService.cancel(true);
                }
                if (pinService != null) {
                    pinService.cancel(true);
                }
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

    private void run() {

        ExecutorService fast = Executors.newSingleThreadExecutor();
        fastService = fast.submit(() -> {
            try {
                threads.server.Service.getInstance(getApplicationContext());

                while (DAEMON_RUNNING.get()) {
                    java.lang.Thread.sleep(TimeUnit.SECONDS.toMillis(30));
                    if (threads.server.Service.isReceiveNotificationsEnabled(
                            getApplicationContext())) {
                        NotificationService.notifications(getApplicationContext());
                    }

                    Log.e(TAG, "Peers : " +
                            GatewayService.evaluatePeers(getApplicationContext(), false));
                }

            } catch (InterruptedException e) {
                Log.e(TAG, "Daemon Fast Service has been successfully stopped");
            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }

        });


        ExecutorService work = Executors.newSingleThreadExecutor();
        workService = work.submit(() -> {
            try {
                threads.server.Service.getInstance(getApplicationContext());

                while (DAEMON_RUNNING.get()) {
                    java.lang.Thread.sleep(TimeUnit.MINUTES.toMillis(5));
                    JobServiceFindPeers.findPeers(getApplicationContext());
                }

            } catch (InterruptedException e) {
                Log.e(TAG, "Daemon Work Service has been successfully stopped");
            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }

        });

        ExecutorService clean = Executors.newSingleThreadExecutor();
        slowService = clean.submit(() -> {
            try {
                threads.server.Service.getInstance(getApplicationContext());

                while (DAEMON_RUNNING.get()) {
                    java.lang.Thread.sleep(TimeUnit.HOURS.toMillis(12));
                    CleanupService.cleanup(getApplicationContext());
                    ContentsService.contents(getApplicationContext());
                }

            } catch (InterruptedException e) {
                Log.e(TAG, "Daemon Slow Service has been successfully stopped");
            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }

        });


        ExecutorService pin = Executors.newSingleThreadExecutor();
        pinService = pin.submit(() -> {
            try {
                threads.server.Service.getInstance(getApplicationContext());
                int time = threads.server.Service.getPinServiceTime(getApplicationContext());

                while (DAEMON_RUNNING.get() && time > 0) {
                    java.lang.Thread.sleep(TimeUnit.HOURS.toMillis(time));
                    PinService.pin(getApplicationContext());
                }

            } catch (InterruptedException e) {
                Log.e(TAG, "Daemon Slow Service has been successfully stopped");
            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }

        });

    }

    private Notification buildNotification() {

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(),
                HIGH_CHANNEL_ID);

        builder.setContentTitle(getString(R.string.daemon_title));
        builder.setSmallIcon(R.drawable.server_network);
        builder.setPriority(NotificationManager.IMPORTANCE_MAX);

        Intent defaultIntent = new Intent(getApplicationContext(), LoginActivity.class);
        int requestID = (int) System.currentTimeMillis();
        defaultIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent defaultPendingIntent = PendingIntent.getActivity(
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
