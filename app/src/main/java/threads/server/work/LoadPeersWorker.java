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
import threads.server.core.events.EVENTS;
import threads.server.fragments.SwarmFragment;
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
        EVENTS events = EVENTS.getInstance(getApplicationContext());
        try {
            checkNotNull(ipfs, "IPFS not valid");
            GatewayService.PeerSummary info =
                    GatewayService.evaluateAllPeers(getApplicationContext());

            String content = SwarmFragment.NONE;
            if (info.getLatency() < 150) {
                content = SwarmFragment.HIGH;
            } else if (info.getLatency() < 500) {
                content = SwarmFragment.MEDIUM;
            } else if (info.getNumPeers() > 0) {
                content = SwarmFragment.LOW;
            }
            events.invokeEvent(SwarmFragment.TAG, content);


            events.invokeEvent(SwarmFragment.TAG, SwarmFragment.NONE);
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
        return Result.success();
    }
}
