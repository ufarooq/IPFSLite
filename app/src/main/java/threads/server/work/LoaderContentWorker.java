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

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.TimeUnit;

import threads.ipfs.CID;
import threads.server.core.peers.Content;
import threads.server.core.threads.Status;
import threads.server.core.threads.THREADS;
import threads.server.core.threads.Thread;
import threads.server.services.LiteService;

import static androidx.core.util.Preconditions.checkArgument;
import static androidx.core.util.Preconditions.checkNotNull;

public class LoaderContentWorker extends Worker {
    public static final String WID = "LCW";
    private static final String TAG = LoaderContentWorker.class.getSimpleName();

    public LoaderContentWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);

    }

    public static void loader(@NonNull Context context, long idx) {
        checkNotNull(context);

        Constraints.Builder builder = new Constraints.Builder()
                .setRequiresCharging(true)
                .setRequiredNetworkType(NetworkType.CONNECTED);

        Data.Builder data = new Data.Builder();
        data.putLong(Content.IDX, idx);

        OneTimeWorkRequest syncWorkRequest =
                new OneTimeWorkRequest.Builder(LoaderContentWorker.class)
                        .addTag(TAG)
                        .setInputData(data.build())
                        .setConstraints(builder.build())
                        .build();

        WorkManager.getInstance(context).enqueueUniqueWork(
                WID + idx, ExistingWorkPolicy.KEEP, syncWorkRequest);
    }

    private boolean pinContent(@NonNull URL url) {
        checkNotNull(url);
        try {
            URLConnection con = url.openConnection();
            con.setConnectTimeout(15000);
            con.setReadTimeout((int) TimeUnit.MINUTES.toMillis(30));
            try (InputStream stream = con.getInputStream()) {
                //noinspection StatementWithEmptyBody
                while (stream.read() != -1) {
                }
                return true;
            } catch (Exception e) {
                Log.e(TAG, "" + e.getLocalizedMessage());
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage());
        }
        return false;
    }

    @NonNull
    @Override
    public Result doWork() {

        long start = System.currentTimeMillis();

        Log.e(TAG, " start [" + (System.currentTimeMillis() - start) + "]...");

        long idx = getInputData().getLong(Content.IDX, -1);
        checkArgument(idx >= 0);

        THREADS threads = THREADS.getInstance(getApplicationContext());

        try {
            String gateway = LiteService.getGateway(getApplicationContext());

            threads.setThreadStatus(idx, Status.STARTED);

            Thread thread = threads.getThreadByIdx(idx);
            checkNotNull(thread);

            CID cid = thread.getContent();
            if (cid != null) {
                long pageTime = System.currentTimeMillis();
                URL url = new URL(gateway + "/ipfs/" + cid);

                boolean success = pinContent(url);
                long time = (System.currentTimeMillis() - pageTime) / 1000;

                if (success) {
                    threads.setThreadStatus(idx, Status.SUCCESS);
                    Log.e(TAG, "Success publish : " + url.toString() + " " + time + " [s]");
                } else {
                    threads.setThreadStatus(idx, Status.FAILED);
                    Log.e(TAG, "Failed publish : " + url.toString() + " " + time + " [s]");
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
