package threads.server;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import threads.iri.ITangleDaemon;
import threads.iri.Logs;
import threads.iri.ServerConfig;
import threads.iri.daemon.TangleDaemon;
import threads.iri.daemon.TangleListener;
import threads.iri.room.TangleDatabase;
import threads.iri.tangle.ITangleServer;
import threads.iri.tangle.TangleServer;

public class DaemonService extends Service {
    public static final int NOTIFICATION_ID = 999;
    public static final String ACTION_START_DAEMON_SERVICE = "ACTION_START_DAEMON_SERVICE";
    public static final String ACTION_STOP_DAEMON_SERVICE = "ACTION_STOP_DAEMON_SERVICE";
    private static final String TAG = DaemonService.class.getSimpleName();

    public DaemonService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();

            switch (action) {
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
        return super.onStartCommand(intent, flags, startId);
    }

    private void startDaemonService() {
        long start = System.currentTimeMillis();

        try {

            Application.createChannel(getApplicationContext());
            // Create notification default intent.
            Intent intent = new Intent();
            PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);


            // Create notification builder.
            NotificationCompat.Builder builder = new NotificationCompat.Builder(
                    getApplicationContext(),
                    Application.CHANNEL_ID);

            int port = ITangleDaemon.TCP_DAEMON_PORT;

            builder.setContentTitle(getString(R.string.daemon_title));
            builder.setContentText(getString(R.string.daemon_text));
            builder.setStyle(new NotificationCompat.BigTextStyle()
                    .bigText(getString(R.string.daemon_long_text, String.valueOf(port))));
            builder.setWhen(System.currentTimeMillis());
            builder.setSmallIcon(R.mipmap.ic_launcher);
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


            // Start foreground service.
            startForeground(999, notification);

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

    public class StartDaemonTask extends AsyncTask<Void, Void, Void> {
        private static final String TAG = "StartDaemonTask";

        @Override
        protected Void doInBackground(Void... addresses) {
            try {
                TangleDatabase tangleDatabase = Application.getTangleDatabase();

                ITangleDaemon daemon = TangleDaemon.getInstance();
                if (!daemon.isDaemonRunning()) {
                    ServerConfig serverConfig = ServerConfig.createServerConfig("https",
                            "nodes.iota.fm", "443", "", false);

                    ITangleServer tangleServer = TangleServer.getTangleServer(serverConfig);


                    daemon.start(
                            getApplicationContext(),
                            tangleDatabase,
                            tangleServer,
                            new TangleListener(),
                            String.valueOf(ITangleDaemon.TCP_DAEMON_PORT),
                            false);


                } else {
                    Logs.i("Daemon is already running ...");
                }

            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage());
            }
            return null;
        }
    }

    public class StopDaemonTask extends AsyncTask<Void, Void, Void> {
        private static final String TAG = "StopDaemonTask";


        @Override
        protected Void doInBackground(Void... addresses) {
            try {
                TangleDatabase tangleDatabase =
                        Application.getTangleDatabase();
                ITangleDaemon daemon = TangleDaemon.getInstance();
                if (daemon.isDaemonRunning()) {
                    daemon.shutdown();
                    Logs.i("Daemon is shutting down ...");
                } else {
                    Logs.e("Daemon is not running ...");
                }
            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage());
            }
            return null;
        }
    }

}
