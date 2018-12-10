package threads.server.daemon;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Pair;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

import threads.server.ServerVisibility;
import threads.server.event.EventsDatabase;
import threads.server.event.IEvent;

import static com.google.common.base.Preconditions.checkNotNull;

public class ThreadsServer implements IThreadsServer {
    private static final String TAG = ThreadsServer.class.getSimpleName();

    private final Context context;

    private final EventsDatabase eventsDatabase;
    private final TransactionDatabase transactionDatabase;
    private final ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(Network network) {
            super.onAvailable(network);

            new Thread(new Runnable() {
                public void run() {
                    try {
                        // now throw an event that public IP might have changed
                        IEvent event = eventsDatabase.createEvent(
                                IThreadsServer.PUBLIC_IP_CHANGE_EVENT);
                        eventsDatabase.insertEvent(event);

                    } catch (Throwable e) {
                        Log.e(TAG, "" + e.getLocalizedMessage(), e);
                    }
                }
            }).start();
        }

        @Override
        public void onLost(Network network) {
            super.onLost(network);
            new Thread(new Runnable() {
                public void run() {
                    try {
                        // now throw an event that public IP might have changed
                        IEvent event = eventsDatabase.createEvent(
                                IThreadsServer.PUBLIC_IP_CHANGE_EVENT);
                        eventsDatabase.insertEvent(event);

                    } catch (Throwable e) {
                        Log.e(TAG, "" + e.getLocalizedMessage(), e);
                    }
                }
            }).start();
        }
    };
    private WebServer server;

    private ThreadsServer(@NonNull Context context,
                          @NonNull TransactionDatabase transactionDatabase,
                          @NonNull EventsDatabase eventsDatabase) {
        checkNotNull(context);
        checkNotNull(transactionDatabase);
        checkNotNull(eventsDatabase);
        this.context = context;
        this.transactionDatabase = transactionDatabase;
        this.eventsDatabase = eventsDatabase;
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager != null) {
            manager.registerDefaultNetworkCallback(networkCallback);
        }

    }


    public static Pair<InetAddress, ServerVisibility> getInetAddress(boolean useIPv4) {
        Pair<InetAddress, ServerVisibility> result = null;

        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                if (intf.isUp()) {
                    List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                    for (InetAddress addr : addrs) {
                        if (!addr.isLoopbackAddress()) {
                            if (useIPv4) {
                                if (addr instanceof Inet4Address) {
                                    if (isValidPublicIP(addr)) {
                                        return Pair.create(addr, ServerVisibility.GLOBAL);
                                    } else {
                                        return Pair.create(addr, ServerVisibility.LOCAL);
                                    }
                                }
                            } else {
                                if (addr instanceof Inet6Address) {
                                    if (isValidPublicIP(addr)) {
                                        if (isIPv6GlobalAddress((Inet6Address) addr)) {
                                            return Pair.create(addr, ServerVisibility.GLOBAL);
                                        } else {
                                            result = Pair.create(addr, ServerVisibility.LOCAL);
                                        }
                                    } else {
                                        if (result == null) {
                                            result = Pair.create(addr, ServerVisibility.LOCAL);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }

        return result;
    }

    public static Pair<String, ServerVisibility> getIPAddress(boolean useIPv4) {
        Pair<InetAddress, ServerVisibility> result = getInetAddress(useIPv4);

        if (result == null) {
            return Pair.create("", ServerVisibility.OFFLINE);
        }
        String host = result.first.getHostAddress().split("%")[0];
        return Pair.create(host, result.second);
    }

    private static boolean isIPv6GlobalAddress(@NonNull Inet6Address address) {
        if (address.isLinkLocalAddress()) return false;

        String host = address.getHostAddress();

        return (host.startsWith("2") || host.startsWith("3")) && (host.indexOf(":") == 4);
    }

    private static boolean isValidPublicIP(InetAddress address) {
        try {
            return !(address.isSiteLocalAddress() ||
                    address.isAnyLocalAddress() ||
                    address.isLinkLocalAddress() ||
                    address.isLoopbackAddress() ||
                    address.isMulticastAddress());
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
        return false;
    }


    public static IThreadsServer createThreadServer(@NonNull Context context,
                                                    @NonNull TransactionDatabase transactionDatabase,
                                                    @NonNull EventsDatabase eventsDatabase) {
        checkNotNull(context);
        checkNotNull(transactionDatabase);
        checkNotNull(eventsDatabase);

        return new ThreadsServer(context, transactionDatabase, eventsDatabase);
    }

    @Override
    public void finalize() {
        try {
            ConnectivityManager manager = (ConnectivityManager)
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (manager != null) {
                manager.unregisterNetworkCallback(networkCallback);
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    @Override
    public synchronized void start(@NonNull IThreadsConfig threadsConfig) {

        checkNotNull(threadsConfig);


        try {
            String port = threadsConfig.getPort();
            checkNotNull(port);

            String hostname = threadsConfig.getHostname();
            checkNotNull(hostname);


            if (!hostname.isEmpty()) {
                eventsDatabase.insertMessage(
                        "Threads IRI Server is bound to the host " + hostname + " ...");
            }
            eventsDatabase.insertMessage("Threads IRI Server runs on port " + port + " ...");

            server = WebServer.getInstance(hostname, Integer.valueOf(port),
                    transactionDatabase, eventsDatabase);



            server.start();

            IEvent event = eventsDatabase.createEvent(
                    IThreadsServer.DAEMON_SERVER_ONLINE_EVENT);
            eventsDatabase.insertEvent(event);

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
            shutdown();
        }

    }

    @Override
    public boolean isRunning() {
        try {
            return server != null && server.isAlive();
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
        return false;
    }

    @Override
    public void shutdown() {
        if (server != null) {
            try {
                if (server.isAlive()) {
                    server.shutdown();
                }
            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            } finally {
                eventsDatabase.insertMessage("Threads Server finished ...");
                IEvent event = eventsDatabase.createEvent(
                        IThreadsServer.DAEMON_SERVER_OFFLINE_EVENT);
                eventsDatabase.insertEvent(event);
            }
        }
    }
}
