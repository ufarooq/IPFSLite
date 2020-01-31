package threads.server.model;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

import threads.server.core.peers.PEERS;
import threads.server.core.peers.PeersDatabase;
import threads.server.core.peers.User;

public class UsersViewModel extends AndroidViewModel {

    @NonNull
    private final PeersDatabase peersDatabase;

    public UsersViewModel(@NonNull Application application) {
        super(application);
        peersDatabase = PEERS.getInstance(
                application.getApplicationContext()).getPeersDatabase();
    }

    @NonNull
    public LiveData<List<User>> getUsers() {
        return peersDatabase.userDao().getLiveDataUsers();
    }
}