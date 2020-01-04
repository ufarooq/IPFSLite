package threads.core.mdl;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import threads.core.Singleton;
import threads.core.api.ThreadsDatabase;
import threads.core.api.User;
import threads.ipfs.api.PID;

import static androidx.core.util.Preconditions.checkArgument;
import static androidx.core.util.Preconditions.checkNotNull;

public class UserViewModel extends AndroidViewModel {

    private final ThreadsDatabase threadsDatabase;

    public UserViewModel(@NonNull Application application) {
        super(application);
        threadsDatabase = Singleton.getInstance(
                application.getApplicationContext()).getThreadsDatabase();
    }

    public LiveData<User> getUser(@NonNull PID pid) {
        checkNotNull(pid);
        checkArgument(!pid.getPid().isEmpty());
        return threadsDatabase.userDao().getLiveDataUser(pid.getPid());
    }
}
