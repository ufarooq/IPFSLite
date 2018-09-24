package threads.server;

import android.os.AsyncTask;
import android.support.annotation.NonNull;

import threads.core.IThreadsAPI;
import threads.core.api.ILink;
import threads.iri.dialog.LoadResponse;

public class LoadLinkTask extends AsyncTask<String, String, ILink> {
    private static final String TAG = LoadLinkTask.class.getSimpleName();

    private final LoadResponse<ILink> response;

    public LoadLinkTask(@NonNull LoadResponse<ILink> response) {
        this.response = response;
    }

    @Override
    protected void onPostExecute(ILink link) {
        response.loaded(link);
    }

    @Override
    protected ILink doInBackground(String... accounts) {
        String accountAddress = accounts[0];
        IThreadsAPI ttApi = Application.getThreadsAPI();
        return ttApi.getLinkByAccountAddress(accountAddress);
    }

}

