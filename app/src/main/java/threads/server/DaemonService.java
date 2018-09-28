package threads.server;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import threads.iri.IThreadsServer;
import threads.iri.server.Server;
import threads.iri.server.ServerVisibility;
import threads.iri.tangle.ITangleServer;
import threads.iri.tangle.Pair;
import threads.iri.tangle.TangleDatabase;

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

        // Create notification default intent.
        Intent intent = new Intent();
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);

        // Create notification builder.
        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                getApplicationContext(),
                Application.CHANNEL_ID);

        builder.setContentTitle(getString(R.string.daemon_title));
        builder.setContentText(getString(R.string.daemon_port_text));
        builder.setStyle(new NotificationCompat.BigTextStyle()
                .bigText(getString(R.string.daemon_port_text)));
        builder.setWhen(System.currentTimeMillis());
        builder.setSmallIcon(R.drawable.graphql);
        // builder.setGroup(Application.GROUP_KEY_TRAVEL_TANGLE); TODO not working for all android versions
        Bitmap largeIconBitmap = Application.getBitmap(getApplicationContext(), R.drawable.server_network);

        builder.setLargeIcon(largeIconBitmap);
        // Make the notification max priority.
        builder.setPriority(Notification.PRIORITY_MAX);
        // Make head-up notification.
        builder.setFullScreenIntent(pendingIntent, true);

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
        RestartDaemonTask task = new RestartDaemonTask();
        task.execute();
    }

    private void startDaemonService() {
        long start = System.currentTimeMillis();

        try {
            StartDaemonTask task = new StartDaemonTask();
            task.execute();
        } catch (Throwable e) {
            Log.e(TAG, e.getLocalizedMessage());
        } finally {
            Log.e(TAG, " finish running [" + (System.currentTimeMillis() - start) + "]...");
        }
    }

    private void stopDaemonService() {
        long start = System.currentTimeMillis();

        try {

            StopDaemonTask task = new StopDaemonTask();
            task.execute();

        } catch (Throwable e) {
            Log.e(TAG, e.getLocalizedMessage());
        } finally {
            // Stop foreground service and remove the notification.
            stopForeground(true);

            // Stop the foreground service.
            stopSelf();

            Log.e(TAG, " finish running [" + (System.currentTimeMillis() - start) + "]...");
        }
    }


    public class RestartDaemonTask extends AsyncTask<Void, Void, Void> {
        private static final String TAG = "RestartDaemonTask";

        @Override
        protected Void doInBackground(Void... params) {
            try {

                IThreadsServer tangleDaemon = Application.getThreadsServer();

                if (tangleDaemon.isRunning()) {
                    tangleDaemon.shutdown();
                }

                ITangleServer tangleServer = Application.getTangleServer(getApplicationContext());
                TangleDatabase tangleDatabase = Application.getTangleDatabase();

                Pair<Server, ServerVisibility> pair =
                        IThreadsServer.getServer(getApplicationContext(), tangleDaemon);
                Server daemonConfig = pair.first;

                tangleDaemon.start(
                        tangleDatabase,
                        tangleServer,
                        daemonConfig.getPort());

            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage());
            }
            return null;
        }
    }

    public class StartDaemonTask extends AsyncTask<Void, Void, Void> {
        private static final String TAG = "StartDaemonTask";

        @Override
        protected Void doInBackground(Void... addresses) {
            try {
                TangleDatabase tangleDatabase = Application.getTangleDatabase();
                IThreadsServer daemon = Application.getThreadsServer();
                if (!daemon.isRunning()) {

                    ITangleServer tangleServer = Application.getTangleServer(getApplicationContext());


                    Server daemonServer = Application.getDaemonServer(getApplicationContext());

                    Log.e(TAG, "Daemon Host : " + daemonServer.getHost());
                    Log.e(TAG, "Daemon Port : " + daemonServer.getPort());
                    daemon.start(
                            tangleDatabase,
                            tangleServer,
                            daemonServer.getPort());

                } else {
                    Log.i(TAG, "Daemon is already running ...");
                }

            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }
            return null;
        }
    }

    public class StopDaemonTask extends AsyncTask<Void, Void, Void> {
        private static final String TAG = "StopDaemonTask";


        @Override
        protected Void doInBackground(Void... addresses) {
            try {
                IThreadsServer daemon = Application.getThreadsServer();
                if (daemon.isRunning()) {
                    daemon.shutdown();
                    Log.i(TAG, "Daemon is shutting down ...");
                } else {
                    Log.i(TAG, "Daemon is not running ...");
                }

            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }
            return null;
        }
    }

}
