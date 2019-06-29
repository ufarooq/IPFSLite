package threads.server;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import threads.core.Preferences;
import threads.core.Singleton;
import threads.core.THREADS;
import threads.ipfs.api.PID;
import threads.share.ConnectService;
import threads.share.PeerService;

import static androidx.core.util.Preconditions.checkNotNull;

public class DownloadService {

    private static final String TAG = DownloadService.class.getSimpleName();

    static void download(@NonNull Context context,
                         @NonNull String pid,
                         @NonNull String cid) {
        checkNotNull(context);
        checkNotNull(pid);
        checkNotNull(cid);

        Service.getInstance(context);


        final THREADS threads = Singleton.getInstance(
                context).getThreads();


        try {

            if (threads.getUserByPID(PID.create(pid)) != null) {

                if (!threads.isAccountBlocked(PID.create(pid))) {

                    final boolean pubsubEnabled = Preferences.isPubsubEnabled(context);

                    PeerService.publishPeer(context);

                    ConnectService.connectUser(context, PID.create(pid), pubsubEnabled);

                    Service.downloadMultihash(context, PID.create(pid), cid);

                }
            }

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }


    }
}
