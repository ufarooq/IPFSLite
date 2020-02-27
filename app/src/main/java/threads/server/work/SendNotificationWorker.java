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
import java.util.Objects;

import threads.iota.EntityService;
import threads.ipfs.Encryption;
import threads.ipfs.IPFS;
import threads.ipfs.PID;
import threads.server.R;
import threads.server.core.events.EVENTS;
import threads.server.core.peers.AddressType;
import threads.server.core.peers.Content;
import threads.server.core.peers.PEERS;
import threads.server.core.peers.User;

public class SendNotificationWorker extends Worker {


    private static final String WID = "SPW";
    private static final String TAG = SendNotificationWorker.class.getSimpleName();


    public SendNotificationWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    private static void send(@NonNull Context context, @NonNull String pid, @NonNull String cid) {

        Constraints.Builder builder = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED);


        Data.Builder data = new Data.Builder();
        data.putString(Content.PID, pid);
        data.putString(Content.CID, cid);

        OneTimeWorkRequest syncWorkRequest =
                new OneTimeWorkRequest.Builder(SendNotificationWorker.class)
                        .setInputData(data.build())
                        .addTag(TAG)
                        .setConstraints(builder.build())
                        .build();

        WorkManager.getInstance(context).enqueueUniqueWork(
                WID + cid, ExistingWorkPolicy.APPEND, syncWorkRequest);


    }

    public static void sendUsers(@NonNull Context context,
                                 @NonNull List<User> users,
                                 @NonNull String cid) {

        WorkManager.getInstance(context).cancelUniqueWork(LoadNotificationsWorker.TAG);
        for (User user : users) {
            send(context, user.getPid(), cid);
        }
    }

    @NonNull
    @Override
    public Result doWork() {

        String pid = getInputData().getString(Content.PID);
        Objects.requireNonNull(pid);

        String cid = getInputData().getString(Content.CID);
        Objects.requireNonNull(cid);
        long start = System.currentTimeMillis();

        Log.e(TAG, " start [" + cid + "]...");
        try {
            PEERS peers = PEERS.getInstance(getApplicationContext());

            if (!peers.isUserBlocked(pid)) {
                notify(pid, cid, start);
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        } finally {
            Log.e(TAG, " finish onStart [" + (System.currentTimeMillis() - start) + "]...");
        }

        return Result.success();
    }

    private void notify(@NonNull String pid, @NonNull String cid, long startTime) {

        try {
            Gson gson = new Gson();
            PEERS peers = PEERS.getInstance(getApplicationContext());
            PID host = IPFS.getPID(getApplicationContext());
            EVENTS events = EVENTS.getInstance(getApplicationContext());
            Objects.requireNonNull(host);

            EntityService entityService = EntityService.getInstance(getApplicationContext());
            String address = AddressType.getAddress(pid);

            String publicKey = peers.getUserPublicKey(pid);
            boolean isLite = peers.getUserIsLite(pid);

            if (publicKey == null) {
                String alias = peers.getUserAlias(pid);
                events.error("Failed sending notification to :"
                        + alias + " Reason : Public Key not available");
            } else if (!isLite) {
                String alias = peers.getUserAlias(pid);
                events.error("Failed sending notification to :"
                        + alias + " Reason : Peer is not a IPFS Lite client");
            } else {
                Content content = new Content();

                content.put(Content.PID, host.getPid());
                content.put(Content.CID, Encryption.encryptRSA(cid, publicKey));


                String alias = peers.getUserAlias(pid);
                if (alias == null) {
                    alias = pid;
                }
                String json = gson.toJson(content);

                try {
                    entityService.insertData(getApplicationContext(), address, json);
                    long time = (System.currentTimeMillis() - startTime) / 1000;

                    events.warning(getApplicationContext().getString(R.string.success_notification,
                            alias, String.valueOf(time)));


                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
                    long time = (System.currentTimeMillis() - startTime) / 1000;
                    events.error(getApplicationContext().getString(R.string.failed_notification,
                            alias, String.valueOf(time)));
                }

            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }

    }
}
