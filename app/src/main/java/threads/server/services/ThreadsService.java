package threads.server.services;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.WorkManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import threads.ipfs.IPFS;
import threads.server.core.threads.THREADS;
import threads.server.work.PublishContentWorker;

public class ThreadsService {


    private static final String TAG = ThreadsService.class.getSimpleName();

    public static void removeThreads(@NonNull Context context, long... indices) {


        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            long start = System.currentTimeMillis();

            try {

                THREADS threads = THREADS.getInstance(context);
                IPFS ipfs = IPFS.getInstance(context);

                threads.setThreadsDeleting(indices);

                for (long idx : indices) {
                    WorkManager.getInstance(context).cancelUniqueWork(
                            PublishContentWorker.getUniqueId(idx));
                }

                for (long idx : indices) {
                    WorkerService.cancelThreadDownload(context, idx);
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

