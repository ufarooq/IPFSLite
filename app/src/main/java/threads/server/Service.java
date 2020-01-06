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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import threads.core.Preferences;
import threads.core.Singleton;
import threads.core.events.EVENTS;
import threads.core.peers.AddressType;
import threads.core.peers.Content;
import threads.core.peers.PEERS;
import threads.core.peers.User;
import threads.core.peers.UserType;
import threads.core.threads.Kind;
import threads.core.threads.Status;
import threads.core.threads.THREADS;
import threads.core.threads.Thread;
import threads.iota.Entity;
import threads.iota.EntityService;
import threads.iota.HashDatabase;
import threads.ipfs.IPFS;
import threads.ipfs.api.CID;
import threads.ipfs.api.ConnMgrConfig;
import threads.ipfs.api.Encryption;
import threads.ipfs.api.LinkInfo;
import threads.ipfs.api.Multihash;
import threads.ipfs.api.PID;
import threads.ipfs.api.PeerInfo;
import threads.ipfs.api.PubsubConfig;
import threads.ipfs.api.RoutingConfig;
import threads.share.ConnectService;
import threads.share.IdentityService;
import threads.share.MimeType;
import threads.share.ThumbnailService;

import static androidx.core.util.Preconditions.checkArgument;
import static androidx.core.util.Preconditions.checkNotNull;


public class Service {

    static final int RELAYS = 5;
    static final String PIN_SERVICE_KEY = "pinServiceKey";
    private static final String TAG = Service.class.getSimpleName();
    private static final Gson gson = new Gson();
    private static final ExecutorService UPLOAD_SERVICE = Executors.newFixedThreadPool(10);
    private static final String APP_KEY = "AppKey";
    private static final String PIN_SERVICE_TIME_KEY = "pinServiceTimeKey";
    private static final String GATEWAY_KEY = "gatewayKey";
    private static final String AUTO_DOWNLOAD_KEY = "autoDownloadKey";
    private static final String UPDATE = "UPDATE";
    private static final String SEND_NOTIFICATIONS_ENABLED_KEY = "sendNotificationKey";
    private static final String RECEIVE_NOTIFICATIONS_ENABLED_KEY = "receiveNotificationKey";
    private static final String SUPPORT_PEER_DISCOVERY_KEY = "supportPeerDiscoveryKey";
    private static Service SINGLETON = null;

    private Service() {
    }

    static boolean isSupportPeerDiscovery(@NonNull Context context) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE);
        return sharedPref.getBoolean(SUPPORT_PEER_DISCOVERY_KEY, true);
    }

    static void setSupportPeerDiscovery(@NonNull Context context, boolean enable) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(SUPPORT_PEER_DISCOVERY_KEY, enable);
        editor.apply();
    }

    static boolean isAutoDownload(@NonNull Context context) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(
                APP_KEY, Context.MODE_PRIVATE);
        return sharedPref.getBoolean(AUTO_DOWNLOAD_KEY, true);
    }

    static void setAutoDownload(@NonNull Context context, boolean automaticDownload) {
        checkNotNull(context);

        SharedPreferences sharedPref = context.getSharedPreferences(
                APP_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(AUTO_DOWNLOAD_KEY, automaticDownload);
        editor.apply();

    }

    @NonNull
    static String getGateway(@NonNull Context context) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(
                APP_KEY, Context.MODE_PRIVATE);
        return sharedPref.getString(GATEWAY_KEY, "https://ipfs.io");
    }

    static void setGateway(@NonNull Context context, @NonNull String gateway) {
        checkNotNull(context);
        checkNotNull(gateway);
        SharedPreferences sharedPref = context.getSharedPreferences(
                APP_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(GATEWAY_KEY, gateway);
        editor.apply();

    }

    static int getPublishServiceTime(@NonNull Context context) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE);
        return sharedPref.getInt(PIN_SERVICE_TIME_KEY, 6);
    }

    static void setPublisherServiceTime(@NonNull Context context, int timeout) {
        checkNotNull(context);
        checkArgument(timeout >= 0);
        SharedPreferences sharedPref = context.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(PIN_SERVICE_TIME_KEY, timeout);
        editor.apply();
    }

    static boolean getDontShowAgain(@NonNull Context context, @NonNull String key) {
        checkNotNull(context);
        checkNotNull(key);
        SharedPreferences sharedPref = context.getSharedPreferences(
                APP_KEY, Context.MODE_PRIVATE);
        return sharedPref.getBoolean(key, false);
    }

    static void setDontShowAgain(@NonNull Context context, @NonNull String key, boolean value) {
        checkNotNull(context);
        checkNotNull(key);
        SharedPreferences sharedPref = context.getSharedPreferences(
                APP_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(key, value);
        editor.apply();

    }


    static boolean isSendNotificationsEnabled(@NonNull Context context) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE);
        return sharedPref.getBoolean(SEND_NOTIFICATIONS_ENABLED_KEY, true);
    }

    static void setSendNotificationsEnabled(@NonNull Context context, boolean enable) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(SEND_NOTIFICATIONS_ENABLED_KEY, enable);
        editor.apply();
    }


    static boolean isReceiveNotificationsEnabled(@NonNull Context context) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE);
        return sharedPref.getBoolean(RECEIVE_NOTIFICATIONS_ENABLED_KEY, true);
    }

    static void setReceiveNotificationsEnabled(@NonNull Context context, boolean enable) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(RECEIVE_NOTIFICATIONS_ENABLED_KEY, enable);
        editor.apply();
    }

    @NonNull
    public static synchronized Service getInstance(@NonNull Context context) {
        checkNotNull(context);
        if (SINGLETON == null) {

            runUpdatesIfNecessary(context);

            ProgressChannel.createProgressChannel(context);

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


    private static long getDaysAgo(int days) {
        return System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days);
    }

    static void notifications(@NonNull Context context) {
        checkNotNull(context);
        final PID host = Preferences.getPID(context);
        if (host != null) {

            final EntityService entityService = EntityService.getInstance(context);
            final ContentService contentService = ContentService.getInstance(context);
            try {
                String address = AddressType.getAddress(host, AddressType.NOTIFICATION);
                List<Entity> entities = entityService.loadEntities(context, address);

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

                        final Singleton singleton = Singleton.getInstance(context);
                        final IPFS ipfs = singleton.getIpfs();
                        checkNotNull(ipfs, "IPFS not valid");
                        if (data.containsKey(Content.PID) && data.containsKey(Content.CID)) {
                            try {
                                String privateKey = ipfs.getPrivateKey();
                                checkNotNull(privateKey, "Private Key not valid");
                                String encPid = data.get(Content.PID);
                                checkNotNull(encPid);
                                final String pidStr = Encryption.decryptRSA(encPid, privateKey);
                                checkNotNull(pidStr);

                                String encCid = data.get(Content.CID);
                                checkNotNull(encCid);
                                final String cidStr = Encryption.decryptRSA(encCid, privateKey);
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

                                // THIS is a try, it tries to find the pubsub of the PID
                                // (for sending a message when done)
                                singleton.connectPubsubTopic(context, pid.getPid());


                                threads.server.Content content =
                                        contentService.getContent(cid);
                                if (content == null) {
                                    contentService.insertContent(pid, cid, false);
                                }

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

    private static boolean notify(@NonNull Context context, @NonNull String pid,
                                  @NonNull String cid, long startTime) {

        checkNotNull(context);
        checkNotNull(pid);
        checkNotNull(cid);

        boolean success = true;

        final THREADS threads = Singleton.getInstance(context).getThreads();
        final PEERS peers = Singleton.getInstance(context).getPeers();
        final PID host = Preferences.getPID(context);
        final EVENTS events = Singleton.getInstance(context).getEvents();
        checkNotNull(host);
        final EntityService entityService = Singleton.getInstance(context).getEntityService();
        try {
            String address = AddressType.getAddress(
                    PID.create(pid), AddressType.NOTIFICATION);

            String publicKey = peers.getUserPublicKey(pid);
            if (publicKey.isEmpty()) {
                IPFS ipfs = Singleton.getInstance(context).getIpfs();
                checkNotNull(ipfs, "IPFS not valid");
                int timeout = Preferences.getConnectionTimeout(context);
                PeerInfo info = ipfs.id(PID.create(pid), timeout);
                if (info != null) {
                    String key = info.getPublicKey();
                    if (key != null) {
                        peers.setUserPublicKey(pid, key);
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


                String alias = peers.getUserAlias(pid);
                String json = gson.toJson(content);

                try {
                    entityService.insertData(context, address, json);
                    long time = (System.currentTimeMillis() - startTime) / 1000;

                    events.invokeEvent(Preferences.INFO,
                            context.getString(R.string.success_notification,
                                    alias, String.valueOf(time)));


                } catch (Throwable e) {
                    success = false;
                    Log.e(TAG, "" + e.getLocalizedMessage(), e);

                    events.invokeEvent(Preferences.EXCEPTION,
                            context.getString(R.string.failed_notification, alias));
                }

            }
        } catch (Throwable e) {
            success = false;
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
        return success;
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

            try {
                for (threads.server.Content content : entries) {

                    contentDatabase.contentDao().removeContent(content);

                    CID cid = content.getCID();
                    ipfs.rm(cid);
                }
            } finally {
                ipfs.gc();
            }

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }

    }

    static void createUnknownUser(@NonNull Context context, @NonNull PID pid) throws Exception {
        checkNotNull(context);
        checkNotNull(pid);

        IPFS ipfs = Singleton.getInstance(context).getIpfs();
        THREADS threads = Singleton.getInstance(context).getThreads();
        PEERS peers = Singleton.getInstance(context).getPeers();
        checkNotNull(ipfs, "IPFS not defined");


        if (peers.getUserByPID(pid) == null) {
            threads.ipfs.api.PeerInfo info = ipfs.id(pid, 3);
            if (info != null) {
                if (info.isLiteAgent()) {
                    String pubKey = info.getPublicKey();
                    if (pubKey != null && !pubKey.isEmpty()) {

                        threads.core.peers.PeerInfo peerInfo = IdentityService.getPeerInfo(
                                context, pid, false);
                        if (peerInfo != null) {
                            String alias = peerInfo.getAdditionalValue(Content.ALIAS);
                            if (!alias.isEmpty()) {
                                CID image = ThumbnailService.getImage(
                                        context,
                                        alias,
                                        R.drawable.server_network);

                                User user = peers.createUser(pid, pubKey, alias,
                                        UserType.UNKNOWN, image);
                                user.setBlocked(true);
                                peers.storeUser(user);
                            }
                        }
                    }
                }
            }
        }
    }

    static void connectPeer(@NonNull Context context, @NonNull PID user) throws Exception {
        checkNotNull(context);
        checkNotNull(user);

        final IPFS ipfs = Singleton.getInstance(context).getIpfs();
        final PEERS peers = Singleton.getInstance(context).getPeers();
        final THREADS threads = Singleton.getInstance(context).getThreads();
        final EVENTS events = Singleton.getInstance(context).getEvents();

        if (!peers.existsUser(user)) {

            String alias = user.getPid();

            checkNotNull(ipfs, "IPFS is not valid");
            CID image = ThumbnailService.getImage(
                    context, alias, R.drawable.server_network);

            User newUser = peers.createUser(user, "",
                    alias, UserType.VERIFIED, image);
            peers.storeUser(newUser);

        } else {
            Preferences.warning(events, context.getString(R.string.peer_exists_with_pid));
            return;
        }


        try {
            peers.setUserDialing(user, true);

            final int timeout = Preferences.getConnectionTimeout(context);
            final boolean peerDiscovery = Service.isSupportPeerDiscovery(context);
            boolean value = ConnectService.connectPeer(context, user,
                    peerDiscovery, true, timeout);
            peers.setUserConnected(user, value);

            if (value) {

                if (Preferences.isPubsubEnabled(context)) {
                    PID host = Preferences.getPID(context);
                    checkNotNull(host);

                    Content map = new Content();
                    map.put(Content.EST, "CONNECT");
                    map.put(Content.ALIAS, peers.getUserAlias(host));
                    map.put(Content.PKEY, peers.getUserPublicKey(host));

                    Singleton.getInstance(context).
                            getConsoleListener().info(
                            "Send Pubsub Notification to PID :" + user);


                    ipfs.pubsubPub(user.getPid(), gson.toJson(map), 50);
                }

                if (peers.getUserPublicKey(user).isEmpty()) {

                    JobServiceLoadPublicKey.publicKey(context, user.getPid());
                }

            }

            threads.core.peers.PeerInfo peerInfo = IdentityService.getPeerInfo(
                    context, user, true);
            if (peerInfo != null) {
                String alias = peerInfo.getAdditionalValue(Content.ALIAS);
                if (!alias.isEmpty()) {
                    peers.setUserAlias(user, alias);
                }
            }


        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
            peers.setUserConnected(user, false);
        } finally {
            peers.setUserDialing(user, false);
        }
    }

    static void sendReceiveMessage(@NonNull Context context, @NonNull String topic) {
        checkNotNull(context);
        checkNotNull(topic);
        Gson gson = new Gson();
        IPFS ipfs = Singleton.getInstance(context).getIpfs();
        PEERS peers = Singleton.getInstance(context).getPeers();
        if (Preferences.isPubsubEnabled(context)) {
            PID host = Preferences.getPID(context);
            checkNotNull(host);

            Content map = new Content();
            map.put(Content.EST, "RECEIVED");
            map.put(Content.ALIAS, peers.getUserAlias(host));

            checkNotNull(ipfs, "IPFS not valid");
            ipfs.pubsubPub(topic, gson.toJson(map), 50);
        }
    }

    private static void sendShareMessage(@NonNull Context context, @NonNull String topic) {
        checkNotNull(context);
        checkNotNull(topic);
        Gson gson = new Gson();
        IPFS ipfs = Singleton.getInstance(context).getIpfs();
        if (Preferences.isPubsubEnabled(context)) {
            Content map = new Content();
            map.put(Content.EST, "SHARE");

            checkNotNull(ipfs, "IPFS not valid");
            ipfs.pubsubPub(topic, gson.toJson(map), 50);
        }
    }


    private static void runUpdatesIfNecessary(@NonNull Context context) {
        try {
            int versionCode = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
            SharedPreferences prefs = context.getSharedPreferences(
                    APP_KEY, Context.MODE_PRIVATE);
            if (prefs.getInt(UPDATE, 0) != versionCode) {

                Preferences.setLoginFlag(context, false); // TODO remove later
                IPFS.deleteConfigFile(context); // TODO remove later


                // Experimental Features
                Preferences.setQUICEnabled(context, true);
                Preferences.setPreferTLS(context, true);


                Preferences.setSwarmPort(context, 4001);
                Preferences.setRoutingType(context, RoutingConfig.TypeEnum.dhtclient);


                Preferences.setAutoNATServiceEnabled(context, false);
                Preferences.setRelayHopEnabled(context, false);
                Preferences.setAutoRelayEnabled(context, true);

                Preferences.setPubsubEnabled(context, true);
                Preferences.setPubsubRouter(context, PubsubConfig.RouterEnum.gossipsub);

                Preferences.setConnMgrConfigType(context, ConnMgrConfig.TypeEnum.basic);
                Preferences.setLowWater(context, 50);
                Preferences.setHighWater(context, 200);
                Preferences.setGracePeriod(context, "10s");


                Preferences.setConnectionTimeout(context, 45);
                EntityService.setTangleTimeout(context, 45);

                Preferences.setMdnsEnabled(context, true);

                Preferences.setRandomSwarmPort(context, true);


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
            final PEERS peers = Singleton.getInstance(context).getPeers();
            User sender = peers.getUserByPID(senderPid);
            checkNotNull(sender);


            CID image = ThumbnailService.getImage(
                    context, alias, R.drawable.server_network);

            sender.setPublicKey(pubKey);
            sender.setAlias(alias);
            sender.setImage(image);
            sender.setType(UserType.VERIFIED);

            peers.storeUser(sender);


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
                    threads.incrementThreadNumber(thread);
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
                                   @NonNull String pubKey) {
        checkNotNull(context);
        checkNotNull(senderPid);
        checkNotNull(alias);
        checkNotNull(pubKey);

        try {
            final THREADS threads = Singleton.getInstance(context).getThreads();
            final PEERS peers = Singleton.getInstance(context).getPeers();
            final IPFS ipfs = Singleton.getInstance(context).getIpfs();
            final EVENTS events = Singleton.getInstance(context).getEvents();
            if (ipfs != null) {
                User sender = peers.getUserByPID(senderPid);
                if (sender == null) {

                    // create a new user which is blocked (User has to unblock and verified the user)
                    CID image = ThumbnailService.getImage(
                            context, alias, R.drawable.server_network);

                    sender = peers.createUser(senderPid, pubKey, alias, UserType.VERIFIED, image);
                    sender.setBlocked(true);
                    peers.storeUser(sender);

                    Preferences.error(events, context.getString(R.string.user_connect_try, alias));
                }

                if (Preferences.isPubsubEnabled(context)) {
                    PID host = Preferences.getPID(context);
                    checkNotNull(host);
                    User hostUser = peers.getUserByPID(host);
                    checkNotNull(hostUser);
                    Content map = new Content();
                    map.put(Content.EST, "CONNECT_REPLY");
                    map.put(Content.ALIAS, hostUser.getAlias());
                    map.put(Content.PKEY, hostUser.getPublicKey());


                    Singleton.getInstance(context).
                            getConsoleListener().info(
                            "Send Pubsub Notification to PID :" + senderPid);

                    ipfs.pubsubPub(senderPid.getPid(), gson.toJson(map), 50);
                }


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
        final PEERS peers = Singleton.getInstance(context).getPeers();
        try {
            threads.resetThreadsNumber();
            threads.resetThreadsPublishing();
            threads.resetThreadsLeaching();
            peers.resetUsersDialing();
            peers.resetPeersConnected();
            peers.resetUsersConnected();
            threads.setThreadStatus(Status.INIT, Status.ERROR);
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }

    }

    private static void createHost(@NonNull Context context) {
        checkNotNull(context);


        final PEERS peers = Singleton.getInstance(context).getPeers();
        final IPFS ipfs = Singleton.getInstance(context).getIpfs();
        final EVENTS events = Singleton.getInstance(context).getEvents();

        if (ipfs != null) {
            try {
                PID pid = Preferences.getPID(context);
                checkNotNull(pid);

                User user = peers.getUserByPID(pid);
                if (user == null) {
                    String publicKey = ipfs.getPublicKey();

                    CID image = ThumbnailService.getImage(
                            context, pid.getPid(), R.drawable.server_network);


                    user = peers.createUser(pid, publicKey, getDeviceName(),
                            UserType.VERIFIED, image);
                    user.setBlocked(true);
                    peers.storeUser(user);


                    JobServiceIdentity.identity(context);

                } else {
                    peers.blockUser(pid);
                }
            } catch (Throwable e) {
                Preferences.evaluateException(events, Preferences.EXCEPTION, e);
            }
        }
    }


    @NonNull
    private static String evaluateMimeType(@NonNull Context context, @NonNull String filename) {
        final THREADS threads = Singleton.getInstance(context).getThreads();
        final EVENTS events = Singleton.getInstance(context).getEvents();

        try {
            Optional<String> extension = ThumbnailService.getExtension(filename);
            if (extension.isPresent()) {
                String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.get());
                if (mimeType != null) {
                    return mimeType;
                }
            }
        } catch (Throwable e) {
            Preferences.evaluateException(events, Preferences.EXCEPTION, e);
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
        final PEERS peers = Singleton.getInstance(context).getPeers();
        User user = peers.getUserByPID(creator);
        checkNotNull(user);

        Thread thread = threads.createThread(user, Status.INIT, Kind.OUT,
                "", parent);
        thread.setCid(cid);
        String filename = link.getName();
        thread.setName(filename);

        long size = link.getSize();
        thread.setSize(size);

        if (link.isDirectory()) {
            thread.setMimeType(DocumentsContract.Document.MIME_TYPE_DIR);
        } else {
            thread.setMimeType(evaluateMimeType(context, filename));
        }

        return threads.storeThread(thread);
    }

    static long createThread(@NonNull Context context,
                             @NonNull IPFS ipfs,
                             @NonNull User creator,
                             @NonNull CID cid,
                             @NonNull Status threadStatus,
                             @Nullable String filename,
                             long filesize,
                             @Nullable String mimeType,
                             @Nullable CID image) {

        checkNotNull(context);
        checkNotNull(ipfs);
        checkNotNull(creator);
        checkNotNull(cid);
        checkNotNull(threadStatus);


        final THREADS threads = Singleton.getInstance(context).getThreads();


        Thread thread = threads.createThread(creator, threadStatus, Kind.OUT,
                "", 0L);
        thread.setCid(cid);


        if (filename != null) {
            thread.setName(filename);
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
            thread.setName(cid.getCid());
        }
        thread.setSize(filesize);
        thread.setImage(image);
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
            final EVENTS events = Singleton.getInstance(context).getEvents();


            if (ipfs != null) {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.submit(() -> {
                    try {
                        Thread threadObject = threadsAPI.getThreadByIdx(idx);
                        checkNotNull(threadObject);

                        CID cid = threadObject.getCid();
                        checkNotNull(cid);

                        int timeout = Preferences.getConnectionTimeout(context);

                        String name = threadObject.getName();
                        long size = -1;
                        try {
                            size = threadObject.getSize();
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
                            boolean finished = ipfs.storeToFile(file, cid,
                                    (percent) -> {

                                        builder.setProgress(100, percent, false);
                                        if (notificationManager != null) {
                                            notificationManager.notify(notifyID, builder.build());
                                        }


                                    }, false, timeout, size);

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
                            Preferences.evaluateException(events, Preferences.EXCEPTION, e);
                        } finally {

                            if (notificationManager != null) {
                                notificationManager.cancel(notifyID);
                            }
                        }

                    } catch (Throwable e) {
                        Preferences.evaluateException(events, Preferences.EXCEPTION, e);
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

        String filename = thread.getName();
        long filesize = thread.getSize();

        return download(context, threads, ipfs, thread, cid, filename, filesize);
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
            threads.setThreadLeaching(thread.getIdx(), true);
            int timeout = Preferences.getConnectionTimeout(context);
            File file = ipfs.getTempCacheFile();
            success = ipfs.storeToFile(file, cid,
                    (percent) -> {

                        builder.setProgress(100, percent, false);
                        if (notificationManager != null) {
                            notificationManager.notify(notifyID, builder.build());
                        }


                    }, false, timeout, size);

            if (success) {
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

                // check if image was not imported
                try {
                    if (thread.getImage() == null) {
                        ThumbnailService.Result res = ThumbnailService.getThumbnail(
                                context, file, filename);
                        CID image = res.getCid();
                        if (image != null) {
                            threads.setImage(thread, image);
                        }
                    }
                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
                }
            }
            if (file.exists()) {
                checkArgument(file.delete());
            }
        } catch (Throwable e) {
            success = false;
        } finally {
            threads.setThreadLeaching(thread.getIdx(), false);
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
                if (entry.getStatus() != Status.DONE) {

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
                    threads.setStatus(entry, Status.DONE);
                } else {
                    threads.setStatus(entry, Status.ERROR);
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

        final EVENTS events = Singleton.getInstance(context).getEvents();

        try {
            threads.setThreadLeaching(thread.getIdx(), true);

            CID cid = thread.getCid();
            checkNotNull(cid);

            List<LinkInfo> links = getLinks(context, ipfs, cid);

            if (links != null) {
                if (links.isEmpty()) {

                    boolean result = downloadThread(context, threads, ipfs, thread);
                    if (result) {
                        threads.setStatus(thread, Status.DONE);
                        if (sender != null) {
                            replySender(context, ipfs, sender, thread);
                        }
                    } else {
                        threads.setStatus(thread, Status.ERROR);
                    }

                } else {

                    // thread is directory

                    if (!thread.getMimeType().equals(DocumentsContract.Document.MIME_TYPE_DIR)) {
                        threads.setMimeType(thread, DocumentsContract.Document.MIME_TYPE_DIR);

                        try {
                            CID image = ThumbnailService.createResourceImage(context, ipfs,
                                    R.drawable.folder_outline);
                            checkNotNull(image);
                            threads.setImage(thread, image);
                        } catch (Throwable e) {
                            Preferences.evaluateException(events, Preferences.EXCEPTION, e);
                        }
                    }


                    boolean result = downloadLinks(context, threads, ipfs, thread, links);
                    if (result) {
                        threads.setStatus(thread, Status.DONE);
                        if (sender != null) {
                            replySender(context, ipfs, sender, thread);
                        }
                    } else {
                        threads.setStatus(thread, Status.ERROR);
                    }
                }
            } else {
                threads.setStatus(thread, Status.ERROR);
            }
        } finally {
            threads.setThreadLeaching(thread.getIdx(), false);
        }
    }

    private static void checkNotifications(@NonNull Context context) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                final IPFS ipfs = Singleton.getInstance(context).getIpfs();
                if (ipfs != null) {

                    while (ipfs.isDaemonRunning()) {
                        java.lang.Thread.sleep(TimeUnit.SECONDS.toMillis(30));
                        JobServiceLoadNotifications.notifications(context);
                    }
                }
            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }
        });
    }

    private static void peersOnlineStatus(@NonNull Context context) {
        checkNotNull(context);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                checkPeersOnlineStatus(context);
            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }
        });
    }

    private static void checkPeersOnlineStatus(@NonNull Context context) {
        checkNotNull(context);
        final IPFS ipfs = Singleton.getInstance(context).getIpfs();
        if (ipfs != null) {
            try {
                while (ipfs.isDaemonRunning()) {
                    checkPeers(context);
                    java.lang.Thread.sleep(1000);
                }
            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }
        }
    }

    private static void checkPeers(@NonNull Context context) {
        checkNotNull(context);

        try {
            final PID host = Preferences.getPID(context);
            final THREADS threads = Singleton.getInstance(context).getThreads();
            final PEERS peers = Singleton.getInstance(context).getPeers();
            final EVENTS events = Singleton.getInstance(context).getEvents();

            final IPFS ipfs = Singleton.getInstance(context).getIpfs();
            if (ipfs != null) {

                List<PID> users = peers.getUsersPIDs();

                users.remove(host);

                for (PID user : users) {
                    if (!peers.isUserBlocked(user) && !peers.getUserDialing(user)) {

                        try {
                            boolean value = ipfs.isConnected(user);

                            boolean preValue = peers.isUserConnected(user);

                            if (preValue != value) {
                                peers.setUserConnected(user, value);
                            }

                        } catch (Throwable e) {
                            Log.e(TAG, "" + e.getLocalizedMessage(), e);
                            peers.setUserConnected(user, false);
                        }
                    }
                }
            }

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    void storeData(@NonNull Context context, @NonNull String text) {
        checkNotNull(context);
        checkNotNull(text);

        final THREADS threads = Singleton.getInstance(context).getThreads();

        final EVENTS events = Singleton.getInstance(context).getEvents();

        final IPFS ipfs = Singleton.getInstance(context).getIpfs();
        final PEERS peers = Singleton.getInstance(context).getPeers();
        if (ipfs != null) {

            UPLOAD_SERVICE.submit(() -> {
                try {

                    PID pid = Preferences.getPID(context);
                    checkNotNull(pid);
                    User host = peers.getUserByPID(pid);
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

                    Thread thread = threads.createThread(host, Status.INIT, Kind.IN,
                            "", 0L);
                    thread.setName(content);
                    thread.setSize(size);
                    thread.setMimeType(mimeType);

                    long idx = threads.storeThread(thread);


                    try {
                        threads.setThreadLeaching(idx, true);

                        CID cid = ipfs.storeText(text, "", true);
                        checkNotNull(cid);


                        // cleanup of entries with same CID and hierarchy
                        List<Thread> sameEntries = threads.getThreadsByCIDAndThread(cid, 0L);
                        threads.removeThreads(ipfs, sameEntries);


                        threads.setThreadCID(idx, cid);
                        threads.setThreadStatus(idx, Status.DONE);
                    } catch (Throwable e) {
                        threads.setThreadStatus(idx, Status.ERROR);
                    } finally {
                        threads.setThreadLeaching(idx, false);
                    }

                } catch (Throwable e) {
                    Preferences.evaluateException(events, Preferences.EXCEPTION, e);
                }
            });
        }
    }

    void storeData(@NonNull Context context, @NonNull Uri uri) {
        checkNotNull(context);
        checkNotNull(uri);
        final EVENTS events = Singleton.getInstance(context).getEvents();

        final THREADS threads = Singleton.getInstance(context).getThreads();
        final PEERS peers = Singleton.getInstance(context).getPeers();
        ThumbnailService.FileDetails fileDetails = ThumbnailService.getFileDetails(context, uri);
        if (fileDetails == null) {
            Preferences.error(events, context.getString(R.string.file_not_supported));
            return;
        }

        final IPFS ipfs = Singleton.getInstance(context).getIpfs();


        UPLOAD_SERVICE.submit(() -> {
            try {
                checkNotNull(ipfs, "IPFS is not valid");
                InputStream inputStream =
                        context.getContentResolver().openInputStream(uri);
                checkNotNull(inputStream);

                PID pid = Preferences.getPID(context);
                checkNotNull(pid);
                User host = peers.getUserByPID(pid);
                checkNotNull(host);


                String name = fileDetails.getFileName();
                long size = fileDetails.getFileSize();

                Thread thread = threads.createThread(host, Status.INIT, Kind.IN,
                        "", 0L);

                ThumbnailService.Result res =
                        ThumbnailService.getThumbnail(context, uri, host.getAlias());

                thread.setName(name);
                thread.setSize(size);
                thread.setImage(res.getCid());
                thread.setMimeType(fileDetails.getMimeType());
                long idx = threads.storeThread(thread);


                try {
                    threads.setThreadLeaching(idx, true);

                    CID cid = ipfs.storeStream(inputStream, true);
                    checkNotNull(cid);

                    // cleanup of entries with same CID and hierarchy
                    List<Thread> sameEntries = threads.getThreadsByCIDAndThread(cid, 0L);
                    threads.removeThreads(ipfs, sameEntries);


                    threads.setThreadCID(idx, cid);
                    threads.setThreadStatus(idx, Status.DONE);
                } catch (Throwable e) {
                    threads.setThreadStatus(idx, Status.ERROR);
                    throw e;
                } finally {
                    threads.setThreadLeaching(idx, false);
                }

            } catch (FileNotFoundException e) {
                Preferences.error(events, context.getString(R.string.file_not_found));
            } catch (Throwable e) {
                Preferences.evaluateException(events, Preferences.EXCEPTION, e);
            }
        });
    }


    ArrayList<String> getEnhancedUserPIDs(@NonNull Context context) {
        checkNotNull(context);

        final THREADS threads = Singleton.getInstance(context).getThreads();
        final PEERS peers = Singleton.getInstance(context).getPeers();
        final PID pid = Preferences.getPID(context);
        ArrayList<String> users = new ArrayList<>();
        checkNotNull(pid);

        for (User user : peers.getUsers()) {
            if (!user.getPID().equals(pid)) {
                if (!peers.isUserBlocked(user.getPID())) {
                    users.add(user.getPID().getPid());
                }
            }
        }
        return users;
    }

    void downloadThread(@NonNull Context context, @NonNull Thread thread) {

        checkNotNull(context);
        checkNotNull(thread);

        final THREADS threads = Singleton.getInstance(context).getThreads();

        final IPFS ipfs = Singleton.getInstance(context).getIpfs();
        if (ipfs != null) {
            try {
                threads.setThreadLeaching(thread.getIdx(), false);
                PID host = Preferences.getPID(context);
                checkNotNull(host);
                PID sender = thread.getSenderPid();


                if (!host.equals(sender)) {

                    SwarmService.ConnectInfo info = SwarmService.connect(context, sender);

                    Service.downloadMultihash(context, threads, ipfs, thread, sender);

                    SwarmService.disconnect(context, info);

                } else {

                    Service.downloadMultihash(context, threads, ipfs, thread, null);
                }
            } finally {
                threads.setThreadLeaching(thread.getIdx(), false);
            }
        }

    }


    private void sharePeer(@NonNull Context context,
                           @NonNull User user,
                           @NonNull CID cid,
                           long start) {
        checkNotNull(context);
        checkNotNull(user);
        checkNotNull(cid);

        final Singleton singleton = Singleton.getInstance(context);
        final THREADS threads = singleton.getThreads();
        final EVENTS events = Singleton.getInstance(context).getEvents();


        JobServiceConnect.connect(context, user.getPID());

        try {
            boolean success = false;
            if (user.getType() == UserType.VERIFIED) {

                success = Service.notify(
                        context, user.getPID().getPid(), cid.getCid(), start);
            }
            // just backup
            if (Preferences.isPubsubEnabled(context)) {
                singleton.connectPubsubTopic(context, user.getPID().getPid());
                if (!success) {
                    final IPFS ipfs = singleton.getIpfs();
                    checkNotNull(ipfs, "IPFS not valid");
                    ipfs.pubsubPub(user.getPID().getPid(), cid.getCid(), 50);
                } else {
                    sendShareMessage(context, user.getPID().getPid());
                }
            }
        } catch (Throwable e) {
            Preferences.evaluateException(events, Preferences.EXCEPTION, e);
        }


    }


    void sendThreads(@NonNull Context context, @NonNull List<User> users, long[] idxs) {
        checkNotNull(context);
        checkNotNull(users);
        checkNotNull(idxs);


        final THREADS threads = Singleton.getInstance(context).getThreads();
        final IPFS ipfs = Singleton.getInstance(context).getIpfs();
        final ContentService contentService = ContentService.getInstance(context);
        final PID host = Preferences.getPID(context);
        final EVENTS events = Singleton.getInstance(context).getEvents();

        if (ipfs != null) {

            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    // clean-up (unset the unread number)
                    threads.resetThreadsNumber(idxs);

                    if (users.isEmpty()) {
                        Preferences.error(events, context.getString(R.string.no_sharing_peers));
                    } else {
                        checkNotNull(host);

                        threads.setThreadsPublishing(true, idxs);


                        long start = System.currentTimeMillis();

                        Contents contents = new Contents();

                        List<Thread> threadList = threads.getThreadByIdxs(idxs);
                        contents.add(threadList);

                        String data = gson.toJson(contents);

                        CID cid = ipfs.storeText(data, "", true);
                        checkNotNull(cid);

                        checkNotNull(host);
                        contentService.insertContent(host, cid, true);

                        JobServicePublish.publish(context, cid, false);

                        JobServiceIdentity.identity(context);


                        ExecutorService executorService = Executors.newSingleThreadExecutor();
                        List<Future> futures = new ArrayList<>();

                        for (User user : users) {
                            futures.add(executorService.submit(() ->
                                    sharePeer(context, user, cid, start)));
                        }


                        for (Future future : futures) {
                            future.get();
                        }

                        threads.setThreadsPublishing(false, idxs);
                    }

                } catch (Throwable e) {
                    Preferences.evaluateException(events, Preferences.EXCEPTION, e);
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
                Service.checkNotifications(context);
                Service.peersOnlineStatus(context);
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
            final PEERS peers = Singleton.getInstance(context).getPeers();
            final EVENTS events = Singleton.getInstance(context).getEvents();

            if (ipfs != null) {

                try {
                    Singleton singleton = Singleton.getInstance(context);
                    singleton.getConsoleListener().debug("Start Daemon");

                    boolean pubSubEnabled = Preferences.isPubsubEnabled(context);
                    ipfs.daemon(pubSubEnabled);

                    singleton.setPubsubHandler((message) -> {
                        try {

                            String sender = message.getSenderPid();
                            String topic = message.getTopic();
                            PID senderPid = PID.create(sender);


                            if (!peers.isUserBlocked(senderPid)) {

                                String code = message.getMessage().trim();

                                CodecDecider result = CodecDecider.evaluate(code);

                                if (result.getCodex() == CodecDecider.Codec.MULTIHASH) {
                                    JobServiceContents.contents(context,
                                            senderPid, CID.create(result.getMultihash()));
                                } else if (result.getCodex() == CodecDecider.Codec.URI) {
                                    JobServiceDownload.download(context,
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
                                                createUser(context, senderPid, alias, pubKey);
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
                                        } else if ("RECEIVED".equals(est)) {
                                            if (content.containsKey(Content.ALIAS)) {
                                                String alias = content.get(Content.ALIAS);
                                                checkNotNull(alias);

                                                Preferences.error(events, context.getString(
                                                        R.string.notification_received, alias));
                                            }
                                        } else if ("SHARE".equals(est)) {
                                            ScheduledExecutorService executorService =
                                                    Executors.newSingleThreadScheduledExecutor();
                                            executorService.schedule(() ->
                                                            Service.notifications(context),
                                                    3, TimeUnit.SECONDS);
                                        }
                                    } else {
                                        Preferences.error(events, context.getString(
                                                R.string.unsupported_pubsub_message,
                                                senderPid.getPid()));
                                    }
                                } else if (result.getCodex() == CodecDecider.Codec.UNKNOWN) {

                                    Preferences.error(events, context.getString(
                                            R.string.unsupported_pubsub_message,
                                            senderPid.getPid()));
                                }

                            }
                        } catch (Throwable e) {
                            Log.e(TAG, "" + e.getLocalizedMessage(), e);
                        } finally {
                            Log.e(TAG, "Receive : " + message.getMessage());
                        }


                    });

                } catch (Throwable e) {
                    Preferences.evaluateException(events, Preferences.IPFS_START_FAILURE, e);
                }

            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }
}
