package threads.server;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import threads.ipfs.IPFS;
import threads.ipfs.api.PID;
import threads.ipfs.api.Profile;

import static com.google.common.base.Preconditions.checkNotNull;


public class DaemonService extends Service {
    public static final String ACTION_START_DAEMON_SERVICE = "ACTION_START_DAEMON_SERVICE";
    public static final String ACTION_STOP_DAEMON_SERVICE = "ACTION_STOP_DAEMON_SERVICE";
    private static final int NOTIFICATION_ID = 999;
    private static final String TAG = DaemonService.class.getSimpleName();
    private static boolean ipfsRunning = false;

    public static boolean isIpfsRunning() {
        return ipfsRunning;
    }

    public static void setIpfsRunning(boolean running) {
        ipfsRunning = running;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Application.createChannel(getApplicationContext());

        Notification notification = buildNotification();

        // Start foreground service.
        startForeground(NOTIFICATION_ID, notification);

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null) {
            String action = intent.getAction();
            checkNotNull(action);
            switch (action) {
                case ACTION_START_DAEMON_SERVICE:
                    startDaemonService();
                    break;
                case ACTION_STOP_DAEMON_SERVICE:
                    stopDaemonService();
                    Toast.makeText(getApplicationContext(), "Daemon service is stopped.", Toast.LENGTH_LONG).show();
                    break;
            }
        }

        return START_NOT_STICKY;
    }

    private Notification buildNotification() {

        // Create notification builder.
        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                getApplicationContext(),
                Application.CHANNEL_ID);

        builder.setContentTitle(getString(R.string.daemon_title));
        builder.setSmallIcon(R.drawable.graphql);
        builder.setGroup(Application.GROUP_ID);
        builder.setGroupSummary(true);
        builder.setPriority(NotificationManager.IMPORTANCE_MAX);

        // Create an explicit intent for an Activity in your app
        Intent defaultIntent = new Intent(getApplicationContext(), MainActivity.class);
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


    private void startDaemonService() {
        long start = System.currentTimeMillis();

        try {
            new Thread(() -> {

                try {
                    IPFS ipfs = Application.getIpfs();
                    if (ipfs != null) {
                        ipfs.start(Profile.LOW_POWER, false, 30000L);
                        PID pid = ipfs.getPeersID();
                        Application.setPid(getApplicationContext(), pid.getPid());
                        setIpfsRunning(true);
                        Event event = Application.getEventsDatabase().createEvent(
                                Application.SERVER_ONLINE_EVENT);
                        Application.getEventsDatabase().insertEvent(event);
                    }


                } catch (Throwable e) {
                    Log.e(TAG, e.getLocalizedMessage(), e);
                }

            }).start();

        } catch (Throwable e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        } finally {
            Log.e(TAG, " finish running [" + (System.currentTimeMillis() - start) + "]...");
        }
    }

    private void stopDaemonService() {
        long start = System.currentTimeMillis();

        try {
            new java.lang.Thread(() -> {
                IPFS ipfs = Application.getIpfs();
                if (ipfs != null) {
                    ipfs.shutdown();
                    setIpfsRunning(false);
                    Application.getEventsDatabase().insertMessage(
                            MessageKind.INFO,
                            getApplicationContext().getString(R.string.server_shutdown));
                    Event event = Application.getEventsDatabase().createEvent(
                            Application.SERVER_OFFLINE_EVENT);
                    Application.getEventsDatabase().insertEvent(event);
                }

            }).start();

        } catch (Throwable e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        } finally {
            // Stop foreground service and remove the notification.
            stopForeground(true);

            // Stop the foreground service.
            stopSelf();

            Log.e(TAG, " finish running [" + (System.currentTimeMillis() - start) + "]...");
        }
    }

}
