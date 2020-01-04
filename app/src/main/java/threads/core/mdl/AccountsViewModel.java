package threads.core.mdl;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

import threads.core.Singleton;
import threads.core.api.ThreadsDatabase;
import threads.core.api.User;
import threads.core.api.UserType;

public class AccountsViewModel extends AndroidViewModel {
    private final ThreadsDatabase threadsDatabase;

    public AccountsViewModel(@NonNull Application application) {
        super(application);
        threadsDatabase = Singleton.getInstance(
                application.getApplicationContext()).getThreadsDatabase();
    }


    public LiveData<List<User>> getUsers() {
        return threadsDatabase.userDao().getLiveDataUsersByType(UserType.VERIFIED);
    }
}
