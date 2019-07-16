package threads.server;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import threads.core.Singleton;
import threads.core.THREADS;
import threads.ipfs.api.CID;

import static androidx.core.util.Preconditions.checkNotNull;

public class PinService {
    private static final String TAG = PinService.class.getSimpleName();

    static void pin(@NonNull Context context) {
        checkNotNull(context);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                Service.getInstance(context);
                Log.e(TAG, "Run pin service");

                pinThreads(context);

            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }
        });

    }

    private static void pinThreads(@NonNull Context context) {
        final THREADS threads = Singleton.getInstance(context).getThreads();

        List<threads.core.api.Thread> pinnedThreads = threads.getPinnedThreads();
        for (threads.core.api.Thread thread : pinnedThreads) {
            CID cid = thread.getCid();
            checkNotNull(cid);
            JobServicePin.pin(context, cid.getCid());

        }

    }


}
