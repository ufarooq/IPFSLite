package threads.server;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;

import threads.iri.ITangleDaemon;
import threads.iri.daemon.ServerVisibility;
import threads.iri.daemon.TangleListener;
import threads.iri.room.TangleDatabase;
import threads.iri.server.ServerConfig;
import threads.iri.tangle.ITangleServer;
import threads.iri.tangle.Pair;
import threads.iri.tangle.TangleServer;
import threads.iri.task.FinishResponse;

public class RestartDaemonTask extends AsyncTask<Void, Void, Void> {
    private static final String TAG = "RestartDaemonTask";

    private final Context context;
    private final FinishResponse response;

    public RestartDaemonTask(@NonNull Context context, @NonNull FinishResponse response) {
        this.response = response;
        this.context = context;
    }

    @Override
    protected void onPostExecute(Void result) {

        response.finish();
    }

    @Override
    protected Void doInBackground(Void... params) {
        try {

            ITangleDaemon tangleDaemon = Application.getTangleDaemon();

            if (tangleDaemon.isDaemonRunning()) {
                tangleDaemon.shutdown();
            }

            ServerConfig serverConfig = Application.getServerConfig(context);

            ITangleServer tangleServer = TangleServer.getTangleServer(serverConfig);
            TangleDatabase tangleDatabase = Application.getTangleDatabase();

            Pair<ServerConfig, ServerVisibility> pair =
                    ITangleDaemon.getDaemonConfig(context, tangleDaemon);
            ServerConfig daemonConfig = pair.first;

            tangleDaemon.start(
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
