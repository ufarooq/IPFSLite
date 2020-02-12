package threads.server.services;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import threads.ipfs.CID;
import threads.server.R;
import threads.server.core.events.EVENTS;
import threads.server.core.threads.THREADS;
import threads.server.core.threads.Thread;
import threads.server.work.DownloadThreadWorker;

import static androidx.core.util.Preconditions.checkNotNull;

public class DownloaderService {

    private static final String TAG = DownloaderService.class.getSimpleName();

    public static void download(@NonNull Context context, @NonNull CID cid) {
        checkNotNull(context);
        checkNotNull(cid);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            long start = System.currentTimeMillis();
            try {

                THREADS threads = THREADS.getInstance(context);


                List<Thread> entries = threads.getThreadsByContentAndParent(cid, 0L);

                if (!entries.isEmpty()) {
                    Thread entry = entries.get(0);
                    if (!entry.isDeleting() && !entry.isSeeding()) {
                        WorkerService.markThreadDownload(context, entry.getIdx());
                        DownloadThreadWorker.download(context, entry.getIdx());
                    } else {
                        EVENTS.getInstance(context).postError(
                                context.getString(R.string.content_already_exists, cid.getCid()));
                    }
                } else {

                    Thread thread = threads.createThread(0L);
                    thread.setContent(cid);
                    thread.setName(cid.getCid());
                    thread.setSize(0L);
                    thread.setLeaching(true);
                    long idx = threads.storeThread(thread);


                    DownloadThreadWorker.download(context, idx);
                }

            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            } finally {
                Log.e(TAG, " finish onStart [" + (System.currentTimeMillis() - start) + "]...");

            }

        });

    }
}
