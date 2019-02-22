package threads.server;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import threads.core.IThreadsAPI;
import threads.core.Preferences;
import threads.core.Singleton;
import threads.core.api.Thread;
import threads.core.api.ThreadStatus;
import threads.ipfs.IPFS;

import static com.google.common.base.Preconditions.checkNotNull;

public class Service {


    public static void deleteThreads(@NonNull Context context, @NonNull String... addresses) {
        checkNotNull(context);
        checkNotNull(addresses);

        final IThreadsAPI threadsAPI = Singleton.getInstance().getThreadsAPI();

        final IPFS ipfs = Singleton.getInstance().getIpfs();
        if (ipfs != null) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {

                try {
                    List<Thread> threads = new ArrayList<>();
                    for (String address : addresses) {
                        Thread thread = threadsAPI.getThreadByAddress(address);
                        checkNotNull(thread);
                        thread.setStatus(ThreadStatus.DELETING);
                        threadsAPI.updateThread(thread);
                        threads.add(thread);
                    }

                    threadsAPI.removeThreads(threads);

                } catch (Throwable e) {
                    Preferences.evaluateException(Preferences.EXCEPTION, e);
                }
            });
        }
    }


}
