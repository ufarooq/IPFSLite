package threads.server.model;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

import threads.server.core.peers.PEERS;
import threads.server.core.peers.Peer;
import threads.server.core.peers.PeersDatabase;

public class PeersViewModel extends AndroidViewModel {

    @NonNull
    private final PeersDatabase peersDatabase;

    public PeersViewModel(@NonNull Application application) {
        super(application);
        peersDatabase = PEERS.getInstance(application.getApplicationContext()).getPeersDatabase();
    }

    @NonNull
    public LiveData<List<Peer>> getPeers() {
        return peersDatabase.peersDao().getLiveDataPeers();
    }
}