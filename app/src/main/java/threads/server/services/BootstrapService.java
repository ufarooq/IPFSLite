package threads.server.services;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import threads.ipfs.IPFS;

import static androidx.core.util.Preconditions.checkNotNull;

public class BootstrapService {

    @NonNull
    static final List<String> Bootstrap = new ArrayList<>(Arrays.asList(
            "/ip4/104.131.131.82/tcp/4001/p2p/QmaCpDMGvV2BGHeYERUEnRQAwe3N8SzbUtfsmvsqQLuvuJ"            // mars.i.ipfs.io
    ));
    private static final String TAG = BootstrapService.class.getSimpleName();

    public static void bootstrap(@NonNull Context context) {
        checkNotNull(context);
        try {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    IPFS ipfs = IPFS.getInstance(context);
                    checkNotNull(ipfs, "IPFS not defined");

                    for (String address : Bootstrap) {
                        Log.e(TAG, "Try : " + address);
                        boolean result = ipfs.swarmConnect(address, 10);
                        Log.e(TAG, result + " \n Bootstrap : " + address);

                    }
                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
                }
            });
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }

    }
}

