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

import com.google.gson.Gson;

import java.util.List;

import threads.iota.Entity;
import threads.iota.EntityService;
import threads.ipfs.Encryption;
import threads.ipfs.IPFS;
import threads.ipfs.Multihash;
import threads.ipfs.PID;
import threads.server.core.contents.CDS;
import threads.server.core.peers.AddressType;
import threads.server.core.peers.Content;
import threads.server.services.LiteService;

import static androidx.core.util.Preconditions.checkNotNull;

public class LoadNotificationsWorker extends Worker {

    public static final String TAG = LoadNotificationsWorker.class.getSimpleName();

    public LoadNotificationsWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);

    }

    public static void notifications(@NonNull Context context) {
        checkNotNull(context);

        if (!LiteService.isReceiveNotificationsEnabled(context)) {
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
        Log.e(TAG, " start [" + (System.currentTimeMillis() - start) + "]...");
        try {
            Gson gson = new Gson();
            PID host = IPFS.getPID(getApplicationContext());
            checkNotNull(host);

            EntityService entityService = EntityService.getInstance(getApplicationContext());
            CDS contentService = CDS.getInstance(getApplicationContext());

            String address = AddressType.getAddress(host, AddressType.NOTIFICATION);
            List<Entity> entities = entityService.loadEntities(getApplicationContext(), address);

            for (Entity entity : entities) {
                String notification = entity.getContent();
                Content data;
                try {
                    data = gson.fromJson(notification, Content.class);
                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
                    continue;
                }
                if (data != null) {

                    IPFS ipfs = IPFS.getInstance(getApplicationContext());
                    checkNotNull(ipfs, "IPFS not valid");
                    if (data.containsKey(Content.PID) && data.containsKey(Content.CID)) {
                        try {
                            String privateKey = IPFS.getPrivateKey(getApplicationContext());
                            checkNotNull(privateKey, "Private Key not valid");
                            String pid = data.get(Content.PID);
                            checkNotNull(pid);
                            String encCid = data.get(Content.CID);
                            checkNotNull(encCid);
                            final String cid = Encryption.decryptRSA(encCid, privateKey);
                            checkNotNull(cid);

                            // check if cid is valid
                            try {
                                Multihash.fromBase58(cid);
                            } catch (Throwable e) {
                                Log.e(TAG, "" + e.getLocalizedMessage(), e);
                                continue;
                            }

                            // check if pid is valid
                            try {
                                Multihash.fromBase58(pid);
                            } catch (Throwable e) {
                                Log.e(TAG, "" + e.getLocalizedMessage(), e);
                                continue;
                            }

                            threads.server.core.contents.Content content =
                                    contentService.getContent(cid);
                            if (content == null) {
                                contentService.insertContent(pid, cid, false);
                            } else {
                                contentService.updateTimestamp(cid);
                            }
                            ConnectUserWorker.connect(getApplicationContext(), pid);


                        } catch (Throwable e) {
                            Log.e(TAG, "" + e.getLocalizedMessage(), e);
                        }
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