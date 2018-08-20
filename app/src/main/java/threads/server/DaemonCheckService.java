package threads.server;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import threads.iri.ITangleDaemon;
import threads.iri.daemon.TangleDaemon;
import threads.iri.tangle.IServerConfig;
import threads.iri.tangle.TangleServerConfig;
import threads.iri.tangle.TangleUtils;

public class DaemonCheckService extends AsyncTask<Void, Void, Void> {
    private static final String TAG = "DaemonCheckService";
    private final Context context;

    public DaemonCheckService(Context context) {
        this.context = context;
    }

    @Override
    protected Void doInBackground(Void... params) {
        try {

            ITangleDaemon daemon = TangleDaemon.getInstance();

            String publicIP = daemon.getPublicIP();

            IServerConfig serverConfig = daemon.getServerConfig();

            IServerConfig publicServerConfig = TangleServerConfig.createServerConfig(serverConfig.getProtocol(),
                    publicIP, serverConfig.getPort(), serverConfig.getCert(), serverConfig.isLocalPow());

            if (TangleUtils.isReachable(publicServerConfig)) {
                Toast.makeText(context, "Public IP : " + publicIP + " is visible from outside.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(context, "SHIT IP : " + publicIP + " is not visible.", Toast.LENGTH_LONG).show();
            }

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage());
        }
        return null;
    }
}
