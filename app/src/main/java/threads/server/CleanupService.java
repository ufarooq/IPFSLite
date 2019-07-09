package threads.server;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static androidx.core.util.Preconditions.checkNotNull;

class CleanupService {

    private static final String TAG = CleanupService.class.getSimpleName();


    static void cleanup(@NonNull Context context) {
        checkNotNull(context);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                Service.getInstance(context);
                threads.server.Service.cleanup(context);

            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }
        });
    }


}
