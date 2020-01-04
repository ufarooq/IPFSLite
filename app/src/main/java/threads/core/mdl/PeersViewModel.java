package threads.core.mdl;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

import threads.core.Singleton;
import threads.core.api.Peer;
import threads.core.api.PeersDatabase;

public class PeersViewModel extends AndroidViewModel {

    @NonNull
    private final PeersDatabase peersDatabase;

    public PeersViewModel(@NonNull Application application) {
        super(application);
        peersDatabase = Singleton.getInstance(
                application.getApplicationContext()).getPeersDatabase();
    }

    @NonNull
    public LiveData<List<Peer>> getPeers() {
        return peersDatabase.peersDao().getLiveDataPeers();
    }
}