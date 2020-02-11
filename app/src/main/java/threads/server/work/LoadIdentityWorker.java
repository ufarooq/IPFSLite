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

import com.google.gson.Gson;

import java.util.List;

import threads.iota.Entity;
import threads.iota.EntityService;
import threads.ipfs.PID;
import threads.server.core.peers.AddressType;
import threads.server.core.peers.Content;
import threads.server.core.peers.PEERS;

import static androidx.core.util.Preconditions.checkNotNull;

public class LoadIdentityWorker extends Worker {

    public static final String WID = "LIW";
    private static final String TAG = ConnectPeerWorker.class.getSimpleName();


    public LoadIdentityWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    public static void identify(@NonNull Context context, @NonNull String pid) {
        checkNotNull(context);
        checkNotNull(pid);
        Constraints.Builder builder = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED);


        Data.Builder data = new Data.Builder();
        data.putString(Content.PID, pid);

        OneTimeWorkRequest syncWorkRequest =
                new OneTimeWorkRequest.Builder(LoadIdentityWorker.class)
                        .setInputData(data.build())
                        .addTag(TAG)
                        .setConstraints(builder.build())
                        .build();

        WorkManager.getInstance(context).enqueueUniqueWork(
                WID + pid, ExistingWorkPolicy.KEEP, syncWorkRequest);


    }


    @NonNull
    @Override
    public Result doWork() {

        String pid = getInputData().getString(Content.PID);
        checkNotNull(pid);
        long start = System.currentTimeMillis();

        try {
            PEERS peers = PEERS.getInstance(getApplicationContext());

            PID user = PID.create(pid);

            String text = loadInfo(user);
            Gson gson = new Gson();
            Content content = gson.fromJson(text, Content.class);

            if (content != null) {
                String alias = content.get(Content.ALIAS);
                if (alias != null && !alias.isEmpty()) {
                    peers.setUserAlias(user, alias);
                }
                String pkey = content.get(Content.PKEY);
                if (pkey != null && !pkey.isEmpty()) {
                    peers.setUserPublicKey(user, pkey);
                }
                peers.setUserIsLite(user.getPid());
            }

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        } finally {
            Log.e(TAG, " finish onStart [" + (System.currentTimeMillis() - start) + "]...");
        }

        return Result.success();
    }

    private String loadInfo(@NonNull PID pid) {

        checkNotNull(pid);
        EntityService entityService = EntityService.getInstance(getApplicationContext());
        String address = AddressType.getAddress(pid, AddressType.PEER);

        try {
            List<Entity> entities = entityService.loadEntities(getApplicationContext(), address);
            if (!entities.isEmpty()) {
                return entities.get(0).getContent();
            }

        } catch (Throwable e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }

        return "";
    }
}
