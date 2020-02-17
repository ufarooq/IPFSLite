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

import java.util.concurrent.TimeUnit;

import threads.ipfs.CID;
import threads.ipfs.IPFS;
import threads.server.core.peers.Content;

import static androidx.core.util.Preconditions.checkNotNull;

public class PublishContentWorker extends Worker {
    private static final String WID = "PCW";
    private static final String TAG = PublishContentWorker.class.getSimpleName();

    public PublishContentWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    static String getUniqueId(@NonNull CID cid) {
        checkNotNull(cid);
        return WID + cid.getCid();
    }

    static OneTimeWorkRequest getWorkRequest(@NonNull CID cid) {
        checkNotNull(cid);
        Constraints.Builder builder = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED);


        Data.Builder data = new Data.Builder();
        data.putString(Content.CID, cid.getCid());

        return new OneTimeWorkRequest.Builder(PublishContentWorker.class)
                .addTag(TAG)
                .setInputData(data.build())
                .setConstraints(builder.build())
                .build();

    }

    public static void publish(@NonNull Context context, @NonNull CID cid) {
        checkNotNull(context);
        checkNotNull(cid);


        WorkManager.getInstance(context).enqueueUniqueWork(
                getUniqueId(cid), ExistingWorkPolicy.KEEP, getWorkRequest(cid));
    }

    @NonNull
    @Override
    public Result doWork() {

        long start = System.currentTimeMillis();

        String cidStr = getInputData().getString(Content.CID);
        checkNotNull(cidStr);

        Log.e(TAG, " start [" + cidStr + "]...");

        try {
            IPFS ipfs = IPFS.getInstance(getApplicationContext());
            checkNotNull(ipfs, "IPFS not valid");

            ipfs.dhtPublish(CID.create(cidStr), true,
                    (int) TimeUnit.MINUTES.toSeconds(3));
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        } finally {
            Log.e(TAG, " finish onStart [" + (System.currentTimeMillis() - start) + "]...");
        }

        return Result.success();

    }
}
