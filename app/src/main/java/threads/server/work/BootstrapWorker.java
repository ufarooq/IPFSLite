package threads.server.work;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import threads.ipfs.IPFS;
import threads.server.utils.Preferences;

import static androidx.core.util.Preconditions.checkNotNull;

public class BootstrapWorker extends Worker {


    private static final String TAG = BootstrapWorker.class.getSimpleName();


    public BootstrapWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    public static void bootstrap(@NonNull Context context) {
        checkNotNull(context);
        Constraints.Builder builder = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED);


        OneTimeWorkRequest syncWorkRequest =
                new OneTimeWorkRequest.Builder(BootstrapWorker.class)
                        .addTag(TAG)
                        .setConstraints(builder.build())
                        .build();

        WorkManager.getInstance(context).enqueueUniqueWork(
                "BootstrapWorker", ExistingWorkPolicy.KEEP, syncWorkRequest);


    }


    @NonNull
    @Override
    public Result doWork() {

        long start = System.currentTimeMillis();

        try {
            IPFS ipfs = IPFS.getInstance(getApplicationContext());
            checkNotNull(ipfs, "IPFS not defined");
            int timeout = Preferences.getConnectionTimeout(getApplicationContext());
            ipfs.bootstrap(timeout);

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        } finally {
            Log.e(TAG, " finish onStart [" + (System.currentTimeMillis() - start) + "]...");
        }

        return Result.success();
    }
}

