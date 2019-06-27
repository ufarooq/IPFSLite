package threads.server;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import threads.core.Singleton;
import threads.core.api.Content;
import threads.iota.IOTA;

import static androidx.core.util.Preconditions.checkNotNull;


public class NotificationServer implements Singleton.NotificationServer {
    private static final String TAG = NotificationServer.class.getSimpleName();
    private static NotificationServer SINGELTON = new NotificationServer();

    private NotificationServer() {
    }

    static NotificationServer getInstance() {
        return SINGELTON;
    }


    public boolean sendNotification(@NonNull Context context,
                                    @NonNull String token,
                                    @NonNull String pid,
                                    @NonNull Map<String, String> params) {
        checkNotNull(context);
        checkNotNull(token);
        checkNotNull(pid);

        Content content = new Content();
        content.put(Content.PID, pid);

        for (String key : params.keySet()) {
            String value = params.get(key);
            if (value != null) {
                content.put(key, value);
            }
        }

        final Gson gson = new Gson();

        final IOTA iota = Singleton.getInstance(context).getIota();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                iota.insertTransaction(token, gson.toJson(content));

                // TODO when transaction failed
            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }
        });

        return true;
    }


}
