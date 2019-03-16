package threads.server;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import threads.core.Preferences;
import threads.core.Singleton;
import threads.core.THREADS;
import threads.core.api.MessageKind;
import threads.core.api.User;
import threads.core.api.UserStatus;
import threads.core.api.UserType;
import threads.ipfs.IPFS;
import threads.ipfs.api.CID;
import threads.ipfs.api.PID;
import threads.ipfs.api.SwarmConfig;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;


public class DaemonService extends Service {
    public static final AtomicBoolean DAEMON_RUNNING = new AtomicBoolean(false);
    private static final String ACTION_START_DAEMON_SERVICE = "ACTION_START_DAEMON_SERVICE";
    private static final String ACTION_STOP_DAEMON_SERVICE = "ACTION_STOP_DAEMON_SERVICE";
    private static final String HIGH_CHANNEL_ID = "HIGH_CHANNEL_ID";
    private static final int NOTIFICATION_ID = 998;
    private static final String TAG = DaemonService.class.getSimpleName();


    public static void startDaemon(@NonNull Context context) {
        checkNotNull(context);
        Intent intent = new Intent(context, DaemonService.class);
        intent.setAction(DaemonService.ACTION_START_DAEMON_SERVICE);
        ContextCompat.startForegroundService(context, intent);
    }

    public static void stopDaemon(@NonNull Context context) {
        checkNotNull(context);
        Intent intent = new Intent(context, DaemonService.class);
        intent.setAction(DaemonService.ACTION_STOP_DAEMON_SERVICE);
        ContextCompat.startForegroundService(context, intent);
    }


    public static void createChannel(@NonNull Context context) {
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
                            R.string.daemon_shutdown, Toast.LENGTH_LONG).show();
                    break;
            }
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        createChannel(getApplicationContext());
        super.onCreate();
    }

    private Notification buildNotification() {

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(),
                HIGH_CHANNEL_ID);

        builder.setContentTitle(getString(R.string.daemon_title));
        builder.setSmallIcon(R.drawable.server_network);
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
        final THREADS threadsApi = Singleton.getInstance().getThreads();
        if (ipfs != null) {
            new Thread(() -> {
                try {

                    boolean hasConfigChanged = Application.hasConfigChanged(getApplicationContext());

                    ipfs.supportQUIC(Preferences.isQUICEnabled(getApplicationContext()),
                            Preferences.getSwarmPort(getApplicationContext()));

                    SwarmConfig swarmConfig = ipfs.getSwarm();
                    swarmConfig.setEnableAutoRelay(
                            Preferences.isAutoRelayEnabled(getApplicationContext()));

                    ipfs.init(Preferences.getProfile(getApplicationContext()), false,
                            hasConfigChanged, null, null,
                            null, null, swarmConfig);

                    Application.setConfigChanged(getApplicationContext(), false);

                    ipfs.daemon(Preferences.isPubsubEnabled(getApplicationContext()));

                    DAEMON_RUNNING.set(true);


                    threadsApi.storeEvent(
                            threadsApi.createEvent(Preferences.IPFS_SERVER_ONLINE_EVENT, ""));

                } catch (Throwable e) {
                    threadsApi.storeEvent(
                            threadsApi.createEvent(Preferences.IPFS_SERVER_OFFLINE_EVENT, ""));
                    Preferences.evaluateException(Preferences.IPFS_START_FAILURE, e);
                    stopForeground(true);
                    stopSelf();
                }

                new Thread(this::startPubsub).start();

                //new Thread(this::startRelay).start();

                new Thread(this::startPeers).start();

            }).start();
        }

    }

    private void startPeers() {
        try {
            while (DAEMON_RUNNING.get()) {
                threads.server.Service.connectPeers(getApplicationContext());
                Thread.sleep(15000); // TODO optimize here when no network
            }
        } catch (Throwable e) {
            // IGNORE exception occurs when daemon is shutdown
        } finally {
            threads.server.Service.connectPeers(getApplicationContext());
        }
    }


    private void startPubsub() {
        final IPFS ipfs = Singleton.getInstance().getIpfs();
        if (ipfs != null) {
            if (DAEMON_RUNNING.get()) {
                if (Preferences.isPubsubEnabled(getApplicationContext())) {
                    final PID pid = Preferences.getPID(getApplicationContext());
                    checkNotNull(pid);
                    checkArgument(!pid.getPid().isEmpty());
                    try {
                        pubsubDaemon(ipfs, pid);
                    } catch (Throwable e) {
                        // IGNORE exception occurs when daemon is shutdown
                    }
                }
            }
        }
    }

    private void pubsubDaemon(@NonNull IPFS ipfs,
                              @NonNull PID pid) throws Exception {
        checkNotNull(ipfs);
        checkNotNull(pid);

        final THREADS threadsAPI = Singleton.getInstance().getThreads();


        if (Preferences.DEBUG_MODE) {
            Log.e(TAG, "Pubsub Daemon :" + pid.getPid());
        }

        ipfs.pubsub_sub(pid.getPid(), false, (message) -> {

            try {
                PID senderPid = PID.create(message.getSenderPid());

                User sender = threadsAPI.getUserByPID(senderPid);
                if (sender == null) {

                    // check content if might be a name
                    String name = senderPid.getPid();

                    try {
                        String alias = message.getMessage().trim();
                        if (alias.length() < name.length()) { // small shitty security
                            name = alias;
                        }
                    } catch (Throwable e) {
                        // ignore exception
                    }

                    // create a new user which is blocked (User has to unblock and verified the user)
                    byte[] data = THREADS.getImage(getApplicationContext(),
                            name, R.drawable.server_network);
                    CID image = ipfs.add(data, true);
                    sender = threadsAPI.createUser(senderPid,
                            senderPid.getPid(),
                            name, UserType.UNKNOWN, image, null);
                    sender.setStatus(UserStatus.BLOCKED);
                    threadsAPI.storeUser(sender);


                    Preferences.error(getString(R.string.user_connect_try, name));

                } else {
                    if (!threadsAPI.isAccountBlocked(senderPid)) {

                        String multihash = message.getMessage().trim();
                        threads.server.Service.downloadMultihash(
                                getApplicationContext(), senderPid, multihash);
                    }
                }
            } catch (Throwable e) {
                if (Preferences.DEBUG_MODE) {
                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
                }
            } finally {
                if (Preferences.DEBUG_MODE) {
                    Log.e(TAG, "Received : " + message.toString());
                }
            }


        });

    }

    private void stopDaemonService() {

        try {
            IPFS ipfs = Singleton.getInstance().getIpfs();
            if (ipfs != null) {
                new Thread(() -> {
                    ipfs.shutdown();
                    DAEMON_RUNNING.set(false);

                    THREADS threadsApi = Singleton.getInstance().getThreads();
                    threadsApi.storeEvent(
                            threadsApi.createEvent(Preferences.IPFS_SERVER_OFFLINE_EVENT, ""));


                    threadsApi.storeMessage(threadsApi.createMessage(MessageKind.INFO,
                            getApplicationContext().getString(R.string.daemon_shutdown),
                            System.currentTimeMillis()));


                }).start();
            }
        } catch (Throwable e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        } finally {
            // Stop foreground service and remove the notification.
            stopForeground(true);

            new Thread(() -> threads.server.Service.connectPeers(getApplicationContext())).start();

            new Thread(() -> threads.server.Service.closeTasks(getApplicationContext())).start();

            // Stop the foreground service.
            stopSelf();

        }
    }

}
