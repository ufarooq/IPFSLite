package threads.server;

import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.gson.Gson;
import com.j256.simplemagic.ContentInfo;

import org.apache.commons.lang3.StringUtils;
import org.iota.jota.pow.pearldiver.PearlDiverLocalPoW;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import threads.core.IdentityService;
import threads.core.MimeType;
import threads.core.Network;
import threads.core.Preferences;
import threads.core.Singleton;
import threads.core.THREADS;
import threads.core.api.AddressType;
import threads.core.api.Content;
import threads.core.api.Kind;
import threads.core.api.Server;
import threads.core.api.Thread;
import threads.core.api.ThreadStatus;
import threads.core.api.User;
import threads.core.api.UserStatus;
import threads.core.api.UserType;
import threads.iota.Entity;
import threads.iota.EntityService;
import threads.iota.HashDatabase;
import threads.iota.IOTA;
import threads.ipfs.IPFS;
import threads.ipfs.api.CID;
import threads.ipfs.api.ConnMgrConfig;
import threads.ipfs.api.Encryption;
import threads.ipfs.api.LinkInfo;
import threads.ipfs.api.Multihash;
import threads.ipfs.api.PID;
import threads.ipfs.api.PeerInfo;
import threads.ipfs.api.PubsubConfig;
import threads.ipfs.api.PubsubInfo;
import threads.ipfs.api.PubsubReader;
import threads.ipfs.api.RoutingConfig;
import threads.share.MimeTypeService;
import threads.share.RTCSession;
import threads.share.ThumbnailService;

import static androidx.core.util.Preconditions.checkArgument;
import static androidx.core.util.Preconditions.checkNotNull;


public class Service {

    public static final int RELAYS = 5;
    public static final String PIN_SERVICE_KEY = "pinServiceKey";
    private static final String TAG = Service.class.getSimpleName();
    private static final Gson gson = new Gson();
    private static final ExecutorService UPLOAD_SERVICE = Executors.newFixedThreadPool(10);
    private static final String APP_KEY = "AppKey";
    private static final String PIN_SERICE_TIME_KEY = "pinServiceTimeKey";
    private static final String GATEWAY_KEY = "gatewayKey";
    private static final String AUTO_DOWNLOAD_KEY = "autoDownloadKey";
    private static final String UPDATE = "UPDATE";
    private static final String SEND_NOTIFICATIONS_ENABLED_KEY = "sendNotificationKey";
    private static final String RECEIVE_NOTIFICATIONS_ENABLED_KEY = "receiveNotificationKey";
    private static final String SUPPORT_PEER_DISCOVERY_KEY = "supportPeerDiscoveryKey";
    private static Service SINGLETON = null;
    private final AtomicBoolean peerCheckFlag = new AtomicBoolean(false);

    private Service() {
    }

    public static boolean isSupportPeerDiscovery(@NonNull Context context) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE);
        return sharedPref.getBoolean(SUPPORT_PEER_DISCOVERY_KEY, true);
    }

    public static void setSupportPeerDiscovery(@NonNull Context context, boolean enable) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(SUPPORT_PEER_DISCOVERY_KEY, enable);
        editor.apply();
    }

    public static boolean isAutoDownload(@NonNull Context context) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(
                APP_KEY, Context.MODE_PRIVATE);
        return sharedPref.getBoolean(AUTO_DOWNLOAD_KEY, true);
    }

    public static void setAutoDownload(@NonNull Context context, boolean automaticDownload) {
        checkNotNull(context);

        SharedPreferences sharedPref = context.getSharedPreferences(
                APP_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(AUTO_DOWNLOAD_KEY, automaticDownload);
        editor.apply();

    }

    @NonNull
    public static String getGateway(@NonNull Context context) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(
                APP_KEY, Context.MODE_PRIVATE);
        return sharedPref.getString(GATEWAY_KEY, "https://ipfs.io");
    }

    public static void setGateway(@NonNull Context context, @NonNull String gateway) {
        checkNotNull(context);
        checkNotNull(gateway);
        SharedPreferences sharedPref = context.getSharedPreferences(
                APP_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(GATEWAY_KEY, gateway);
        editor.apply();

    }

    public static int getPinServiceTime(@NonNull Context context) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE);
        return sharedPref.getInt(PIN_SERICE_TIME_KEY, 6);
    }

    public static void setPinServiceTime(@NonNull Context context, int timeout) {
        checkNotNull(context);
        checkArgument(timeout >= 0);
        SharedPreferences sharedPref = context.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(PIN_SERICE_TIME_KEY, timeout);
        editor.apply();
    }

    public static boolean getDontShowAgain(@NonNull Context context, @NonNull String key) {
        checkNotNull(context);
        checkNotNull(key);
        SharedPreferences sharedPref = context.getSharedPreferences(
                APP_KEY, Context.MODE_PRIVATE);
        return sharedPref.getBoolean(key, false);
    }

    public static void setDontShowAgain(@NonNull Context context, @NonNull String key, boolean value) {
        checkNotNull(context);
        checkNotNull(key);
        SharedPreferences sharedPref = context.getSharedPreferences(
                APP_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(key, value);
        editor.apply();

    }


    public static boolean isSendNotificationsEnabled(@NonNull Context context) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE);
        return sharedPref.getBoolean(SEND_NOTIFICATIONS_ENABLED_KEY, true);
    }

    public static void setSendNotificationsEnabled(@NonNull Context context, boolean enable) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(SEND_NOTIFICATIONS_ENABLED_KEY, enable);
        editor.apply();
    }


    public static boolean isReceiveNotificationsEnabled(@NonNull Context context) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE);
        return sharedPref.getBoolean(RECEIVE_NOTIFICATIONS_ENABLED_KEY, true);
    }

    public static void setReceiveNotificationsEnabled(@NonNull Context context, boolean enable) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(RECEIVE_NOTIFICATIONS_ENABLED_KEY, enable);
        editor.apply();
    }

    @NonNull
    static Service getInstance(@NonNull Context context) {
        checkNotNull(context);
        if (SINGLETON == null) {

            runUpdatesIfNecessary(context);

            ProgressChannel.createProgressChannel(context);
            RTCSession.createRTCChannel(context);

            long time = System.currentTimeMillis();
            Singleton.getInstance(context);
            Log.e(TAG, "Time Instance : " + (System.currentTimeMillis() - time));

            SINGLETON = new Service();
            SINGLETON.startDaemon(context);
            Log.e(TAG, "Time Daemon : " + (System.currentTimeMillis() - time));
            SINGLETON.init(context);

        }
        return SINGLETON;
    }


    private static void checkTangleServer(@NonNull Context context) {
        checkNotNull(context);
        try {
            if (Network.isConnected(context)) {
                IOTA iota = Singleton.getInstance(context).getIota();
                checkNotNull(iota);

                try {
                    if (!IOTA.remotePoW(iota.getNodeInfo())) {
                        iota.setLocalPoW(new PearlDiverLocalPoW());
                    }
                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
                }

            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }


    private static long getDaysAgo(int days) {
        return System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days);
    }

    public static void notifications(@NonNull Context context) {

        final PID host = Preferences.getPID(context);
        if (host != null) {
            final int timeout = Preferences.getTangleTimeout(context);
            final Server server = Preferences.getTangleServer(context);

            final EntityService entityService = EntityService.getInstance(context);
            final ContentService contentService = ContentService.getInstance(context);
            try {
                String address = AddressType.getAddress(host, AddressType.NOTIFICATION);
                List<Entity> entities = entityService.loadEntities(address,
                        server.getProtocol(), server.getHost(), server.getPort(), timeout);

                Log.e(TAG, "Received " + entities.size() + " incoming messages");

                for (Entity entity : entities) {
                    String notification = entity.getContent();
                    Content data;
                    try {
                        data = gson.fromJson(notification, Content.class);
                    } catch (Throwable e) {
                        Log.e(TAG, "" + e.getLocalizedMessage(), e);
                        continue;
                    }
                    if (data != null) {


                        Service.getInstance(context); // now time to load instance

                        final IPFS ipfs = Singleton.getInstance(context).getIpfs();
                        final THREADS threads = Singleton.getInstance(context).getThreads();
                        final IOTA iota = Singleton.getInstance(context).getIota();
                        checkNotNull(ipfs, "IPFS not valid");
                        if (data.containsKey(Content.PID) && data.containsKey(Content.CID)) {
                            try {
                                String privateKey = ipfs.getPrivateKey();
                                checkNotNull(privateKey, "Private Key not valid");
                                final String pidStr = Encryption.decryptRSA(
                                        data.get(Content.PID), privateKey);
                                checkNotNull(pidStr);

                                final String cidStr = Encryption.decryptRSA(
                                        data.get(Content.CID), privateKey);
                                checkNotNull(cidStr);

                                // check if cid is valid
                                try {
                                    Multihash.fromBase58(cidStr);
                                } catch (Throwable e) {
                                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
                                    continue;
                                }

                                // check if pid is valid
                                try {
                                    Multihash.fromBase58(pidStr);
                                } catch (Throwable e) {
                                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
                                    continue;
                                }

                                PID pid = PID.create(pidStr);
                                CID cid = CID.create(cidStr);


                                threads.server.Content content =
                                        contentService.getContent(cid);
                                if (content == null) {
                                    contentService.insertContent(pid, cid, false);
                                }


                                if (data.containsKey(Content.HASH)) {
                                    final String hash = data.get(Content.HASH);

                                    threads.core.api.PeerInfo peerInfo =
                                            threads.loadPeerInfoByHash(
                                                    iota, pid, hash, BuildConfig.ApiAesKey);
                                    boolean success = false;
                                    if (peerInfo != null) {
                                        success = IdentityService.connectPeer(
                                                context, peerInfo,
                                                timeout, true, true);

                                    }
                                    Singleton.getInstance(context).getConsoleListener().info(
                                            "Success Connect Hash : " + success);
                                }


                                Singleton.getInstance(context).getConsoleListener().info(
                                        "Receive Inbox Notification from PID :" + pid);


                                JobServiceContents.contents(context, pid, cid);


                            } catch (Throwable e) {
                                Log.e(TAG, "" + e.getLocalizedMessage(), e);
                            }
                        }

                    }
                }
            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }
        }

    }

    public static void notify(@NonNull Context context,
                              @NonNull String pid,
                              @NonNull String cid,
                              @Nullable String hash,
                              long startTime) {

        checkNotNull(context);
        checkNotNull(pid);
        checkNotNull(cid);

        final THREADS threads = Singleton.getInstance(context).getThreads();
        final PID host = Preferences.getPID(context);
        checkNotNull(host);
        final IOTA iota = Singleton.getInstance(context).getIota();
        try {
            String address = AddressType.getAddress(
                    PID.create(pid), AddressType.NOTIFICATION);

            String publicKey = threads.getUserPublicKey(pid);
            if (publicKey.isEmpty()) {
                IPFS ipfs = Singleton.getInstance(context).getIpfs();
                checkNotNull(ipfs, "IPFS not valid");
                int timeout = Preferences.getConnectionTimeout(context);
                PeerInfo info = ipfs.id(PID.create(pid), timeout);
                if (info != null) {
                    String key = info.getPublicKey();
                    if (key != null) {
                        threads.setUserPublicKey(pid, key);
                        publicKey = key;
                    }
                }
            }
            if (publicKey.isEmpty()) {
                Singleton.getInstance(context).getConsoleListener().error(
                        "Failed sending notification to PID Inbox :"
                                + pid + " Reason : Public Key not available");
            } else {
                Content content = new Content();

                content.put(Content.PID, Encryption.encryptRSA(host.getPid(), publicKey));
                content.put(Content.CID, Encryption.encryptRSA(cid, publicKey));
                if (hash != null) {
                    content.put(Content.HASH, hash);
                }

                String alias = threads.getUserAlias(pid);
                String json = gson.toJson(content);

                try {
                    iota.insertTransaction(address, json);
                    long time = (System.currentTimeMillis() - startTime) / 1000;

                    threads.invokeEvent(Preferences.INFO,
                            context.getString(R.string.success_notification,
                                    alias, String.valueOf(time)));

                    Singleton.getInstance(context).getConsoleListener().info(
                            "Sucess sending notification to PID Inbox : "
                                    + pid + " Time : " + time + "[s]");

                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
                    long time = (System.currentTimeMillis() - startTime) / 1000;

                    threads.invokeEvent(Preferences.INFO,
                            context.getString(R.string.failed_notification,
                                    alias, String.valueOf(time)));

                    Singleton.getInstance(context).getConsoleListener().error(
                            "Failed sending notification to PID Inbox :"
                                    + pid + " Time : " + time + "[s]");
                }

            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    static void cleanup(@NonNull Context context) {


        try {
            final ContentService contentService = ContentService.getInstance(context);
            final EntityService entityService = EntityService.getInstance(context);
            final IPFS ipfs = Singleton.getInstance(context).getIpfs();

            // remove all old hashes from hash database
            HashDatabase hashDatabase = entityService.getHashDatabase();
            long timestamp = getDaysAgo(28);
            hashDatabase.hashDao().removeAllHashesWithSmallerTimestamp(timestamp);


            // remove all content
            timestamp = getDaysAgo(14);
            ContentDatabase contentDatabase = contentService.getContentDatabase();
            List<threads.server.Content> entries = contentDatabase.contentDao().
                    getContentWithSmallerTimestamp(timestamp);

            checkNotNull(ipfs, "IPFS not valid");

            for (threads.server.Content content : entries) {

                contentDatabase.contentDao().removeContent(content);

                CID cid = content.getCID();
                ipfs.rm(cid);
            }

            ipfs.repo_gc();
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }

    }

    public static void connectPeer(@NonNull Context context, @NonNull PID user) throws Exception {
        checkNotNull(context);
        checkNotNull(user);

        final IPFS ipfs = Singleton.getInstance(context).getIpfs();

        final THREADS threads = Singleton.getInstance(context).getThreads();

        if (!threads.existsUser(user)) {

            String alias = user.getPid();

            checkNotNull(ipfs, "IPFS is not valid");
            CID image = ThumbnailService.getImage(
                    context, alias, "", R.drawable.server_network);

            User newUser = threads.createUser(user, "",
                    alias, UserType.VERIFIED, image);
            newUser.setStatus(UserStatus.OFFLINE);
            threads.storeUser(newUser);

        } else {
            Preferences.warning(threads, context.getString(R.string.peer_exists_with_pid));
            return;
        }


        try {
            threads.setUserStatus(user, UserStatus.DIALING);

            final int timeout = Preferences.getConnectionTimeout(context);
            final boolean peerDiscovery = Service.isSupportPeerDiscovery(context);
            boolean value = IdentityService.connectPeer(context, user,
                    BuildConfig.ApiAesKey, peerDiscovery, true, true);
            if (value) {
                threads.setUserStatus(user, UserStatus.ONLINE);
                PID host = Preferences.getPID(context);
                checkNotNull(host);

                Content map = new Content();
                map.put(Content.EST, "CONNECT");
                map.put(Content.ALIAS, threads.getUserAlias(host));
                map.put(Content.PKEY, threads.getUserPublicKey(host));

                Singleton.getInstance(context).
                        getConsoleListener().info(
                        "Send Notification to PID :" + user);


                ipfs.pubsubPub(user.getPid(), gson.toJson(map), 50);


                if (threads.getUserPublicKey(user).isEmpty()) {

                    PeerInfo info = ipfs.id(user, timeout);
                    if (info != null) {
                        String key = info.getPublicKey();
                        if (key != null) {
                            threads.setUserPublicKey(user, key);
                        }
                    }
                }

            } else {
                threads.setUserStatus(user, UserStatus.OFFLINE);
            }

            threads.core.api.PeerInfo peerInfo = IdentityService.getPeerInfo(context, user,
                    BuildConfig.ApiAesKey);
            if (peerInfo != null) {
                String name = peerInfo.getAdditional(Content.ALIAS);
                if (!name.isEmpty()) {
                    threads.setUserAlias(user, name);
                }
            }


        } catch (Throwable e) {
            threads.setUserStatus(user, UserStatus.OFFLINE);
        }
    }

    private static void runUpdatesIfNecessary(@NonNull Context context) {
        try {
            int versionCode = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
            SharedPreferences prefs = context.getSharedPreferences(
                    APP_KEY, Context.MODE_PRIVATE);
            if (prefs.getInt(UPDATE, 0) != versionCode) {


                // Experimental Features
                Preferences.setQUICEnabled(context, true);
                Preferences.setFilestoreEnabled(context, false);
                Preferences.setPreferTLS(context, false);


                Preferences.setSwarmPort(context, 4001);
                Preferences.setRoutingType(context, RoutingConfig.TypeEnum.dhtclient);


                Preferences.setAutoNATServiceEnabled(context, true);
                Preferences.setRelayHopEnabled(context, false);
                Preferences.setAutoRelayEnabled(context, true);

                Preferences.setPubsubEnabled(context, true);
                Preferences.setPubsubRouter(context, PubsubConfig.RouterEnum.gossipsub);
                Preferences.setReproviderInterval(context, "0");

                Preferences.setConnMgrConfigType(context, ConnMgrConfig.TypeEnum.basic);
                Preferences.setLowWater(context, 50);
                Preferences.setHighWater(context, 200);
                Preferences.setGracePeriod(context, "10s");


                Preferences.setConnectionTimeout(context, 45000);
                Preferences.setTangleTimeout(context, 30);

                Preferences.setMdnsEnabled(context, true);

                Preferences.setReportMode(context, true);
                Preferences.setDebugMode(context, false);


                setDontShowAgain(context, Service.PIN_SERVICE_KEY, false);

                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt(UPDATE, versionCode);
                editor.apply();
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    private static boolean handleDirectoryLink(@NonNull Context context,
                                               @NonNull THREADS threads,
                                               @NonNull IPFS ipfs,
                                               @NonNull Thread thread,
                                               @NonNull LinkInfo link) {

        List<LinkInfo> links = getLinks(context, ipfs, link.getCid());
        if (links != null) {
            return downloadLinks(context, threads, ipfs, thread, links);
        } else {
            return false;
        }

    }

    private static void adaptUser(@NonNull Context context,
                                  @NonNull PID senderPid,
                                  @NonNull String alias,
                                  @NonNull String pubKey) {
        checkNotNull(context);
        checkNotNull(senderPid);
        checkNotNull(alias);
        checkNotNull(pubKey);

        try {
            final THREADS threadsAPI = Singleton.getInstance(context).getThreads();

            User sender = threadsAPI.getUserByPID(senderPid);
            checkNotNull(sender);


            CID image = ThumbnailService.getImage(
                    context, alias, "", R.drawable.server_network);

            sender.setPublicKey(pubKey);
            sender.setAlias(alias);
            sender.setImage(image);
            sender.setType(UserType.VERIFIED);

            threadsAPI.storeUser(sender);


        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    private static void receiveReply(@NonNull Context context,
                                     @NonNull PID sender,
                                     @NonNull String multihash) {
        checkNotNull(context);
        checkNotNull(sender);
        checkNotNull(multihash);


        final THREADS threads = Singleton.getInstance(context).getThreads();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                CID cid = CID.create(multihash);
                List<Thread> entries = threads.getThreadsByCID(cid);
                for (Thread thread : entries) {
                    threads.incrementUnreadNotesNumber(thread);
                }
            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }
        });


    }


    static String getAddressLink(@NonNull String address) {
        return "https://thetangle.org/address/" + address;
    }

    private static void createUser(@NonNull Context context,
                                   @NonNull PID senderPid,
                                   @NonNull String alias,
                                   @NonNull String pubKey,
                                   @NonNull UserType userType) {
        checkNotNull(context);
        checkNotNull(senderPid);
        checkNotNull(alias);
        checkNotNull(pubKey);
        checkNotNull(userType);

        try {
            final THREADS threads = Singleton.getInstance(context).getThreads();
            final IPFS ipfs = Singleton.getInstance(context).getIpfs();
            if (ipfs != null) {
                User sender = threads.getUserByPID(senderPid);
                if (sender == null) {

                    // create a new user which is blocked (User has to unblock and verified the user)
                    CID image = ThumbnailService.getImage(
                            context, alias, "", R.drawable.server_network);

                    sender = threads.createUser(senderPid, pubKey, alias, userType, image);
                    sender.setBlocked(true);
                    threads.storeUser(sender);

                    Preferences.error(threads, context.getString(R.string.user_connect_try, alias));
                }

                PID host = Preferences.getPID(context);
                checkNotNull(host);
                User hostUser = threads.getUserByPID(host);
                checkNotNull(hostUser);
                Content map = new Content();
                map.put(Content.EST, "CONNECT_REPLY");
                map.put(Content.ALIAS, hostUser.getAlias());
                map.put(Content.PKEY, hostUser.getPublicKey());


                Singleton.getInstance(context).
                        getConsoleListener().info(
                        "Send Notification to PID :" + senderPid);

                ipfs.pubsubPub(senderPid.getPid(), gson.toJson(map), 50);


            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    static void replySender(@NonNull Context context,
                            @NonNull IPFS ipfs,
                            @NonNull PID sender,
                            @NonNull Thread thread) {
        try {
            if (Preferences.isPubsubEnabled(context)) {
                CID cid = thread.getCid();
                checkNotNull(cid);

                Content map = new Content();
                map.put(Content.EST, "REPLY");
                map.put(Content.CID, cid.getCid());


                Singleton.getInstance(context).getConsoleListener().info(
                        "Send Pubsub Notification to PID :" + sender);

                ipfs.pubsubPub(sender.getPid(), gson.toJson(map), 50);
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }


    private static String getDeviceName() {
        try {
            String manufacturer = Build.MANUFACTURER;
            String model = Build.MODEL;
            if (model.startsWith(manufacturer)) {
                return capitalize(model);
            }
            return capitalize(manufacturer) + " " + model;
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
        return "";
    }

    private static String capitalize(String str) {
        if (TextUtils.isEmpty(str)) {
            return str;
        }
        char[] arr = str.toCharArray();
        boolean capitalizeNext = true;
        String phrase = "";
        for (char c : arr) {
            if (capitalizeNext && Character.isLetter(c)) {
                phrase = phrase.concat("" + Character.toUpperCase(c));
                capitalizeNext = false;
                continue;
            } else if (Character.isWhitespace(c)) {
                capitalizeNext = true;
            }
            phrase = phrase.concat("" + c);
        }
        return phrase;
    }

    private static void cleanStates(@NonNull Context context) {
        checkNotNull(context);

        final THREADS threads = Singleton.getInstance(context).getThreads();

        try {
            threads.setUserStatus(UserStatus.DIALING, UserStatus.OFFLINE);
            threads.setThreadStatus(ThreadStatus.PUBLISHING, ThreadStatus.ONLINE);
            threads.setThreadStatus(ThreadStatus.LEACHING, ThreadStatus.ERROR);
            threads.setThreadStatus(ThreadStatus.OFFLINE, ThreadStatus.ERROR);
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }

    }

    private static void createHost(@NonNull Context context) {
        checkNotNull(context);

        final THREADS threads = Singleton.getInstance(context).getThreads();
        final IPFS ipfs = Singleton.getInstance(context).getIpfs();
        if (ipfs != null) {
            try {
                PID pid = Preferences.getPID(context);
                checkNotNull(pid);

                User user = threads.getUserByPID(pid);
                if (user == null) {
                    String publicKey = ipfs.getPublicKey();

                    CID image = ThumbnailService.getImage(
                            context, pid.getPid(), "", R.drawable.server_network);


                    user = threads.createUser(pid, publicKey, getDeviceName(),
                            UserType.VERIFIED, image);
                    user.setBlocked(true);
                    user.setStatus(UserStatus.ONLINE);
                    threads.storeUser(user);
                } else {
                    threads.setUserStatus(pid, UserStatus.ONLINE);
                    threads.blockUser(pid);
                }
            } catch (Throwable e) {
                Preferences.evaluateException(threads, Preferences.EXCEPTION, e);
            }
        }
    }

    static void removeThreads(@NonNull Context context, long... idxs) {
        checkNotNull(context);
        checkNotNull(idxs);
        final THREADS threads = Singleton.getInstance(context).getThreads();
        final IPFS ipfs = Singleton.getInstance(context).getIpfs();

        try {
            checkNotNull(ipfs, "IPFS is not valid");
            threads.setThreadsStatus(ThreadStatus.DELETING, idxs);

            threads.removeThreads(ipfs, idxs);

        } catch (Throwable e) {
            Preferences.evaluateException(threads, Preferences.EXCEPTION, e);
        }


    }

    @NonNull
    private static String evaluateMimeType(@NonNull Context context, @NonNull String filename) {
        final THREADS threads = Singleton.getInstance(context).getThreads();
        try {
            Optional<String> extension = ThumbnailService.getExtension(filename);
            if (extension.isPresent()) {
                String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.get());
                if (mimeType != null) {
                    return mimeType;
                }
            }
        } catch (Throwable e) {
            Preferences.evaluateException(threads, Preferences.EXCEPTION, e);
        }
        return MimeType.OCTET_MIME_TYPE;
    }

    private static long createThread(@NonNull Context context,
                                     @NonNull IPFS ipfs,
                                     @NonNull PID creator,
                                     @NonNull CID cid,
                                     @NonNull LinkInfo link,
                                     long parent) {

        checkNotNull(context);
        checkNotNull(ipfs);
        checkNotNull(creator);
        checkNotNull(cid);
        checkNotNull(link);


        final THREADS threads = Singleton.getInstance(context).getThreads();

        User user = threads.getUserByPID(creator);
        checkNotNull(user);

        Thread thread = threads.createThread(user, ThreadStatus.OFFLINE, Kind.OUT,
                "", cid, parent);

        if (link.isDirectory()) {
            thread.addAdditional(Preferences.THREAD_KIND, ThreadKind.NODE.name(), false);
        } else {
            thread.addAdditional(Preferences.THREAD_KIND, ThreadKind.LEAF.name(), false);
        }

        String filename = link.getName();
        thread.addAdditional(Content.FILENAME, filename, false);

        long size = link.getSize();
        thread.addAdditional(Content.FILESIZE, String.valueOf(size), false);

        if (link.isDirectory()) {
            thread.setMimeType(DocumentsContract.Document.MIME_TYPE_DIR);
        } else {
            thread.setMimeType(evaluateMimeType(context, filename));
        }


        if (link.isDirectory()) {
            try {
                CID image = ThumbnailService.createResourceImage(context, threads, ipfs,
                        R.drawable.folder_outline, "");
                if (image != null) {
                    thread.addAdditional(Content.IMG, String.valueOf(false), true);
                    thread.setImage(image);
                }
            } catch (Throwable e) {
                Preferences.evaluateException(threads, Preferences.EXCEPTION, e);
            }
        } else {
            try {
                CID image = ThumbnailService.getImage(context.getApplicationContext(),
                        user.getAlias(), "", R.drawable.file);
                thread.addAdditional(Content.IMG, String.valueOf(false), true);
                thread.setImage(image);
            } catch (Throwable e) {
                Preferences.evaluateException(threads, Preferences.EXCEPTION, e);
            }
        }
        return threads.storeThread(thread);
    }

    public static long createThread(@NonNull Context context,
                                    @NonNull IPFS ipfs,
                                    @NonNull User creator,
                                    @NonNull CID cid,
                                    @NonNull ThreadStatus threadStatus,
                                    @Nullable String filename,
                                    @Nullable String filesize,
                                    @Nullable String mimeType,
                                    @Nullable CID image) {

        checkNotNull(context);
        checkNotNull(ipfs);
        checkNotNull(creator);
        checkNotNull(cid);
        checkNotNull(threadStatus);


        final THREADS threads = Singleton.getInstance(context).getThreads();


        Thread thread = threads.createThread(creator, threadStatus, Kind.OUT,
                "", cid, 0L);
        thread.addAdditional(Preferences.THREAD_KIND, ThreadKind.LEAF.name(), false); // not known yet
        if (filename != null) {
            thread.addAdditional(Content.FILENAME, filename, false);
            if (mimeType != null) {
                thread.setMimeType(mimeType);
            } else {
                thread.setMimeType(evaluateMimeType(context, filename));
            }
        } else {
            if (mimeType != null) {
                thread.setMimeType(mimeType);
            } else {
                thread.setMimeType(MimeType.OCTET_MIME_TYPE); // not known yet
            }
            thread.addAdditional(Content.FILENAME, cid.getCid(), false);
        }
        if (filesize != null) {
            thread.addAdditional(Content.FILESIZE, filesize, false);
        } else {
            thread.addAdditional(Content.FILESIZE, "-1", false);
        }


        try {
            if (image == null) {
                int resource = MimeTypeService.getMediaResource(thread.getMimeType(),
                        false);
                if (resource <= 0) {
                    resource = R.drawable.file;
                }
                thread.addAdditional(Content.IMG, String.valueOf(false), true);
                thread.setImage(ThumbnailService.getImage(context.getApplicationContext(),
                        creator.getAlias(), "", resource));
            } else {
                thread.addAdditional(Content.IMG, String.valueOf(true), true);
                thread.setImage(image);
            }
        } catch (Throwable e) {
            Preferences.evaluateException(threads, Preferences.EXCEPTION, e);
        }

        return threads.storeThread(thread);
    }

    static void localDownloadThread(@NonNull Context context, long idx) {
        checkNotNull(context);
        try {
            final THREADS threadsAPI = Singleton.getInstance(context).getThreads();
            final DownloadManager downloadManager = (DownloadManager)
                    context.getSystemService(Context.DOWNLOAD_SERVICE);
            checkNotNull(downloadManager);

            final IPFS ipfs = Singleton.getInstance(context).getIpfs();

            if (ipfs != null) {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.submit(() -> {
                    try {
                        Thread threadObject = threadsAPI.getThreadByIdx(idx);
                        checkNotNull(threadObject);

                        CID cid = threadObject.getCid();
                        checkNotNull(cid);

                        int timeout = Preferences.getConnectionTimeout(context);

                        String name = threadObject.getAdditional(Content.FILENAME);
                        long size = -1;
                        try {
                            size = Long.valueOf(threadObject.getAdditional(Content.FILESIZE));
                        } catch (Throwable e) {
                            Log.e(TAG, "" + e.getLocalizedMessage(), e);
                        }

                        File dir = Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_DOWNLOADS);
                        File file = new File(dir, name);


                        NotificationCompat.Builder builder =
                                ProgressChannel.createProgressNotification(
                                        context, name);

                        final NotificationManager notificationManager = (NotificationManager)
                                context.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
                        int notifyID = cid.hashCode();
                        Notification notification = builder.build();
                        if (notificationManager != null) {
                            notificationManager.notify(notifyID, notification);
                        }

                        try {
                            boolean finished = threadsAPI.download(ipfs, file, cid, "",
                                    new IPFS.Progress() {
                                        @Override
                                        public void setProgress(int percent) {
                                            builder.setProgress(100, percent, false);
                                            if (notificationManager != null) {
                                                notificationManager.notify(notifyID, builder.build());
                                            }
                                        }

                                        @Override
                                        public boolean isStopped() {
                                            return !Network.isConnected(context);
                                        }
                                    }, false, true, timeout, size);

                            if (finished) {
                                String mimeType = threadObject.getMimeType();
                                checkNotNull(mimeType);

                                downloadManager.addCompletedDownload(file.getName(),
                                        file.getName(), true,
                                        mimeType,
                                        file.getAbsolutePath(),
                                        file.length(), true);
                            }
                        } catch (Throwable e) {
                            Preferences.evaluateException(threadsAPI, Preferences.EXCEPTION, e);
                        } finally {

                            if (notificationManager != null) {
                                notificationManager.cancel(notifyID);
                            }
                        }

                    } catch (Throwable e) {
                        Preferences.evaluateException(threadsAPI, Preferences.EXCEPTION, e);
                    }
                });
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    private static boolean downloadThread(@NonNull Context context,
                                          @NonNull THREADS threads,
                                          @NonNull IPFS ipfs,
                                          @NonNull Thread thread) {

        checkNotNull(context);
        checkNotNull(threads);
        checkNotNull(ipfs);
        checkNotNull(thread);

        CID cid = thread.getCid();
        checkNotNull(cid);

        String filename = thread.getAdditional(Content.FILENAME);
        String filesize = thread.getAdditional(Content.FILESIZE);

        return download(context, threads, ipfs, thread, cid, filename, Long.valueOf(filesize));
    }

    private static boolean download(@NonNull Context context,
                                    @NonNull THREADS threads,
                                    @NonNull IPFS ipfs,
                                    @NonNull Thread thread,
                                    @NonNull CID cid,
                                    @NonNull String filename,
                                    long size) {

        checkNotNull(context);
        checkNotNull(threads);
        checkNotNull(ipfs);
        checkNotNull(thread);
        checkNotNull(cid);
        checkNotNull(filename);


        NotificationCompat.Builder builder =
                ProgressChannel.createProgressNotification(
                        context, filename);

        final NotificationManager notificationManager = (NotificationManager)
                context.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        int notifyID = cid.getCid().hashCode();
        Notification notification = builder.build();
        if (notificationManager != null) {
            notificationManager.notify(notifyID, notification);
        }

        boolean success;
        try {
            threads.setStatus(thread, ThreadStatus.LEACHING);
            int timeout = Preferences.getConnectionTimeout(context);
            File file = threads.receive(ipfs, cid, "",
                    new IPFS.Progress() {
                        @Override
                        public void setProgress(int percent) {
                            builder.setProgress(100, percent, false);
                            if (notificationManager != null) {
                                notificationManager.notify(notifyID, builder.build());
                            }
                        }

                        @Override
                        public boolean isStopped() {
                            return !Network.isConnected(context);
                        }
                    }, timeout, size);

            if (file != null) {
                success = true;


                // Now check if MIME TYPE of thread can be re-evaluated
                if (threads.getMimeType(thread).equals(MimeType.OCTET_MIME_TYPE)) {
                    ContentInfo contentInfo = ipfs.getContentInfo(file);
                    if (contentInfo != null) {
                        String mimeType = contentInfo.getMimeType();
                        if (mimeType != null) {
                            threads.setMimeType(thread, mimeType);
                        }
                    }
                }

                // check if image was imported
                try {
                    String img = thread.getAdditional(Content.IMG);
                    if (img.isEmpty() || !Boolean.valueOf(img)) {
                        ThumbnailService.Result res = ThumbnailService.getThumbnail(
                                context, file, filename, "");
                        threads.setAdditional(thread,
                                Content.IMG, String.valueOf(res.isThumbnail()), true);
                        CID image = res.getCid();
                        if (image != null) {
                            threads.setImage(thread, image);
                        }
                    }
                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
                }
            } else {
                success = false;
            }
        } catch (Throwable e) {
            success = false;
        } finally {
            if (notificationManager != null) {
                notificationManager.cancel(notifyID);
            }
        }

        return success;
    }

    private static boolean downloadLink(@NonNull Context context,
                                        @NonNull THREADS threads,
                                        @NonNull IPFS ipfs,
                                        @NonNull Thread thread,
                                        @NonNull LinkInfo link) {
        if (link.isDirectory()) {
            return handleDirectoryLink(context, threads, ipfs, thread, link);
        } else {
            return download(context, threads, ipfs, thread,
                    link.getCid(), link.getName(), link.getSize());
        }


    }

    private static Thread getDirectoryThread(@NonNull THREADS threads,
                                             @NonNull Thread thread,
                                             @NonNull CID cid) {
        List<Thread> entries = threads.getThreadsByCID(cid);
        if (!entries.isEmpty()) {
            for (Thread entry : entries) {
                if (entry.getThread() == thread.getIdx()) {
                    return entry;
                }
            }
        }
        return null;
    }

    private static boolean downloadLinks(@NonNull Context context,
                                         @NonNull THREADS threads,
                                         @NonNull IPFS ipfs,
                                         @NonNull Thread thread,
                                         @NonNull List<LinkInfo> links) {

        AtomicInteger successCounter = new AtomicInteger(0);
        for (LinkInfo link : links) {

            CID cid = link.getCid();
            Thread entry = getDirectoryThread(threads, thread, cid);
            if (entry != null) {
                if (entry.getStatus() != ThreadStatus.ONLINE) {

                    boolean success = downloadLink(context, threads, ipfs, entry, link);
                    if (success) {
                        successCounter.incrementAndGet();
                    }

                } else {
                    successCounter.incrementAndGet();
                }
            } else {

                long idx = createThread(context, ipfs,
                        thread.getSenderPid(), cid, link, thread.getIdx());
                entry = threads.getThreadByIdx(idx);
                checkNotNull(entry);
                boolean success = downloadLink(context, threads, ipfs, entry, link);

                if (success) {
                    successCounter.incrementAndGet();
                    threads.setStatus(entry, ThreadStatus.ONLINE);
                } else {
                    threads.setStatus(entry, ThreadStatus.ERROR);
                }
            }

        }

        return successCounter.get() == links.size();
    }

    @Nullable
    private static List<LinkInfo> getLinks(@NonNull Context context,
                                           @NonNull IPFS ipfs,
                                           @NonNull CID cid) {
        checkNotNull(context);
        checkNotNull(cid);
        int timeout = Preferences.getConnectionTimeout(context);
        List<LinkInfo> links = ipfs.ls(cid, timeout, false);
        if (links == null) {
            return null;
        }
        List<LinkInfo> result = new ArrayList<>();
        for (LinkInfo link : links) {
            if (!link.getName().isEmpty()) {
                result.add(link);
            }
        }
        return result;
    }

    static void downloadMultihash(@NonNull Context context,
                                  @NonNull THREADS threads,
                                  @NonNull IPFS ipfs,
                                  @NonNull Thread thread,
                                  @Nullable PID sender) {
        checkNotNull(context);
        checkNotNull(threads);
        checkNotNull(ipfs);
        checkNotNull(thread);


        threads.setStatus(thread, ThreadStatus.LEACHING);

        CID cid = thread.getCid();
        checkNotNull(cid);

        List<LinkInfo> links = getLinks(context, ipfs, cid);

        if (links != null) {
            if (links.isEmpty()) {

                boolean result = downloadThread(context, threads, ipfs, thread);
                if (result) {
                    threads.setStatus(thread, ThreadStatus.ONLINE);
                    if (sender != null) {
                        replySender(context, ipfs, sender, thread);
                    }
                } else {
                    threads.setStatus(thread, ThreadStatus.ERROR);
                }

            } else {

                // thread is directory

                if (!thread.getMimeType().equals(DocumentsContract.Document.MIME_TYPE_DIR)) {
                    threads.setMimeType(thread, DocumentsContract.Document.MIME_TYPE_DIR);
                    threads.setAdditional(thread, Preferences.THREAD_KIND,
                            ThreadKind.NODE.name(), true);

                    try {
                        CID image = ThumbnailService.createResourceImage(context, threads, ipfs,
                                R.drawable.folder_outline, "");
                        checkNotNull(image);
                        threads.setImage(thread, image);
                    } catch (Throwable e) {
                        Preferences.evaluateException(threads, Preferences.EXCEPTION, e);
                    }
                }


                boolean result = downloadLinks(context, threads, ipfs, thread, links);
                if (result) {
                    threads.setStatus(thread, ThreadStatus.ONLINE);
                    if (sender != null) {
                        replySender(context, ipfs, sender, thread);
                    }
                } else {
                    threads.setStatus(thread, ThreadStatus.ERROR);
                }
            }
        } else {
            threads.setStatus(thread, ThreadStatus.ERROR);
        }

    }


    void storeData(@NonNull Context context, @NonNull String text) {
        checkNotNull(context);
        checkNotNull(text);

        final THREADS threads = Singleton.getInstance(context).getThreads();


        final IPFS ipfs = Singleton.getInstance(context).getIpfs();

        if (ipfs != null) {

            UPLOAD_SERVICE.submit(() -> {
                try {

                    PID pid = Preferences.getPID(context);
                    checkNotNull(pid);
                    User host = threads.getUserByPID(pid);
                    checkNotNull(host);

                    String content;
                    String mimeType = MimeType.PLAIN_MIME_TYPE;
                    try {

                        URL url = new URL(text);
                        mimeType = MimeType.LINK_MIME_TYPE;
                        content = url.toString();
                    } catch (MalformedURLException e) {
                        content = StringUtils.substring(text, 0, 80);
                    }

                    long size = text.length();

                    Thread thread = threads.createThread(host, ThreadStatus.OFFLINE, Kind.IN,
                            "", null, 0L);
                    thread.addAdditional(Content.IMG, String.valueOf(false), true);
                    thread.addAdditional(Content.FILENAME, content, false);
                    thread.addAdditional(Preferences.THREAD_KIND, ThreadKind.LEAF.name(), false);
                    thread.addAdditional(Content.FILESIZE, String.valueOf(size), false);

                    int resource = MimeTypeService.getMediaResource(
                            mimeType, false);

                    CID image = ThumbnailService.getImage(
                            context, host.getAlias(), "", resource);
                    thread.setImage(image);
                    thread.setMimeType(mimeType);

                    long idx = threads.storeThread(thread);

                    Preferences.event(threads, Preferences.THREAD_SCROLL_EVENT, "");


                    try {
                        threads.setThreadStatus(idx, ThreadStatus.LEACHING);

                        CID cid = ipfs.add(text, "", true);
                        checkNotNull(cid);

                        // cleanup of entries with same CID
                        List<Thread> sameEntries = threads.getThreadsByCID(cid);
                        for (Thread entry : sameEntries) {
                            threads.removeThread(entry);
                        }

                        threads.setThreadCID(idx, cid);
                        threads.setThreadStatus(idx, ThreadStatus.ONLINE);
                    } catch (Throwable e) {
                        threads.setThreadStatus(idx, ThreadStatus.ERROR);
                    } finally {
                        Preferences.event(threads, Preferences.THREAD_SCROLL_EVENT, "");
                    }

                } catch (Throwable e) {
                    Preferences.evaluateException(threads, Preferences.EXCEPTION, e);
                }
            });
        }
    }

    void storeData(@NonNull Context context, @NonNull Uri uri) {
        checkNotNull(context);
        checkNotNull(uri);

        final THREADS threads = Singleton.getInstance(context).getThreads();
        ThumbnailService.FileDetails fileDetails = ThumbnailService.getFileDetails(context, uri);
        if (fileDetails == null) {
            Preferences.error(threads, context.getString(R.string.file_not_supported));
            return;
        }

        final IPFS ipfs = Singleton.getInstance(context).getIpfs();

        if (ipfs != null) {

            UPLOAD_SERVICE.submit(() -> {
                try {
                    InputStream inputStream =
                            context.getContentResolver().openInputStream(uri);
                    checkNotNull(inputStream);

                    PID pid = Preferences.getPID(context);
                    checkNotNull(pid);
                    User host = threads.getUserByPID(pid);
                    checkNotNull(host);


                    String name = fileDetails.getFileName();
                    long size = fileDetails.getFileSize();

                    Thread thread = threads.createThread(host, ThreadStatus.OFFLINE, Kind.IN,
                            "", null, 0L);

                    ThumbnailService.Result res =
                            ThumbnailService.getThumbnail(context, uri, host.getAlias(), "");

                    thread.addAdditional(Content.IMG, String.valueOf(res.isThumbnail()), true);
                    thread.addAdditional(Content.FILENAME, name, false);
                    thread.addAdditional(Preferences.THREAD_KIND, ThreadKind.LEAF.name(), false);
                    thread.addAdditional(Content.FILESIZE, String.valueOf(size), false);

                    thread.setImage(res.getCid());
                    thread.setMimeType(fileDetails.getMimeType());
                    long idx = threads.storeThread(thread);

                    Preferences.event(threads, Preferences.THREAD_SCROLL_EVENT, "");


                    try {
                        threads.setThreadStatus(idx, ThreadStatus.LEACHING);

                        CID cid = ipfs.add(inputStream, "", true);
                        checkNotNull(cid);

                        // cleanup of entries with same CID
                        List<Thread> sameEntries = threads.getThreadsByCID(cid);
                        for (Thread entry : sameEntries) {
                            threads.removeThread(entry);
                        }

                        threads.setThreadCID(idx, cid);
                        threads.setThreadStatus(idx, ThreadStatus.ONLINE);
                    } catch (Throwable e) {
                        threads.setThreadStatus(idx, ThreadStatus.ERROR);
                    } finally {
                        Preferences.event(threads, Preferences.THREAD_SCROLL_EVENT, "");
                    }

                } catch (Throwable e) {
                    Preferences.evaluateException(threads, Preferences.EXCEPTION, e);
                }
            });
        }
    }

    void peersCheckEnable(boolean value) {
        peerCheckFlag.set(value);
    }

    void checkPeersOnlineStatus(@NonNull Context context) {
        checkNotNull(context);
        final IPFS ipfs = Singleton.getInstance(context).getIpfs();
        if (ipfs != null) {
            try {
                while (peerCheckFlag.get() && ipfs.isDaemonRunning()) {
                    checkPeers(context);
                    java.lang.Thread.sleep(1000);
                }
            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }
        }
    }

    private void checkPeers(@NonNull Context context) {
        checkNotNull(context);

        try {
            final PID host = Preferences.getPID(context);
            final THREADS threads = Singleton.getInstance(context).getThreads();


            final IPFS ipfs = Singleton.getInstance(context).getIpfs();
            if (ipfs != null) {

                List<PID> users = threads.getUsersPIDs();

                users.remove(host);


                for (PID user : users) {
                    if (!threads.isUserBlocked(user)) {
                        UserStatus currentStatus = threads.getUserStatus(user);
                        if (currentStatus != UserStatus.DIALING) {
                            try {
                                boolean value = ipfs.isConnected(user);

                                currentStatus = threads.getUserStatus(user);
                                if (currentStatus != UserStatus.DIALING) {
                                    if (value) {
                                        if (currentStatus != UserStatus.ONLINE) {
                                            threads.setUserStatus(user, UserStatus.ONLINE);
                                        }
                                    } else {
                                        if (currentStatus != UserStatus.OFFLINE) {
                                            threads.setUserStatus(user, UserStatus.OFFLINE);
                                        }
                                    }
                                }
                            } catch (Throwable e) {
                                if (threads.getUserStatus(user) != UserStatus.DIALING) {
                                    threads.setUserStatus(user, UserStatus.OFFLINE);
                                }
                            }
                        }
                    }
                }
            }

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    ArrayList<String> getEnhancedUserPIDs(@NonNull Context context) {
        checkNotNull(context);

        final THREADS threads = Singleton.getInstance(context).getThreads();
        final PID pid = Preferences.getPID(context);
        ArrayList<String> users = new ArrayList<>();
        checkNotNull(pid);

        for (User user : threads.getUsers()) {

            if (!user.getPID().equals(pid)) {
                if (!threads.isUserBlocked(user.getPID())) {
                    //if(threads.getPeerByPID(user.getPID()) != null) {
                    users.add(user.getPID().getPid());
                    //}
                }
            }
        }
        return users;
    }

    void downloadThread(@NonNull Context context, @NonNull Thread thread) {

        checkNotNull(context);
        checkNotNull(thread);
        final boolean peerDiscovery = Service.isSupportPeerDiscovery(context);
        final THREADS threads = Singleton.getInstance(context).getThreads();
        final IPFS ipfs = Singleton.getInstance(context).getIpfs();
        if (ipfs != null) {
            threads.setStatus(thread, ThreadStatus.LEACHING);
            PID host = Preferences.getPID(context);
            checkNotNull(host);
            PID sender = thread.getSenderPid();
            if (!host.equals(sender)) {

                IdentityService.connectPeer(context, sender,
                        BuildConfig.ApiAesKey, peerDiscovery, true, true);

            }

            // TODO maybe also just download when connection established

            Service.downloadMultihash(context, threads, ipfs, thread, null);

        }

    }


    private void sharePeer(@NonNull Context context, @NonNull User user, long[] idxs) {
        checkNotNull(user);
        checkNotNull(idxs);
        final THREADS threads = Singleton.getInstance(context).getThreads();
        final IPFS ipfs = Singleton.getInstance(context).getIpfs();
        final ContentService contentService = ContentService.getInstance(context);
        final PID host = Preferences.getPID(context);
        final int timeout = Preferences.getConnectionTimeout(context);
        final boolean peerDiscovery = Service.isSupportPeerDiscovery(context);


        if (ipfs != null) {
            try {
                Contents contents = new Contents();

                List<Thread> threadList = threads.getThreadByIdxs(idxs);
                contents.add(threadList);

                CID cid = ipfs.add(gson.toJson(contents), "", true);
                checkNotNull(cid);

                checkNotNull(host);
                contentService.insertContent(host, cid, true);


                long start = System.currentTimeMillis();
                boolean cleanStoredPeers = DaemonService.DAEMON_RUNNING.get()
                        && Network.isConnected(context) && (DaemonService.running() > timeout);
                boolean success = false;
                if (peerDiscovery) {
                    success = IdentityService.publishIdentity(
                            context, BuildConfig.ApiAesKey, false,
                            timeout, Service.RELAYS, true, cleanStoredPeers);
                }

                String hash = null;
                if (success) {
                    hash = threads.getPeerInfoHash(host);
                }
                Service.notify(context, user.getPID().getPid(), cid.getCid(), hash, start);


                if (ipfs.isConnected(user.getPID())) {
                    boolean enabled = Preferences.isPubsubEnabled(context);
                    if (enabled) {
                        ipfs.pubsubPub(user.getPID().getPid(), cid.getCid(), 50);
                    }
                }


            } catch (Throwable e) {
                Preferences.evaluateException(threads, Preferences.EXCEPTION, e);
            }
        }

    }


    void sendThreads(@NonNull Context context, @NonNull List<User> users, long[] idxs) {
        checkNotNull(context);
        checkNotNull(users);
        checkNotNull(idxs);

        final PID host = Preferences.getPID(context.getApplicationContext());

        final THREADS threads = Singleton.getInstance(context).getThreads();
        final IPFS ipfs = Singleton.getInstance(context).getIpfs();

        if (ipfs != null) {

            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    // clean-up (unset the unread number)
                    threads.resetThreadsUnreadNotesNumber(idxs);

                    if (users.isEmpty()) {
                        Preferences.error(threads, context.getString(R.string.no_sharing_peers));
                    } else {
                        checkNotNull(host);

                        threads.setThreadsStatus(ThreadStatus.PUBLISHING, idxs);


                        // TODO maybe in the future it might sense to make it parallel
                        ExecutorService executorService = Executors.newFixedThreadPool(1);
                        List<Future> futures = new ArrayList<>();

                        for (User user : users) {
                            futures.add(executorService.submit(() ->
                                    sharePeer(context, user, idxs)));
                        }


                        for (Future future : futures) {
                            future.get();
                        }

                        threads.setThreadsStatus(ThreadStatus.ONLINE, idxs);
                    }

                } catch (Throwable e) {
                    Preferences.evaluateException(threads, Preferences.EXCEPTION, e);
                }
            });


        }
    }

    private void init(@NonNull Context context) {
        checkNotNull(context);
        new java.lang.Thread(() -> {
            try {
                Service.cleanStates(context);
                Service.createHost(context);
            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }
        }).start();

    }

    private void startDaemon(@NonNull Context context) {
        checkNotNull(context);
        try {
            final IPFS ipfs = Singleton.getInstance(context).getIpfs();
            final THREADS threads = Singleton.getInstance(context).getThreads();
            if (ipfs != null) {

                try {
                    Singleton.getInstance(context).getConsoleListener().debug("Start Daemon");

                    ipfs.daemon(new PubsubReader() {
                        @Override
                        public void receive(@NonNull PubsubInfo message) {
                            try {

                                String sender = message.getSenderPid();
                                PID senderPid = PID.create(sender);


                                if (!threads.isUserBlocked(senderPid)) {

                                    String code = message.getMessage().trim();

                                    CodecDecider result = CodecDecider.evaluate(code);

                                    if (result.getCodex() == CodecDecider.Codec.MULTIHASH) {
                                        JobServiceContents.contents(context,
                                                senderPid, CID.create(result.getMultihash()));
                                    } else if (result.getCodex() == CodecDecider.Codec.URI) {
                                        JobServiceMultihash.download(context,
                                                senderPid, CID.create(result.getMultihash()));

                                    } else if (result.getCodex() == CodecDecider.Codec.CONTENT) {
                                        Content content = result.getContent();
                                        checkNotNull(content);
                                        if (content.containsKey(Content.EST)) {
                                            String est = content.get(Content.EST);
                                            if ("CONNECT".equals(est)) {
                                                if (content.containsKey(Content.ALIAS)) {
                                                    String alias = content.get(Content.ALIAS);
                                                    checkNotNull(alias);
                                                    String pubKey = content.get(Content.PKEY);
                                                    if (pubKey == null) {
                                                        pubKey = "";
                                                    }
                                                    createUser(context, senderPid,
                                                            alias, pubKey, UserType.VERIFIED);
                                                }
                                            } else if ("CONNECT_REPLY".equals(est)) {
                                                if (content.containsKey(Content.ALIAS)) {
                                                    String alias = content.get(Content.ALIAS);
                                                    checkNotNull(alias);
                                                    String pubKey = content.get(Content.PKEY);
                                                    if (pubKey == null) {
                                                        pubKey = "";
                                                    }
                                                    adaptUser(context, senderPid, alias, pubKey);
                                                }
                                            } else if ("REPLY".equals(est)) {

                                                if (content.containsKey(Content.CID)) {
                                                    String cid = content.get(Content.CID);
                                                    checkNotNull(cid);
                                                    Service.receiveReply(context, senderPid, cid);
                                                }
                                            } else {

                                                RTCSession.handleContent(context,
                                                        BuildConfig.ApiAesKey, senderPid, content);
                                            }
                                        } else {
                                            Preferences.error(threads, context.getString(
                                                    R.string.unsupported_pubsub_message,
                                                    senderPid.getPid()));
                                        }
                                    } else if (result.getCodex() == CodecDecider.Codec.UNKNOWN) {

                                        Preferences.error(threads, context.getString(
                                                R.string.unsupported_pubsub_message,
                                                senderPid.getPid()));
                                    }

                                }
                            } catch (Throwable e) {
                                Log.e(TAG, "" + e.getLocalizedMessage(), e);
                            } finally {
                                Log.e(TAG, "Receive : " + message.getMessage());
                            }
                        }

                        @NonNull
                        @Override
                        public String getTopic() {
                            return ipfs.getPid().getPid();
                        }
                    }, Preferences.isPubsubEnabled(context), false);

                } catch (Throwable e) {
                    Preferences.evaluateException(threads, Preferences.IPFS_START_FAILURE, e);
                }


                new java.lang.Thread(() -> checkTangleServer(context)).start();

                if (Preferences.isDebugMode(context)) {
                    ExecutorService executor = Executors.newSingleThreadExecutor();
                    executor.submit(() -> {
                        try {
                            ipfs.logs();
                        } catch (Throwable e) {
                            Preferences.evaluateException(threads, Preferences.EXCEPTION, e);
                        }
                    });
                }
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }


    public enum ThreadKind {
        LEAF, NODE
    }

}
