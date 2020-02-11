package threads.server.services;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import threads.ipfs.IPFS;

import static androidx.core.util.Preconditions.checkNotNull;

public class BootstrapService {

    private static final String TAG = BootstrapService.class.getSimpleName();


    public static void bootstrap(@NonNull Context context) {
        checkNotNull(context);
        try {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    IPFS ipfs = IPFS.getInstance(context);
                    checkNotNull(ipfs, "IPFS not defined");
                    ipfs.bootstrap(5);
                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
                }
            });
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }

    }
}

