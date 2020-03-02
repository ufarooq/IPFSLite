package threads.server.work;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import threads.ipfs.IPFS;
import threads.ipfs.PID;
import threads.server.InitApplication;
import threads.server.core.peers.Content;

public class SwarmConnectWorker extends Worker {

    private static final String WID = "SCW";
    private static final String TAG = SwarmConnectWorker.class.getSimpleName();


    public SwarmConnectWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    private static String getUniqueId(@NonNull String pid) {
        return WID + pid;
    }

    private static OneTimeWorkRequest getWork(@NonNull String pid,
                                              @NonNull String host, int port, boolean inet6) {

        Data.Builder data = new Data.Builder();
        data.putString(Content.PID, pid);
        data.putString(Content.HOST, host);
        data.putInt(Content.PORT, port);
        data.putBoolean(Content.INET6, inet6);

        return new OneTimeWorkRequest.Builder(SwarmConnectWorker.class)
                .setInputData(data.build())
                .setInitialDelay(5, TimeUnit.SECONDS)
                .addTag(TAG)
                .build();
    }

    public static void connect(@NonNull Context context, @NonNull String pid,
                               @NonNull String host, int port, boolean inet6) {

        WorkManager.getInstance(context).enqueueUniqueWork(
                getUniqueId(pid), ExistingWorkPolicy.KEEP, getWork(pid, host, port, inet6));
    }


    @NonNull
    @Override
    public Result doWork() {

        String pid = getInputData().getString(Content.PID);
        Objects.requireNonNull(pid);
        String host = getInputData().getString(Content.HOST);
        int port = getInputData().getInt(Content.PORT, 0);
        boolean inet6 = getInputData().getBoolean(Content.INET6, false);
        long start = System.currentTimeMillis();

        Log.e(TAG, " start connect [" + pid + "]...");

        try {
            IPFS ipfs = IPFS.getInstance(getApplicationContext());
            if (!ipfs.isConnected(PID.create(pid))) {

                String pre = "/ip4";
                if (inet6) {
                    pre = "/ip6";
                }
                String multiAddress = pre + host +
                        "/tcp/" + port + "/" +
                        IPFS.Style.p2p + "/" + pid;

                int timeout = InitApplication.getConnectionTimeout(getApplicationContext());
                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.submit(() -> ipfs.swarmConnect(multiAddress, timeout));
            }


        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
            return Result.failure();
        } finally {
            Log.e(TAG, " finish onStart [" + (System.currentTimeMillis() - start) + "]...");
        }

        return Result.success();
    }
}

