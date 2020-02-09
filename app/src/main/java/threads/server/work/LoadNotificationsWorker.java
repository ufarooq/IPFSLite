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
import java.util.concurrent.TimeUnit;

import threads.iota.Entity;
import threads.iota.EntityService;
import threads.ipfs.CID;
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

    private static final String TAG = LoadNotificationsWorker.class.getSimpleName();

    public LoadNotificationsWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);

    }

    public static void notifications(@NonNull Context context, int secondsDelay) {
        checkNotNull(context);

        if (!LiteService.isReceiveNotificationsEnabled(context)) {
            return;
        }

        Constraints.Builder builder = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED);


        OneTimeWorkRequest syncWorkRequest =
                new OneTimeWorkRequest.Builder(LoadNotificationsWorker.class)
                        .setInitialDelay(secondsDelay, TimeUnit.SECONDS)
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
                            String privateKey = ipfs.getPrivateKey();
                            checkNotNull(privateKey, "Private Key not valid");
                            String encPid = data.get(Content.PID);
                            checkNotNull(encPid);
                            final String pidStr = Encryption.decryptRSA(encPid, privateKey);
                            checkNotNull(pidStr);

                            String encCid = data.get(Content.CID);
                            checkNotNull(encCid);
                            final String cidStr = Encryption.decryptRSA(encCid, privateKey);
                            checkNotNull(cidStr);

                            // check if cid is valid
                            try {
                                Multihash.fromBase58(cidStr);
                            } catch (Throwable e) {
                                Log.e(TAG, "" + e.getLocalizedMessage(), e);
                                continue;
                            }

                            // check if pid is valid
                            try {
                                Multihash.fromBase58(pidStr);
                            } catch (Throwable e) {
                                Log.e(TAG, "" + e.getLocalizedMessage(), e);
                                continue;
                            }

                            PID pid = PID.create(pidStr);
                            CID cid = CID.create(cidStr);

                            // THIS is a try, it tries to find the pubsub of the PID
                            // (for sending a message when done)
                            ipfs.addPubSubTopic(getApplicationContext(), pid.getPid());


                            threads.server.core.contents.Content content =
                                    contentService.getContent(cid);
                            if (content == null) {
                                contentService.insertContent(pid, cid, false);
                            }

                            ContentsWorker.download(getApplicationContext(), cid, pid);

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