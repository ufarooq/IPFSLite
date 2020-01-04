package threads.core.mdl;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

import threads.core.Singleton;
import threads.core.api.ThreadsDatabase;
import threads.core.api.User;

public class UsersViewModel extends AndroidViewModel {

    @NonNull
    private final ThreadsDatabase threadsDatabase;

    public UsersViewModel(@NonNull Application application) {
        super(application);
        threadsDatabase = Singleton.getInstance(
                application.getApplicationContext()).getThreadsDatabase();
    }

    @NonNull
    public LiveData<List<User>> getUsers() {
        return threadsDatabase.userDao().getLiveDataUsers();
    }
}