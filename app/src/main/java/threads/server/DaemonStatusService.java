package threads.server;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;

import threads.iri.ITangleDaemon;
import threads.iri.daemon.TangleDaemon;
import threads.iri.server.ServerConfig;
import threads.iri.tangle.TangleUtils;

public class DaemonStatusService extends AsyncTask<Void, Void, Void> {
    private static final String TAG = "DaemonStatusService";


    private final Context context;

    public DaemonStatusService(@NonNull Context context) {
        this.context = context;
    }


    @Override
    protected Void doInBackground(Void... params) {

        try {

            DaemonDatabase daemonDatabase = Application.getDaemonDatabase();
            threads.server.Status status = daemonDatabase.getStatus();
            ITangleDaemon tangleDaemon = TangleDaemon.getInstance();
            if (tangleDaemon.isDaemonRunning()) {
                status.setServerRunning(true);
                if (Application.isNetworkAvailable(context)) {
                    status.setNetworkAvailable(true);
                    ServerConfig serverConfig = Application.getServerConfig(context);
                    if (TangleUtils.isReachable(serverConfig)) {
                        status.setServerReachable(true);
                    }
                }
            }
            daemonDatabase.updateStatus(status);


        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage());
        }
        return null;
    }
}
