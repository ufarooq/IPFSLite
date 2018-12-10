package threads.server.daemon;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.telephony.TelephonyManager;
import android.util.Pair;

import threads.server.ServerData;
import threads.server.ServerVisibility;

import static com.google.common.base.Preconditions.checkNotNull;

public interface IThreadsServer {
    String TAG = IThreadsServer.class.getSimpleName();
    int MIN_PORT = 443;
    int MAX_PORT = 99999;
    int TCP_PORT = 14265;


    String DAEMON_SERVER_RENAME_HOST_EVENT = "DAEMON_SERVER_RENAME_HOST_EVENT";
    String DAEMON_SERVER_ONLINE_EVENT = "DAEMON_SERVER_ONLINE_EVENT";
    String DAEMON_SERVER_OFFLINE_EVENT = "DAEMON_SERVER_OFFLINE_EVENT";
    String PUBLIC_IP_CHANGE_EVENT = "PUBLIC_IP_CHANGE_EVENT";
    String HTTPS_PROTOCOL = "https";
    String HTTP_PROTOCOL = "http";
    String CONFIG = "CONFIG";
    String CONFIG_HOST = "host";
    String CONFIG_PORT = "port";


    static void setConfig(@NonNull Context context,
                          @NonNull String host,
                          @NonNull Integer port) {
        checkNotNull(context);
        checkNotNull(host);
        checkNotNull(port);
        SharedPreferences sharedPref = context.getSharedPreferences(CONFIG, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(CONFIG_HOST, host);
        editor.putInt(CONFIG_PORT, port);
        editor.apply();
    }

    static ServerData getServer(@NonNull Context context, @NonNull IThreadsConfig threadsConfig) {
        checkNotNull(context);
        checkNotNull(threadsConfig);
        SharedPreferences sharedPref = context.getSharedPreferences(CONFIG, Context.MODE_PRIVATE);
        String host = sharedPref.getString(CONFIG_HOST, "");
        if (host.isEmpty() || host.equalsIgnoreCase("localhost")) {
            Pair<String, ServerVisibility> ipv6 = IThreadsServer.getIPv6HostAddress();
            Pair<String, ServerVisibility> ipv4 = IThreadsServer.getIPv4HostAddress();
            if (ipv6.second == ServerVisibility.GLOBAL) {
                host = ipv6.first;
            } else if (ipv4.second == ServerVisibility.GLOBAL) {
                host = ipv4.first;
            } else {
                if (ipv6.second == ServerVisibility.LOCAL) {
                    host = ipv6.first;
                } else {
                    host = ipv4.first;
                }
            }
        }

        String protocol = IThreadsServer.HTTP_PROTOCOL;
        String port = threadsConfig.getPort();

        return ServerData.createServerData(protocol, host, port);
    }


    static IThreadsConfig getHttpThreadsConfig(@NonNull Context context) {
        checkNotNull(context);

        SharedPreferences sharedPref = context.getSharedPreferences(CONFIG, Context.MODE_PRIVATE);
        int port = sharedPref.getInt(CONFIG_PORT, IThreadsServer.TCP_PORT);
        String host = "";


        Pair<String, ServerVisibility> ipv6 = IThreadsServer.getIPv6HostAddress();
        Pair<String, ServerVisibility> ipv4 = IThreadsServer.getIPv4HostAddress();
        if (ipv6.second == ServerVisibility.GLOBAL) {
            host = ipv6.first;
        } else if (ipv4.second == ServerVisibility.GLOBAL) {
            host = ipv4.first;
        }
        final String hostname = host;

        return new IThreadsConfig() {
            @NonNull
            @Override
            public String getPort() {
                return String.valueOf(port);
            }


            @NonNull
            @Override
            public String getHostname() {
                return hostname;
            }
        };

    }


    static IThreadsConfig getHttpsThreadsConfig(@NonNull Context context) {
        checkNotNull(context);

        SharedPreferences sharedPref = context.getSharedPreferences(CONFIG, Context.MODE_PRIVATE);
        String host = "";
        int port = sharedPref.getInt(CONFIG_PORT, IThreadsServer.TCP_PORT);

        Pair<String, ServerVisibility> ipv6 = IThreadsServer.getIPv6HostAddress();
        Pair<String, ServerVisibility> ipv4 = IThreadsServer.getIPv4HostAddress();
        if (ipv6.second == ServerVisibility.GLOBAL) {
            host = ipv6.first;
        } else if (ipv4.second == ServerVisibility.GLOBAL) {
            host = ipv4.first;
        }
        String hostname = host;
        return new IThreadsConfig() {
            @NonNull
            @Override
            public String getPort() {
                return String.valueOf(port);
            }

            @NonNull
            @Override
            public String getHostname() {
                return hostname;
            }
        };

    }


    static Pair<String, ServerVisibility> getIPv4HostAddress() {
        return ThreadsServer.getIPAddress(true);
    }

    static Pair<String, ServerVisibility> getIPv6HostAddress() {
        return ThreadsServer.getIPAddress(false);
    }

    static NetworkInfo getNetworkInfo(@NonNull Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo();
    }

    static boolean isConnected(@NonNull Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) return false;

        Network network = connectivityManager.getActiveNetwork();
        if (network == null) return false;

        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
        return capabilities != null
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);

    }


    static boolean isConnectedWifi(@NonNull Context context) {
        NetworkInfo info = IThreadsServer.getNetworkInfo(context);
        return (info != null && info.isConnected() && info.getType() == ConnectivityManager.TYPE_WIFI);
    }


    static boolean isConnectedMobile(@NonNull Context context) {
        NetworkInfo info = IThreadsServer.getNetworkInfo(context);
        return (info != null && info.isConnected() && info.getType() == ConnectivityManager.TYPE_MOBILE);
    }

    static boolean isConnectedFast(Context context) {
        NetworkInfo info = getNetworkInfo(context);
        return (info != null && info.isConnected() && isConnectionFast(info.getType(), info.getSubtype()));
    }

    static boolean isConnectionFast(int type, int subType) {
        if (type == ConnectivityManager.TYPE_WIFI) {
            return true;
        } else if (type == ConnectivityManager.TYPE_MOBILE) {
            switch (subType) {
                case TelephonyManager.NETWORK_TYPE_1xRTT:
                    return false; // ~ 50-100 kbps
                case TelephonyManager.NETWORK_TYPE_CDMA:
                    return false; // ~ 14-64 kbps
                case TelephonyManager.NETWORK_TYPE_EDGE:
                    return false; // ~ 50-100 kbps
                case TelephonyManager.NETWORK_TYPE_EVDO_0:
                    return true; // ~ 400-1000 kbps
                case TelephonyManager.NETWORK_TYPE_EVDO_A:
                    return true; // ~ 600-1400 kbps
                case TelephonyManager.NETWORK_TYPE_GPRS:
                    return false; // ~ 100 kbps
                case TelephonyManager.NETWORK_TYPE_HSDPA:
                    return true; // ~ 2-14 Mbps
                case TelephonyManager.NETWORK_TYPE_HSPA:
                    return true; // ~ 700-1700 kbps
                case TelephonyManager.NETWORK_TYPE_HSUPA:
                    return true; // ~ 1-23 Mbps
                case TelephonyManager.NETWORK_TYPE_UMTS:
                    return true; // ~ 400-7000 kbps
                /*
                 * Above API level 7, make sure to set android:targetSdkVersion
                 * to appropriate level to use these
                 */
                case TelephonyManager.NETWORK_TYPE_EHRPD: // API level 11
                    return true; // ~ 1-2 Mbps
                case TelephonyManager.NETWORK_TYPE_EVDO_B: // API level 9
                    return true; // ~ 5 Mbps
                case TelephonyManager.NETWORK_TYPE_HSPAP: // API level 13
                    return true; // ~ 10-20 Mbps
                case TelephonyManager.NETWORK_TYPE_IDEN: // API level 8
                    return false; // ~25 kbps
                case TelephonyManager.NETWORK_TYPE_LTE: // API level 11
                    return true; // ~ 10+ Mbps
                // Unknown
                case TelephonyManager.NETWORK_TYPE_UNKNOWN:
                default:
                    return false;
            }
        } else {
            return false;
        }
    }

    void shutdown();

    boolean isRunning();

    void start(@NonNull IThreadsConfig threadsConfig);

}
