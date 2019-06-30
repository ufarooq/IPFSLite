package threads.server;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import threads.core.Singleton;
import threads.core.THREADS;
import threads.ipfs.IPFS;
import threads.ipfs.api.CID;
import threads.ipfs.api.PID;
import threads.share.ConnectService;

import static androidx.core.util.Preconditions.checkNotNull;

public class UploadService {

    private static final String TAG = UploadService.class.getSimpleName();

    static void upload(@NonNull Context context, @NonNull PID pid, @NonNull CID cid) {
        checkNotNull(context);
        checkNotNull(pid);
        checkNotNull(cid);
        Service.getInstance(context);

        final THREADS threads = Singleton.getInstance(context).getThreads();

        final IPFS ipfs = Singleton.getInstance(context).getIpfs();
        if (ipfs != null) {

            try {

                if (threads.getUserByPID(pid) != null) {

                    if (!threads.isAccountBlocked(pid)) {

                        boolean success = ConnectService.connectUser(context, pid);

                        if (success) {
                            Thread.sleep(1000);
                            ipfs.pubsubPub(pid.getPid(), cid.getCid(), 50);
                            Thread.sleep(120000); // wait for 120 sec
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
}
