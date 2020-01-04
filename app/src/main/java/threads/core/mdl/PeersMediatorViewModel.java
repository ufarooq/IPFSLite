package threads.core.mdl;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import java.util.ArrayList;
import java.util.List;

import threads.core.Singleton;
import threads.core.api.IPeer;
import threads.core.api.Peer;
import threads.core.api.PeersDatabase;
import threads.core.api.ThreadsDatabase;
import threads.core.api.User;

public class PeersMediatorViewModel extends AndroidViewModel {

    private final List<IPeer> storedPeers = new ArrayList<>();

    @NonNull
    private final MediatorLiveData<List<IPeer>> liveDataMerger = new MediatorLiveData<>();
    @NonNull
    private final ThreadsDatabase threadsDatabase;
    @NonNull
    private final PeersDatabase peersDatabase;

    public PeersMediatorViewModel(@NonNull Application application) {
        super(application);
        threadsDatabase = Singleton.getInstance(
                application.getApplicationContext()).getThreadsDatabase();
        peersDatabase = Singleton.getInstance(
                application.getApplicationContext()).getPeersDatabase();

        LiveData<List<User>> liveDataUsers = threadsDatabase.userDao().getLiveDataUsers();

        liveDataMerger.addSource(liveDataUsers, (users) -> {

            if (users != null) {
                storedPeers.clear();
                storedPeers.addAll(users);
            }

        });

        LiveData<List<Peer>> liveDataPeers = peersDatabase.peersDao().getLiveDataPeers();
        liveDataMerger.addSource(liveDataPeers, (users) -> {

            if (users != null) {
                List<IPeer> peers = new ArrayList<>(storedPeers);
                peers.addAll(users);
                liveDataMerger.setValue(peers);
            }

        });
    }

    @NonNull
    public LiveData<List<IPeer>> getPeers() {
        return liveDataMerger;
    }
}
