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

import threads.server.services.Service;

import static androidx.core.util.Preconditions.checkNotNull;

public class LoadNotificationsWorker extends Worker {

    private static final String TAG = LoadNotificationsWorker.class.getSimpleName();

    public LoadNotificationsWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);

    }

    public static void notifications(@NonNull Context context) {
        checkNotNull(context);

        if (!Service.isReceiveNotificationsEnabled(context)) {
            return;
        }


        Constraints.Builder builder = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED);


        OneTimeWorkRequest syncWorkRequest =
                new OneTimeWorkRequest.Builder(LoadNotificationsWorker.class)
                        .addTag(TAG)
                        .setConstraints(builder.build())
                        .build();

        WorkManager.getInstance(context).enqueueUniqueWork(
                TAG, ExistingWorkPolicy.KEEP, syncWorkRequest);

    }

    @NonNull
    @Override
    public Result doWork() {

        long start = System.currentTimeMillis();
        try {
            Service.notifications(getApplicationContext());

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        } finally {
            Log.e(TAG, " finish onStart [" + (System.currentTimeMillis() - start) + "]...");
        }


        return Result.success();

    }
}