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

import threads.ipfs.CID;
import threads.ipfs.IPFS;
import threads.ipfs.Multihash;
import threads.server.core.threads.THREADS;
import threads.server.utils.Preferences;

import static androidx.core.util.Preconditions.checkArgument;
import static androidx.core.util.Preconditions.checkNotNull;

public class DownloadThumbnailWorker extends Worker {
    private static final String TAG = DownloadThumbnailWorker.class.getSimpleName();
    private static final String ID = "ID";
    private static final String IDX = "IDX";

    public DownloadThumbnailWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);

    }

    public static void download(@NonNull Context context, @NonNull String cid, long threadIdx) {

        checkNotNull(context);
        checkNotNull(cid);

        Constraints.Builder builder = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED);


        Data.Builder data = new Data.Builder();
        data.putString(ID, cid);
        data.putLong(IDX, threadIdx);

        OneTimeWorkRequest syncWorkRequest =
                new OneTimeWorkRequest.Builder(DownloadThumbnailWorker.class)
                        .addTag("DownloadContentWorker")
                        .setInputData(data.build())
                        .setConstraints(builder.build())
                        .build();

        WorkManager.getInstance(context).enqueueUniqueWork(
                cid, ExistingWorkPolicy.KEEP, syncWorkRequest);
    }

    @NonNull
    @Override
    public Result doWork() {

        IPFS ipfs = IPFS.getInstance(getApplicationContext());
        THREADS threads = THREADS.getInstance(getApplicationContext());
        int timeout = Preferences.getConnectionTimeout(getApplicationContext());// todo


        try {
            String multihash = getInputData().getString(ID);
            checkNotNull(multihash);

            Multihash.fromBase58(multihash);

            long idx = getInputData().getLong(IDX, -1);
            checkArgument(idx >= 0);


            CID cid = CID.create(multihash);
            byte[] data = ipfs.loadData(cid, timeout);
            if (data != null) {
                threads.setThreadThumbnail(idx, cid);
            }

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
        return Result.success();

    }
}
