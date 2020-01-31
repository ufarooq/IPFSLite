package threads.server.work;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.List;
import java.util.concurrent.TimeUnit;

import threads.ipfs.IPFS;
import threads.ipfs.PID;
import threads.server.core.contents.CDS;
import threads.server.core.contents.Content;
import threads.server.core.peers.PEERS;
import threads.server.services.ContentsService;
import threads.server.services.SwarmService;

import static androidx.core.util.Preconditions.checkNotNull;

public class DownloadContentsWorker extends Worker {


    private static final String TAG = DownloadContentsWorker.class.getSimpleName();


    public DownloadContentsWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    public static void download(@NonNull Context context) {
        checkNotNull(context);
        Constraints.Builder builder = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED);


        OneTimeWorkRequest syncWorkRequest =
                new OneTimeWorkRequest.Builder(DownloadContentsWorker.class)
                        .addTag(TAG)
                        .setConstraints(builder.build())
                        .build();

        WorkManager.getInstance(context).enqueueUniqueWork(
                "DownloadContentsWorker", ExistingWorkPolicy.KEEP, syncWorkRequest);


    }

    public static void periodic(@NonNull Context context) {
        checkNotNull(context);
        Constraints.Builder builder = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED);


        PeriodicWorkRequest syncWorkRequest =
                new PeriodicWorkRequest.Builder(DownloadContentsWorker.class,
                        12, TimeUnit.HOURS)
                        .addTag(TAG)
                        .setConstraints(builder.build())
                        .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "DownloadContentsWorkerPeriodic", ExistingPeriodicWorkPolicy.KEEP,
                syncWorkRequest);


    }


    @NonNull
    @Override
    public Result doWork() {

        long start = System.currentTimeMillis();

        try {
            CDS contentService = CDS.getInstance(getApplicationContext());
            PEERS threads = PEERS.getInstance(getApplicationContext());

            IPFS ipfs = IPFS.getInstance(getApplicationContext());
            PID host = IPFS.getPID(getApplicationContext());
            checkNotNull(ipfs, "IPFS not valid");
            for (PID user : threads.getUsersPIDs()) {

                if (user.equals(host)) {
                    continue;
                }

                if (!threads.isUserBlocked(user)) {

                    long timestamp = System.currentTimeMillis() -
                            TimeUnit.MINUTES.toMillis(10);


                    List<Content> contents = contentService.getContentDatabase().
                            contentDao().getContents(user, timestamp, false);

                    if (!contents.isEmpty()) {

                        SwarmService.ConnectInfo info = SwarmService.connect(getApplicationContext(), user);

                        if (info.isConnected()) {
                            for (Content entry : contents) {
                                ContentsService.download(getApplicationContext(),
                                        entry.getPid(), entry.getCID());
                            }
                        }

                        SwarmService.disconnect(getApplicationContext(), info);
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
