package threads.core;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import threads.core.api.Addresses;
import threads.core.api.Members;
import threads.core.api.Thread;
import threads.ipfs.IPFS;
import threads.ipfs.api.PID;

import static androidx.core.util.Preconditions.checkArgument;
import static androidx.core.util.Preconditions.checkNotNull;

public class ConnectService {

    private static final String TAG = ConnectService.class.getSimpleName();


    public static boolean connectPeer(@NonNull Context context, @NonNull PID pid,
                                      boolean supportDiscovery, boolean updateUser, int timeout) {

        checkNotNull(context);

        checkNotNull(pid);

        checkArgument(timeout > 0);
        if (!Network.isConnected(context)) {
            return false;
        }


        if (supportDiscovery) {
            threads.core.api.PeerInfo peer = IdentityService.getPeerInfo(
                    context, pid, updateUser);
            if (peer != null) {

                if (swarmConnect(context, peer, "")) {
                    return true;
                }

            }

        }
        final IPFS ipfs = Singleton.getInstance(context).getIpfs();
        if (ipfs != null) {
            ipfs.swarmConnect(pid, timeout);

            return ipfs.isConnected(pid);

        }
        return false;
    }


    public static void swarmUnProtect(@NonNull Context context,
                                      @NonNull threads.core.api.PeerInfo peer,
                                      @NonNull String tag) {
        checkNotNull(context);
        checkNotNull(peer);
        checkNotNull(tag);
        final IPFS ipfs = Singleton.getInstance(context).getIpfs();
        if (ipfs != null) {
            Addresses addresses = peer.getAddresses();
            for (String relay : addresses.keySet()) {
                PID pid = PID.create(relay);
                if (!tag.isEmpty()) {
                    ipfs.unProtectPeer(pid, tag);
                }
            }
        }
    }

    public static boolean swarmConnect(@NonNull Context context,
                                       @NonNull threads.core.api.PeerInfo peer,
                                       @NonNull String tag) {
        checkNotNull(context);
        checkNotNull(peer);

        final int timeout = Preferences.getSwarmTimeout(context);

        final Singleton.ConsoleListener consoleListener =
                Singleton.getInstance(context).getConsoleListener();

        final IPFS ipfs = Singleton.getInstance(context).getIpfs();
        if (ipfs != null) {
            Addresses addresses = peer.getAddresses();
            for (String relay : addresses.keySet()) {
                try {
                    String ma = addresses.get(relay);
                    checkNotNull(ma);
                    PID relayPID = PID.create(relay);
                    boolean relayConnected = ipfs.isConnected(relayPID);
                    if (!relayConnected) {
                        relayConnected = ipfs.swarmConnect(
                                ma + "/" + IPFS.Style.p2p.name() + "/" + relay,
                                timeout);
                    }
                    if (relayConnected) {

                        if (!tag.isEmpty()) {
                            ipfs.protectPeer(PID.create(relay), tag);
                        }
                    }

                    if (relayConnected) {
                        String address = ipfs.relayAddress(ma, relayPID, peer.getPID());
                        ipfs.swarmConnect(address, timeout);
                    }

                } catch (Throwable e) {
                    Log.e(TAG, e.getLocalizedMessage(), e);
                }
            }

            return ipfs.isConnected(peer.getPID());
        }
        return false;
    }

    public static List<Future<Boolean>> connectMembersAsync(
            @NonNull Context context, @NonNull Thread thread,
            boolean supportDiscovery, boolean updateUser, int timeout) {
        checkNotNull(context);
        checkNotNull(thread);


        checkArgument(timeout > 0);


        ArrayList<Future<Boolean>> futures = new ArrayList<>();
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        Members members = thread.getMembers();
        for (PID pid : members) {
            Future<Boolean> future = executorService.submit(() -> {
                try {
                    return connectPeer(context, pid, supportDiscovery, updateUser, timeout);
                } catch (Throwable e) {
                    Log.e(TAG, e.getLocalizedMessage(), e);
                    return false;
                }
            });
            futures.add(future);
        }
        return futures;
    }


    public static boolean connectMembers(@NonNull Context context, @NonNull Thread thread,
                                         boolean supportDiscovery, boolean updateUser, int timeout, boolean shortcut) {

        checkNotNull(context);
        checkNotNull(thread);

        checkArgument(timeout > 0);
        boolean result = true;
        List<Future<Boolean>> futures = connectMembersAsync(
                context, thread, supportDiscovery,
                updateUser, timeout);
        for (Future<Boolean> future : futures) {
            try {
                if (!future.get()) {
                    result = false;
                    if (shortcut) {
                        return result;
                    }
                }
            } catch (Throwable e) {
                Log.e(TAG, e.getLocalizedMessage(), e);
                result = false;
            }
        }
        return result;
    }
}
