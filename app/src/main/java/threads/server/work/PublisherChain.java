package threads.server.work;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.ExistingWorkPolicy;
import androidx.work.WorkManager;

import threads.ipfs.CID;

import static androidx.core.util.Preconditions.checkNotNull;

public class PublisherChain {

    public static void publish(@NonNull Context context, @NonNull CID cid, long idx) {
        checkNotNull(context);
        checkNotNull(cid);

        WorkManager.getInstance(context).beginUniqueWork(PublishContentWorker.getUniqueId(cid),
                ExistingWorkPolicy.KEEP, PublishContentWorker.getWorkRequest(cid))
                .then(LoaderContentWorker.getWorkRequest(idx)).enqueue();
    }
}
