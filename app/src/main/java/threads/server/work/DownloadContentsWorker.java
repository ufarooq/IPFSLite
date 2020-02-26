package threads.server.work;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.List;
import java.util.Objects;

import threads.server.core.contents.CDS;
import threads.server.core.contents.Content;
import threads.server.core.peers.PEERS;

public class DownloadContentsWorker extends Worker {

    private static final String WID = "DCW";
    private static final String TAG = DownloadContentsWorker.class.getSimpleName();


    public DownloadContentsWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    public static void download(@NonNull Context context, @NonNull String pid) {

        Constraints.Builder builder = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED);


        Data.Builder data = new Data.Builder();
        data.putString(threads.server.core.peers.Content.PID, pid);

        OneTimeWorkRequest syncWorkRequest =
                new OneTimeWorkRequest.Builder(DownloadContentsWorker.class)
                        .addTag(TAG)
                        .setConstraints(builder.build())
                        .setInputData(data.build())
                        .build();

        WorkManager.getInstance(context).enqueueUniqueWork(
                WID + pid, ExistingWorkPolicy.KEEP, syncWorkRequest);


    }


    @NonNull
    @Override
    public Result doWork() {

        long start = System.currentTimeMillis();
        String pid = getInputData().getString(threads.server.core.peers.Content.PID);
        Objects.requireNonNull(pid);

        try {
            CDS contentService = CDS.getInstance(getApplicationContext());
            PEERS threads = PEERS.getInstance(getApplicationContext());


            if (!threads.isUserBlocked(pid)) {

                List<Content> contents = contentService.getContentDatabase().
                        contentDao().getContents(pid);

                if (!contents.isEmpty()) {
                    for (Content entry : contents) {
                        ContentsWorker.download(getApplicationContext(),
                                entry.getCid(), entry.getPid());
                    }
                }
            }


        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        } finally {
            Log.e(TAG, " finish onStart [" + (System.currentTimeMillis() - start) + "]...");
        }

        return Result.success();
    }
}
