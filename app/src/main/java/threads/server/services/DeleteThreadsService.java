package threads.server.services;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import threads.ipfs.IPFS;
import threads.server.core.threads.THREADS;

import static androidx.core.util.Preconditions.checkNotNull;

public class DeleteThreadsService {


    private static final String TAG = DeleteThreadsService.class.getSimpleName();
    private static final String ICES = "ices";

    public static void removeThreads(@NonNull Context context, long... indices) {
        checkNotNull(context);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            long start = System.currentTimeMillis();

            try {

                THREADS threads = THREADS.getInstance(context);
                IPFS ipfs = IPFS.getInstance(context);

                checkNotNull(ipfs, "IPFS is not valid");

                threads.setThreadsDeleting(indices);

                if (indices != null) {
                    for (long idx : indices) {
                        WorkerService.cancelThreadDownload(context, idx);
                    }
                }

                threads.removeThreads(ipfs, indices);

                ipfs.gc();


            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            } finally {
                Log.e(TAG, " finish onStart [" + (System.currentTimeMillis() - start) + "]...");
            }

        });
    }


}

