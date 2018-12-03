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

import threads.iota.server.Certificate;
import threads.server.daemon.IThreadsConfig;
import threads.server.daemon.IThreadsServer;


public class DaemonService extends Service {
    public static final int NOTIFICATION_ID = 999;
    public static final String ACTION_START_DAEMON_SERVICE = "ACTION_START_DAEMON_SERVICE";
    public static final String ACTION_STOP_DAEMON_SERVICE = "ACTION_STOP_DAEMON_SERVICE";
    public static final String ACTION_RESTART_DAEMON_SERVICE = "ACTION_RESTART_DAEMON_SERVICE";
    private static final String TAG = DaemonService.class.getSimpleName();


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

            switch (action) {
                case ACTION_RESTART_DAEMON_SERVICE:
                    restartDaemonService();
                    Toast.makeText(getApplicationContext(), "Daemon service is restarted.", Toast.LENGTH_LONG).show();
                    break;
                case ACTION_START_DAEMON_SERVICE:
                    startDaemonService();
                    Toast.makeText(getApplicationContext(), "Daemon service is started.", Toast.LENGTH_LONG).show();
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
        builder.setContentText(getString(R.string.daemon_port_text));
        builder.setSmallIcon(R.drawable.graphql);
        builder.setGroup(Application.GROUP_ID);
        builder.setGroupSummary(true);
        builder.setPriority(Notification.PRIORITY_MAX);

        // Create an explicit intent for an Activity in your app
        Intent defaultIntent = new Intent(getApplicationContext(), MainActivity.class);
        int requestID = (int) System.currentTimeMillis();
        defaultIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent defaultPendingIntent = PendingIntent.getActivity(
                getApplicationContext(), requestID, defaultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(defaultPendingIntent);


        Notification notification = builder.build();

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, notification);
        return notification;
    }

    private void restartDaemonService() {
        long start = System.currentTimeMillis();

        try {
            new java.lang.Thread(new Runnable() {
                public void run() {
                    IThreadsServer threadsServer = Application.getThreadsServer();

                    if (threadsServer.isRunning()) {
                        threadsServer.shutdown();
                    }
                    Certificate certificate = Application.getCertificate();
                    IThreadsConfig threadsConfig = IThreadsServer.getHttpsThreadsConfig(
                            getApplicationContext(),
                            Application.getCertificate());


                    Log.e(TAG, "Daemon Prot : " + IThreadsServer.HTTPS_PROTOCOL);
                    Log.e(TAG, "Daemon Host : " + threadsConfig.getHostname());
                    Log.e(TAG, "Daemon Port : " + threadsConfig.getPort());
                    Log.e(TAG, "Daemon Cert : " + certificate.getShaHash());

                    threadsServer.start(threadsConfig);
                }
            }).start();
        } catch (Throwable e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        } finally {
            Log.e(TAG, " finish running [" + (System.currentTimeMillis() - start) + "]...");
        }
    }

    private void startDaemonService() {
        long start = System.currentTimeMillis();

        try {
            new Thread(new Runnable() {
                public void run() {


                    IThreadsServer threadsServer = Application.getThreadsServer();
                    if (!threadsServer.isRunning()) {
                        Certificate certificate = Application.getCertificate();

                        IThreadsConfig threadsConfig = IThreadsServer.getHttpsThreadsConfig(
                                getApplicationContext(),
                                Application.getCertificate());
                        Log.e(TAG, "Daemon Prot : " + IThreadsServer.HTTPS_PROTOCOL);
                        Log.e(TAG, "Daemon Host : " + threadsConfig.getHostname());
                        Log.e(TAG, "Daemon Port : " + threadsConfig.getPort());
                        Log.e(TAG, "Daemon Cert : " + certificate.getShaHash());

                        threadsServer.start(threadsConfig);
                    }
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
            new java.lang.Thread(new Runnable() {
                public void run() {
                    IThreadsServer threadsServer = Application.getThreadsServer();
                    if (threadsServer.isRunning()) {
                        threadsServer.shutdown();
                        Log.i(TAG, "Daemon is shutting down ...");
                    }
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
