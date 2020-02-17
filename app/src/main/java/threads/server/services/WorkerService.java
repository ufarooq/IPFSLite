package threads.server.services;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.WorkManager;

import java.util.List;

import threads.server.core.threads.THREADS;
import threads.server.core.threads.Thread;
import threads.server.work.DownloadContentWorker;
import threads.server.work.DownloadThreadWorker;

import static androidx.core.util.Preconditions.checkNotNull;

public class WorkerService {

    public static void cancelThreadDownload(@NonNull Context context, long idx) {
        checkNotNull(context);

        THREADS.getInstance(context).setThreadLeaching(idx, false);
        WorkManager.getInstance(context).cancelUniqueWork(
                DownloadContentWorker.getUniqueId(idx));
        WorkManager.getInstance(context).cancelUniqueWork(
                DownloadThreadWorker.getUniqueId(idx));

        List<Thread> threads = THREADS.getInstance(context).getChildren(idx);
        for (Thread thread : threads) {
            cancelThreadDownload(context, thread.getIdx());
        }

    }

    public static void markThreadDownload(@NonNull Context context, long idx) {
        checkNotNull(context);
        THREADS.getInstance(context).setThreadLeaching(idx, true);

        List<Thread> threadList = THREADS.getInstance(context).getChildren(idx);
        for (Thread child : threadList) {
            if (!child.isSeeding()) {
                markThreadDownload(context, child.getIdx());
            }
        }
    }
}
