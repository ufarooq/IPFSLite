package threads.core;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Hashtable;
import java.util.concurrent.Future;

import threads.core.events.EVENTS;
import threads.core.peers.PEERS;
import threads.core.threads.THREADS;
import threads.iota.EntityService;
import threads.ipfs.IPFS;
import threads.ipfs.api.PubsubInfo;

import static androidx.core.util.Preconditions.checkNotNull;


public class Singleton {
    private static final String TAG = Singleton.class.getSimpleName();

    private static Singleton SINGLETON = null;

    private final Hashtable<String, Future> topics = new Hashtable<>();
    private final THREADS threads;
    private final PEERS peers;
    private final EVENTS events;
    private final EntityService entityService;


    @Nullable
    private IPFS ipfs = null;

    private Singleton(@NonNull Context context) {
        checkNotNull(context);


        entityService = EntityService.getInstance(context);

        threads = THREADS.getInstance(context);

        events = EVENTS.getInstance(context);

        peers = PEERS.getInstance(context);



        try {
            ipfs = IPFS.getInstance(context);

            IPFS.setPID(context, ipfs.getPeerID());
        } catch (Throwable e) {
            Preferences.evaluateException(events, Preferences.IPFS_INSTALL_FAILURE, e);
        }

    }

    @NonNull
    public static Singleton getInstance(@NonNull Context context) {
        checkNotNull(context);
        if (SINGLETON == null) {
            SINGLETON = new Singleton(context);
        }
        return SINGLETON;
    }


    @Nullable
    public IPFS getIpfs() {
        return ipfs;
    }

    @NonNull
    public THREADS getThreads() {
        return threads;
    }

    @NonNull
    public EVENTS getEvents() {
        return events;
    }

    @NonNull
    public PEERS getPeers() {
        return peers;
    }

    @NonNull
    public EntityService getEntityService() {
        return entityService;
    }


    public interface PubsubHandler {
        void receive(@NonNull PubsubInfo message);
    }

}
