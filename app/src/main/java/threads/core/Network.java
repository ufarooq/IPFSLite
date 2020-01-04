package threads.core;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static androidx.core.util.Preconditions.checkNotNull;

public class Network {

    // H+ HSPA+	21Mbit/s	4Mbit/s (3G)
    private static final int MIN_NETWORK_BANDWIDTH_KBPS = 4000;

    public static int nextFreePort() {
        return nextFreePort(4001, 65535);
    }

    public static int nextFreePort(int from, int to) {
        int port = ThreadLocalRandom.current().nextInt(from, to);
        while (true) {
            if (isLocalPortFree(port)) {
                return port;
            } else {
                port = ThreadLocalRandom.current().nextInt(from, to);
            }
        }
    }

    public static boolean isLocalPortFree(int port) {
        try {
            new ServerSocket(port).close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static boolean isIPv6(@NonNull String ma) {
        checkNotNull(ma);
        return ma.startsWith("/ip6/");
    }


    public static boolean isConnected(@NonNull Context context) {
        checkNotNull(context);
        ConnectivityManager connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) return false;

        android.net.Network network = connectivityManager.getActiveNetwork();
        if (network == null) return false;

        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
        return capabilities != null
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);

    }

    public static boolean isConnectedWifi(@NonNull Context context) {
        checkNotNull(context);
        ConnectivityManager connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) return false;


        android.net.Network network = connectivityManager.getActiveNetwork();
        if (network == null) return false;

        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
        return capabilities != null
                && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);


    }

    public static boolean isConnectedMobile(@NonNull Context context) {
        checkNotNull(context);

        ConnectivityManager connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) return false;


        android.net.Network network = connectivityManager.getActiveNetwork();
        if (network == null) return false;

        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
        return capabilities != null
                && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
    }

    private static boolean isConnectedFast(@NonNull Context context) {
        checkNotNull(context);

        ConnectivityManager connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) return false;
        android.net.Network network = connectivityManager.getActiveNetwork();
        if (network == null) return false;

        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
        if (capabilities == null) return false;

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED);
    }


    private static boolean isConnectedMinHighBandwidth(@NonNull Context context) {
        checkNotNull(context);

        ConnectivityManager connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) return false;
        android.net.Network network = connectivityManager.getActiveNetwork();
        if (network == null) return false;

        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
        if (capabilities == null) return false;


        int bandwidth = capabilities.getLinkDownstreamBandwidthKbps();

        return bandwidth >= MIN_NETWORK_BANDWIDTH_KBPS;

    }

    @NonNull
    public static List<InetAddress> getValidInetAddresses() throws Exception {
        List<InetAddress> result = new ArrayList<>();

        List<NetworkInterface> interfaces =
                Collections.list(NetworkInterface.getNetworkInterfaces());
        for (NetworkInterface networkInterface : interfaces) {
            if (networkInterface.isUp()) {
                List<InetAddress> addrs = Collections.list(networkInterface.getInetAddresses());
                for (InetAddress address : addrs) {
                    if (!address.isLoopbackAddress()) {
                        if (address instanceof Inet4Address) {
                            if (isValidPublicIP(address)) {
                                result.add(address);
                            }
                        } else if (address instanceof Inet6Address) {
                            if (isValidPublicIP(address)) {
                                result.add(address);
                            }
                        }

                    }
                }
            }
        }


        return result;
    }


    @Nullable
    private static Pair<InetAddress, Boolean> getInetAddress(boolean useIPv4) throws Exception {


        List<InetAddress> addresses = getValidInetAddresses();
        Pair<InetAddress, Boolean> result = null;


        for (InetAddress addr : addresses) {
            if (!addr.isLoopbackAddress()) {
                if (useIPv4) {
                    if (addr instanceof Inet4Address) {
                        if (isValidPublicIP(addr)) {
                            return Pair.create(addr, true);
                        } else {
                            return Pair.create(addr, false);
                        }
                    }
                } else {
                    if (addr instanceof Inet6Address) {
                        if (isValidPublicIP(addr)) {
                            if (isIPv6GlobalAddress((Inet6Address) addr)) {
                                return Pair.create(addr, true);
                            } else {
                                result = Pair.create(addr, false);
                            }
                        } else {
                            if (result == null) {
                                result = Pair.create(addr, false);
                            }
                        }
                    }
                }
            }
        }

        return result;
    }

    public static boolean hasGlobalIPv6Address() throws Exception {
        Pair<InetAddress, Boolean> result = getInetAddress(false);

        if (result == null) {
            return false;
        }
        return result.second;
    }

    private static boolean isIPv6GlobalAddress(@NonNull Inet6Address address) {
        checkNotNull(address);
        if (address.isLinkLocalAddress()) return false;

        String host = address.getHostAddress();

        return (host.startsWith("2") || host.startsWith("3")) && (host.indexOf(":") == 4);
    }

    private static boolean isValidPublicIP(@NonNull InetAddress address) {
        checkNotNull(address);

        return !(address.isSiteLocalAddress() ||
                address.isAnyLocalAddress() ||
                address.isLinkLocalAddress() ||
                address.isLoopbackAddress() ||
                address.isMulticastAddress());

    }
}
