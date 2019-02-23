package threads.server;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.util.Log;

import androidx.annotation.NonNull;
import threads.core.IThreadsAPI;
import threads.core.Preferences;
import threads.core.Singleton;
import threads.core.api.MessageKind;
import threads.core.api.User;
import threads.core.api.UserStatus;
import threads.core.api.UserType;
import threads.ipfs.IPFS;
import threads.ipfs.api.PID;

import static com.google.common.base.Preconditions.checkNotNull;

public class Application extends android.app.Application {


    private static final String TAG = Application.class.getSimpleName();


    public static boolean isConnected(@NonNull Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) return false;

        Network network = connectivityManager.getActiveNetwork();
        if (network == null) return false;

        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
        return capabilities != null
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);

    }


    private static void init(@NonNull Context context) {
        checkNotNull(context);

        final IThreadsAPI threadsApi = Singleton.getInstance().getThreadsAPI();
        final IPFS ipfs = Singleton.getInstance().getIpfs();
        if (ipfs != null) {
            new Thread(() -> {


                PID pid = Preferences.getPID(context);
                checkNotNull(pid);
                User user = threadsApi.getUserByPID(pid);
                if (user == null) {

                    String inbox = Preferences.getInbox(context);
                    checkNotNull(inbox);
                    String publicKey = ipfs.getPublicKey();

                    user = threadsApi.createUser(pid, inbox, publicKey,
                            pid.getPid(), UserType.VERIFIED, null);
                    user.setStatus(UserStatus.ONLINE);
                    threadsApi.storeUser(user);
                }

                threadsApi.storeMessage(threadsApi.createMessage(MessageKind.INFO,
                        "\nWelcome to IPFS",
                        System.currentTimeMillis()));

                threadsApi.storeMessage(threadsApi.createMessage(MessageKind.INFO,
                        "Please feel free to start an IPFS daemon ...\n\n"
                        , System.currentTimeMillis()));

                DaemonService.evalUserStatus(threadsApi);

            }).start();
        }
    }


    @Override
    public void onTerminate() {
        super.onTerminate();
        Log.e(TAG, "...... end application");
    }

    @Override
    public void onCreate() {
        super.onCreate();


        Log.e(TAG, "...... start application");
        Preferences.setPubsubEnabled(getApplicationContext(), true); // TODO remove again
        NotificationSender.createChannel(getApplicationContext());

        try {
            Singleton.getInstance().init(getApplicationContext(), () -> "",
                    null, false, true);
        } catch (Throwable e) {
            Preferences.evaluateException(Preferences.IPFS_INSTALL_FAILURE, e);
        }

        init(getApplicationContext());


    }

}
