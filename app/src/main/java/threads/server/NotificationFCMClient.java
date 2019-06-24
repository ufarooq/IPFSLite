package threads.server;

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
import threads.iota.IOTA;
import threads.ipfs.api.PID;
import threads.share.ConnectService;
import threads.share.RelayService;

import static androidx.core.util.Preconditions.checkNotNull;


public class NotificationFCMClient extends FirebaseMessagingService {

    public NotificationFCMClient() {
        super();
    }


    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);

        Preferences.setToken(getApplicationContext(), token);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        Map<String, String> data = remoteMessage.getData();

        if (data != null) {

            if (data.containsKey(Content.PID)) {
                final String pid = StringEscapeUtils.unescapeJava(data.get(Content.PID));
                checkNotNull(pid);

                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.submit(() -> {
                    try {

                        Service.getInstance(getApplicationContext());

                        Singleton.getInstance(getApplicationContext()).getConsoleListener().debug(data.toString());
                        final THREADS threadsAPI = Singleton.getInstance(getApplicationContext()).getThreads();
                        if (!threadsAPI.isAccountBlocked(PID.create(pid))) {

                            // check if peer hash is transmitted
                            if (data.containsKey(Content.PEER)) {
                                IOTA iota = Singleton.getInstance(getApplicationContext()).getIota();
                                checkNotNull(iota);
                                String hash = StringEscapeUtils.unescapeJava(data.get(Content.PEER));
                                checkNotNull(hash);
                                threadsAPI.getPeerByHash(iota, PID.create(pid), hash);
                            }


                            final boolean pubsubEnabled = Preferences.isPubsubEnabled(
                                    getApplicationContext());
                            RelayService.publishPeer(getApplicationContext());
                            ConnectService.connectUser(getApplicationContext(),
                                    PID.create(pid), pubsubEnabled);
                        }
                    } catch (Throwable e) {
                        Singleton.getInstance(getApplicationContext()).getConsoleListener().debug(
                                "" + e.getLocalizedMessage());
                    }
                });

            }
        }
    }


}
