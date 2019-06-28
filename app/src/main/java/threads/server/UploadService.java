package threads.server;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import threads.core.Preferences;
import threads.core.Singleton;
import threads.core.THREADS;
import threads.iota.IOTA;
import threads.ipfs.IPFS;
import threads.ipfs.api.PID;
import threads.share.ConnectService;
import threads.share.PeerService;

import static androidx.core.util.Preconditions.checkNotNull;

public class UploadService {

    private static final String TAG = UploadService.class.getSimpleName();

    static void upload(@NonNull Context context,
                       @NonNull String pid,
                       @NonNull String cid,
                       @Nullable String peer) {

        checkNotNull(context);
        checkNotNull(pid);
        checkNotNull(cid);
        Service.getInstance(context);


        final THREADS threads = Singleton.getInstance(context).getThreads();

        final IPFS ipfs = Singleton.getInstance(context).getIpfs();
        if (ipfs != null) {

            try {

                if (threads.getUserByPID(PID.create(pid)) != null) {
                    if (!threads.isAccountBlocked(PID.create(pid))) {


                        if (peer != null) {
                            IOTA iota = Singleton.getInstance(context).getIota();
                            checkNotNull(iota);
                            threads.getPeerByHash(iota, PID.create(pid), peer);
                        }


                        final boolean pubsubEnabled = Preferences.isPubsubEnabled(
                                context);
                        PeerService.publishPeer(context);
                        boolean success = ConnectService.connectUser(context,
                                PID.create(pid), pubsubEnabled);

                        if (success) {
                            ipfs.pubsubPub(pid, cid, 50);
                        }
                    }
                }
            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }
        }

    }
}
