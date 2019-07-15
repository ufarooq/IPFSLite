package threads.server;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import threads.core.Network;

import static androidx.core.util.Preconditions.checkNotNull;

class ContentsService {

    private static final String TAG = ContentsService.class.getSimpleName();


    static void contents(@NonNull Context context) {
        checkNotNull(context);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                Service.getInstance(context);

                if (Network.isConnected(context)) {
                    Log.e(TAG, "Run contents service");
                    Service.downloadContents(context);
                }
            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }

        });
    }

}
