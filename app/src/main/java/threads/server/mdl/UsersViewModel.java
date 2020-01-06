package threads.server.mdl;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

import threads.core.peers.PEERS;
import threads.core.peers.PeersDatabase;
import threads.core.peers.User;

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