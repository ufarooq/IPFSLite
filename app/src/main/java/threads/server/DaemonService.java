package threads.server;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import threads.core.IThreadsAPI;
import threads.core.Preferences;
import threads.core.Singleton;
import threads.core.api.MessageKind;
import threads.core.api.User;
import threads.core.api.UserStatus;
import threads.ipfs.IPFS;
import threads.ipfs.api.PID;

import static com.google.common.base.Preconditions.checkNotNull;


public class DaemonService extends Service {
    public static final String ACTION_START_DAEMON_SERVICE = "ACTION_START_DAEMON_SERVICE";
    public static final String ACTION_STOP_DAEMON_SERVICE = "ACTION_STOP_DAEMON_SERVICE";
    private static final int NOTIFICATION_ID = 999;
    private static final String TAG = DaemonService.class.getSimpleName();
    public static final AtomicBoolean DAEMON_RUNNING = new AtomicBoolean(false);
    public static boolean configHasChanged = false;




    @Override
    public void onCreate() {
        super.onCreate();

        Application.createChannel(getApplicationContext());

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
                    startForeground(NOTIFICATION_ID, buildNotification());
                    startDaemonService();
                    break;
                case ACTION_STOP_DAEMON_SERVICE:
                    stopDaemonService();
                    Toast.makeText(getApplicationContext(),
                            R.string.damon_service_shutdown, Toast.LENGTH_LONG).show();
                    break;
            }
        }

        return START_NOT_STICKY;
    }

    private Notification buildNotification() {

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                getApplicationContext(),
                Application.CHANNEL_ID);

        builder.setContentTitle(getString(R.string.daemon_title));
        builder.setSmallIcon(R.drawable.server_network);
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
        final IPFS ipfs = Singleton.getInstance().getIpfs();
        final IThreadsAPI threadsApi = Singleton.getInstance().getThreadsAPI();
        if (ipfs != null) {
            new Thread(() -> {
                try {
                    ipfs.init(Preferences.getProfile(getApplicationContext()), configHasChanged,
                            Application.isQUICEnabled(getApplicationContext()));
                    ipfs.daemon(Application.isPubsubEnabled(getApplicationContext()));

                    DAEMON_RUNNING.set(true);


                    threadsApi.storeEvent(
                            threadsApi.createEvent(Preferences.IPFS_SERVER_ONLINE_EVENT, ""));



                } catch (Throwable e) {
                    Log.e(TAG, e.getLocalizedMessage(), e);
                    threadsApi.storeEvent(
                            threadsApi.createEvent(Preferences.IPFS_SERVER_OFFLINE_EVENT, ""));
                    stopForeground(true);
                    stopSelf();
                }

                try {
                    while (DAEMON_RUNNING.get()) {
                        List<User> users = threadsApi.getUsers();
                        for (User user : users) {

                            if (ipfs.swarm_is_connected(PID.create(user.getPid()))) {
                                threadsApi.setStatus(user, UserStatus.ONLINE);
                            } else {
                                threadsApi.setStatus(user, UserStatus.OFFLINE);
                            }

                        }
                        Thread.sleep(10000);
                    }
                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
                }
            }).start();
        }

    }

    private void stopDaemonService() {
        long start = System.currentTimeMillis();

        try {
            IPFS ipfs = Singleton.getInstance().getIpfs();
            if (ipfs != null) {
                new Thread(() -> {
                    ipfs.shutdown();
                    DAEMON_RUNNING.set(false);

                    IThreadsAPI threadsApi = Singleton.getInstance().getThreadsAPI();
                    threadsApi.storeEvent(
                            threadsApi.createEvent(Preferences.IPFS_SERVER_OFFLINE_EVENT, ""));


                    threadsApi.storeMessage(threadsApi.createMessage(MessageKind.INFO,
                            getApplicationContext().getString(R.string.server_shutdown),
                            System.currentTimeMillis()));


                }).start();
            }
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
