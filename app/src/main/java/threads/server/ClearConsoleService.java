package threads.server;

import android.os.AsyncTask;
import android.util.Log;

public class ClearConsoleService extends AsyncTask<Void, Void, Void> {
    private static final String TAG = "DaemonCheckService";


    @Override
    protected Void doInBackground(Void... params) {
        try {
            Application.getMessagesDatabase().clear();
            Application.initMessageDatabase();
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage());
        }
        return null;
    }
}

