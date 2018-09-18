package threads.server;

import android.os.AsyncTask;
import android.util.Log;

public class ClearConsoleService extends AsyncTask<Void, Void, Void> {
    private static final String TAG = "DaemonStatusService";


    @Override
    protected Void doInBackground(Void... params) {
        try {
            Application.getEventsDatabase().clear();
            Application.initMessageDatabase();
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage());
        }
        return null;
    }
}

