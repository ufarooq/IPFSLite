package threads.server.model;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

import threads.server.core.peers.PEERS;
import threads.server.core.peers.User;
import threads.server.core.peers.UsersDatabase;

public class LiteUsersViewModel extends AndroidViewModel {

    @NonNull
    private final UsersDatabase usersDatabase;

    public LiteUsersViewModel(@NonNull Application application) {
        super(application);
        usersDatabase = PEERS.getInstance(
                application.getApplicationContext()).getUsersDatabase();
    }

    @NonNull
    public LiveData<List<User>> getLiteUsers() {
        return usersDatabase.userDao().getLiveDataNonBlockedLiteUsers();
    }
}