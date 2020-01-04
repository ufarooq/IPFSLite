package threads.core.mdl;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import java.util.ArrayList;
import java.util.List;

import threads.core.Singleton;
import threads.core.api.Status;
import threads.core.api.Thread;
import threads.core.api.ThreadsDatabase;

public class ThreadsViewModel extends AndroidViewModel {
    @NonNull
    private final MediatorLiveData<List<Thread>> liveDataMerger = new MediatorLiveData<>();
    private final ThreadsDatabase threadsDatabase;

    public ThreadsViewModel(@NonNull Application application) {
        super(application);
        threadsDatabase = Singleton.getInstance(
                application.getApplicationContext()).getThreadsDatabase();

        LiveData<List<Thread>> liveDataThreads = threadsDatabase.threadDao().getLiveDataThreads();


        liveDataMerger.addSource(liveDataThreads, (threads) -> {

            if (threads != null) {
                List<Thread> adds = new ArrayList<>();
                for (Thread thread : threads) {
                    if (thread.getStatus() != Status.DELETING) {
                        adds.add(thread);
                    }
                }
                liveDataMerger.setValue(adds);
            }

        });
    }

    @NonNull
    public LiveData<List<Thread>> getThreads() {
        return liveDataMerger;
    }


}
