package threads.server;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import threads.core.Network;

import static androidx.core.util.Preconditions.checkNotNull;

class NotificationService {

    private static final String TAG = NotificationService.class.getSimpleName();

    static void notifications(@NonNull Context context) {
        checkNotNull(context);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {

                Service.getInstance(context);
                if (Network.isConnected(context)) {
                    Log.e(TAG, "Run notifications service");
                    Service.notifications(context);
                }

            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }
        });
    }

}
