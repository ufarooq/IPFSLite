package threads.server;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;

import threads.iri.ITangleDaemon;
import threads.iri.daemon.TangleDaemon;
import threads.iri.tangle.IServerConfig;
import threads.iri.tangle.TangleServerConfig;
import threads.iri.tangle.TangleUtils;

public class DaemonCheckService extends AsyncTask<Void, Void, Boolean> {
    private static final String TAG = "DaemonCheckService";


    private final FinishResponse response;

    public DaemonCheckService(@NonNull FinishResponse response) {

        this.response = response;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        response.finish(result);
    }


    @Override
    protected Boolean doInBackground(Void... params) {
        try {

            ITangleDaemon daemon = TangleDaemon.getInstance();

            String publicIP = daemon.getPublicIP();

            IServerConfig serverConfig = daemon.getServerConfig();

            IServerConfig publicServerConfig = TangleServerConfig.createServerConfig(serverConfig.getProtocol(),
                    publicIP, serverConfig.getPort(), serverConfig.getCert(), serverConfig.isLocalPow());

            return TangleUtils.isReachable(publicServerConfig);

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage());
        }
        return false;
    }
}
