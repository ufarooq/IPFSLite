package threads.server;

import android.os.AsyncTask;
import android.util.Log;

class ClearConsoleService extends AsyncTask<Void, Void, Void> {
    private static final String TAG = ClearConsoleService.class.getSimpleName();

    @Override
    protected Void doInBackground(Void... params) {
        try {
            Application.getEventsDatabase().clear();
            Application.init();
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage());
        }
        return null;
    }
}

