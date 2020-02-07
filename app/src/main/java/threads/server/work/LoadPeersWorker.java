package threads.server.work;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import threads.ipfs.IPFS;
import threads.server.services.GatewayService;

import static androidx.core.util.Preconditions.checkNotNull;

public class LoadPeersWorker extends Worker {

    private static final String TAG = LoadPeersWorker.class.getSimpleName();

    public LoadPeersWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
    }

    public static void loadPeers(@NonNull Context context) {
        checkNotNull(context);

        OneTimeWorkRequest syncWorkRequest =
                new OneTimeWorkRequest.Builder(LoadPeersWorker.class)
                        .addTag(TAG)
                        .build();

        WorkManager.getInstance(context).enqueueUniqueWork(
                TAG, ExistingWorkPolicy.KEEP, syncWorkRequest);


    }


    @NonNull
    @Override
    public Result doWork() {

        IPFS ipfs = IPFS.getInstance(getApplicationContext());
        try {
            checkNotNull(ipfs, "IPFS not valid");
            GatewayService.evaluateAllPeers(getApplicationContext());
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
        return Result.success();
    }
}
