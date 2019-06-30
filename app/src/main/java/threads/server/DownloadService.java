package threads.server;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import threads.core.Singleton;
import threads.core.THREADS;
import threads.ipfs.api.CID;
import threads.ipfs.api.PID;
import threads.share.ConnectService;

import static androidx.core.util.Preconditions.checkNotNull;

public class DownloadService {

    private static final String TAG = DownloadService.class.getSimpleName();

    static void download(@NonNull Context context,
                         @NonNull PID pid,
                         @NonNull CID cid) {
        checkNotNull(context);
        checkNotNull(pid);
        checkNotNull(cid);

        Service.getInstance(context);


        final THREADS threads = Singleton.getInstance(
                context).getThreads();


        try {

            if (threads.getUserByPID(pid) != null) {

                if (!threads.isAccountBlocked(pid)) {

                    boolean success = ConnectService.connectUser(context, pid);

                    if (success) {
                        Service.downloadMultihash(context, pid, cid);
                    } else {
                        Singleton.getInstance(context).getConsoleListener().info(
                                "Can't connect to PID :" + pid);
                    }

                }
            }

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }


    }
}
