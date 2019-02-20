package threads.server;

import android.os.AsyncTask;
import android.util.Log;

import threads.core.IThreadsAPI;
import threads.core.Singleton;

public class ClearConsoleService extends AsyncTask<Void, Void, Void> {
    private static final String TAG = ClearConsoleService.class.getSimpleName();

    @Override
    protected Void doInBackground(Void... params) {
        try {
            IThreadsAPI threadsAPI = Singleton.getInstance().getThreadsAPI();
            threadsAPI.clearMessages();
            Application.init();
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage());
        }
        return null;
    }
}

