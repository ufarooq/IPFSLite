package threads.server.model;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

import threads.server.core.threads.THREADS;
import threads.server.core.threads.Thread;
import threads.server.core.threads.ThreadsDatabase;

public class PinsViewModel extends AndroidViewModel {

    private final ThreadsDatabase threadsDatabase;

    public PinsViewModel(@NonNull Application application) {
        super(application);
        threadsDatabase = THREADS.getInstance(
                application.getApplicationContext()).getThreadsDatabase();
    }


    public LiveData<List<Thread>> getVisiblePinnedThreads() {
        return threadsDatabase.threadDao().getLiveDataVisbilePinnedThreads();
    }
}