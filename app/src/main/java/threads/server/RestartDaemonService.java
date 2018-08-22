package threads.server;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;

import threads.iri.ITangleDaemon;
import threads.iri.Logs;
import threads.iri.daemon.TangleDaemon;
import threads.iri.daemon.TangleListener;
import threads.iri.room.TangleDatabase;
import threads.iri.server.ServerConfig;
import threads.iri.tangle.ITangleServer;
import threads.iri.tangle.TangleServer;

public class RestartDaemonService extends AsyncTask<Void, Void, Void> {
    private static final String TAG = "RestartDaemonService";

    private final Context context;

    public RestartDaemonService(@NonNull Context context) {
        this.context = context;
    }


    @Override
    protected Void doInBackground(Void... params) {
        try {


            ITangleDaemon tangleDaemon = TangleDaemon.getInstance();
            if (tangleDaemon.isDaemonRunning()) {
                tangleDaemon.shutdown();
                Logs.i("Daemon is shutting down ...");
            }
            Thread.sleep(2000);


            // TODO add much more server
            ServerConfig serverConfig = ServerConfig.createServerConfig("https",
                    "nodes.iota.fm", "443", "", false);

            ITangleServer tangleServer = TangleServer.getTangleServer(serverConfig);
            TangleDatabase tangleDatabase = Application.getTangleDatabase();

            ServerConfig daemonConfig = Application.getServerConfig(context);
            tangleDaemon.start(
                    context,
                    tangleDatabase,
                    tangleServer,
                    new TangleListener(),
                    daemonConfig.getPort(),
                    daemonConfig.isLocalPow());

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage());
        }
        return null;
    }
}
