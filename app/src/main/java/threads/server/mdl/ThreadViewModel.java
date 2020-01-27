package threads.server.mdl;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

import threads.server.core.threads.THREADS;
import threads.server.core.threads.Thread;
import threads.server.core.threads.ThreadsDatabase;

import static androidx.core.util.Preconditions.checkNotNull;

public class ThreadViewModel extends AndroidViewModel {

    private final ThreadsDatabase threadsDatabase;

    public ThreadViewModel(@NonNull Application application) {
        super(application);
        threadsDatabase = THREADS.getInstance(
                application.getApplicationContext()).getThreadsDatabase();
    }


    public LiveData<List<Thread>> getVisibleChildrenByQuery(long thread, String query) {
        checkNotNull(query);
        String searchQuery = query.trim();
        if (!searchQuery.startsWith("%")) {
            searchQuery = "%" + searchQuery;
        }
        if (!searchQuery.endsWith("%")) {
            searchQuery = searchQuery + "%";
        }
        return threadsDatabase.threadDao().getLiveDataVisibleChildrenByQuery(thread, searchQuery);
    }

    public LiveData<List<Thread>> getThreads() {
        return threadsDatabase.threadDao().getLiveDataThreads();
    }
}