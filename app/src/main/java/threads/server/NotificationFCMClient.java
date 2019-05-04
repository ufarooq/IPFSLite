package threads.server;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.apache.commons.text.StringEscapeUtils;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import threads.core.Preferences;
import threads.core.Singleton;
import threads.core.THREADS;
import threads.core.api.Content;
import threads.ipfs.api.PID;
import threads.share.ConnectService;

import static androidx.core.util.Preconditions.checkNotNull;


public class NotificationFCMClient extends FirebaseMessagingService {
    private static final String TAG = NotificationFCMClient.class.getSimpleName();


    public NotificationFCMClient() {
        super();
    }


    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);

        Log.e(TAG, "TOKEN : " + token);

        Preferences.setToken(getApplicationContext(), token);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        Map<String, String> data = remoteMessage.getData();

        if (data != null) {
            Log.e(TAG, data.toString());
            if (data.containsKey(Content.PID)) {
                final String pid = StringEscapeUtils.unescapeJava(data.get(Content.PID));
                checkNotNull(pid);

                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.submit(() -> {
                    try {
                        if (!Service.isInitialized()) {
                            Service.getInstance(getApplicationContext());

                            final THREADS threadsAPI = Singleton.getInstance().getThreads();
                            if (!threadsAPI.isAccountBlocked(PID.create(pid))) {
                                final boolean pubsubEnabled = Preferences.isPubsubEnabled(
                                        getApplicationContext());
                                final int timeout = Preferences.getConnectionTimeout(
                                        getApplicationContext());
                                final int threshold = Preferences.getThresholdPong(
                                        getApplicationContext());
                                ConnectService.connectUser(
                                        PID.create(pid), pubsubEnabled, timeout, threshold);
                            }

                        }
                    } catch (Throwable e) {
                        Log.e(TAG, e.getLocalizedMessage(), e);
                    }
                });

            }
        }
    }


}
