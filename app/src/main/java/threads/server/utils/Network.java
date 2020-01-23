package threads.server.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static androidx.core.util.Preconditions.checkNotNull;

public class Network {

    private static final int MIN_NETWORK_BANDWIDTH_KBPS = 4000;


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
    private static List<InetAddress> getValidInetAddresses() throws Exception {
        List<InetAddress> result = new ArrayList<>();

        List<NetworkInterface> interfaces =
                Collections.list(NetworkInterface.getNetworkInterfaces());
        for (NetworkInterface networkInterface : interfaces) {
            if (networkInterface.isUp()) {
                List<InetAddress> addresses = Collections.list(networkInterface.getInetAddresses());
                for (InetAddress address : addresses) {
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


        for (InetAddress addresse : addresses) {
            if (!addresse.isLoopbackAddress()) {
                if (useIPv4) {
                    if (addresse instanceof Inet4Address) {
                        if (isValidPublicIP(addresse)) {
                            return Pair.create(addresse, true);
                        } else {
                            return Pair.create(addresse, false);
                        }
                    }
                } else {
                    if (addresse instanceof Inet6Address) {
                        if (isValidPublicIP(addresse)) {
                            if (isIPv6GlobalAddress((Inet6Address) addresse)) {
                                return Pair.create(addresse, true);
                            } else {
                                result = Pair.create(addresse, false);
                            }
                        } else {
                            if (result == null) {
                                result = Pair.create(addresse, false);
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
