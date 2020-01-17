package threads.server;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.provider.DocumentsContract;
import android.util.Log;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.gson.Gson;
import com.j256.simplemagic.ContentInfo;

import java.io.File;
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
import threads.core.contents.CDS;
import threads.core.contents.ContentDatabase;
import threads.core.contents.Contents;
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
import threads.ipfs.CID;
import threads.ipfs.ConnMgrConfig;
import threads.ipfs.Encryption;
import threads.ipfs.IPFS;
import threads.ipfs.LinkInfo;
import threads.ipfs.Multihash;
import threads.ipfs.PID;
import threads.ipfs.PeerInfo;
import threads.ipfs.PubsubConfig;
import threads.ipfs.RoutingConfig;
import threads.server.jobs.JobServiceAutoConnect;
import threads.server.jobs.JobServiceCleanup;
import threads.server.jobs.JobServiceConnect;
import threads.server.jobs.JobServiceContents;
import threads.server.jobs.JobServiceDownload;
import threads.server.jobs.JobServiceDownloader;
import threads.server.jobs.JobServiceFindPeers;
import threads.server.jobs.JobServiceIdentity;
import threads.server.jobs.JobServiceLoadNotifications;
import threads.server.jobs.JobServiceLoadPublicKey;
import threads.server.jobs.JobServicePeers;
import threads.server.jobs.JobServicePublish;
import threads.server.jobs.JobServicePublisher;
import threads.share.ConnectService;
import threads.share.IdentityService;
import threads.share.MimeType;
import threads.share.ThumbnailService;

import static androidx.core.util.Preconditions.checkArgument;
import static androidx.core.util.Preconditions.checkNotNull;


public class Service {

    public static final int RELAYS = 5;
    static final String PIN_SERVICE_KEY = "pinServiceKey";
    private static final String TAG = Service.class.getSimpleName();
    private static final Gson gson = new Gson();
    private static final String APP_KEY = "AppKey";
    private static final String PIN_SERVICE_TIME_KEY = "pinServiceTimeKey";
    private static final String GATEWAY_KEY = "gatewayKey";
    private static final String AUTO_DOWNLOAD_KEY = "autoDownloadKey";
    private static final String UPDATE = "UPDATE";
    private static final String SEND_NOTIFICATIONS_ENABLED_KEY = "sendNotificationKey";
    private static final String RECEIVE_NOTIFICATIONS_ENABLED_KEY = "receiveNotificationKey";
    private static final String SUPPORT_PEER_DISCOVERY_KEY = "supportPeerDiscoveryKey";
    private static Service INSTANCE = null;

    private Service() {
    }

    public static boolean isSupportPeerDiscovery(@NonNull Context context) {
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
    public static String getGateway(@NonNull Context context) {
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

    public static int getPublishServiceTime(@NonNull Context context) {
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


    public static boolean isReceiveNotificationsEnabled(@NonNull Context context) {
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
    public static Service getInstance(@NonNull Context context) {
        checkNotNull(context);
        if (INSTANCE == null) {

            synchronized (Service.class) {

                if (INSTANCE == null) {
                    runUpdatesIfNecessary(context);

                    ProgressChannel.createProgressChannel(context);

                    long time = System.currentTimeMillis();

                    INSTANCE = new Service();
                    INSTANCE.startDaemon(context);
                    Log.e(TAG, "Time Daemon : " + (System.currentTimeMillis() - time));
                    INSTANCE.init(context);
                }
            }

        }
        return INSTANCE;
    }


    private static long getDaysAgo(int days) {
        return System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days);
    }

    public static void notifications(@NonNull Context context) {
        checkNotNull(context);
        final PID host = IPFS.getPID(context);
        if (host != null) {

            final EntityService entityService = EntityService.getInstance(context);
            final CDS contentService = CDS.getInstance(context);
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

                        final IPFS ipfs = IPFS.getInstance(context);
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
                                ipfs.connectPubsubTopic(context, pid.getPid());


                                threads.core.contents.Content content =
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

        final PEERS peers = PEERS.getInstance(context);
        final PID host = IPFS.getPID(context);
        final EVENTS events = EVENTS.getInstance(context);
        checkNotNull(host);
        final EntityService entityService = EntityService.getInstance(context);
        try {
            String address = AddressType.getAddress(
                    PID.create(pid), AddressType.NOTIFICATION);

            String publicKey = peers.getUserPublicKey(pid);
            if (publicKey.isEmpty()) {
                IPFS ipfs = IPFS.getInstance(context);
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
                Log.w(TAG,
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

    public static void cleanup(@NonNull Context context) {


        try {
            final CDS contentService = CDS.getInstance(context);
            final EntityService entityService = EntityService.getInstance(context);
            final IPFS ipfs = IPFS.getInstance(context);

            // remove all old hashes from hash database
            HashDatabase hashDatabase = entityService.getHashDatabase();
            long timestamp = getDaysAgo(28);
            hashDatabase.hashDao().removeAllHashesWithSmallerTimestamp(timestamp);


            // remove all content
            timestamp = getDaysAgo(14);
            ContentDatabase contentDatabase = contentService.getContentDatabase();
            List<threads.core.contents.Content> entries = contentDatabase.contentDao().
                    getContentWithSmallerTimestamp(timestamp);

            checkNotNull(ipfs, "IPFS not valid");

            try {
                for (threads.core.contents.Content content : entries) {

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

    public static void createUnknownUser(@NonNull Context context, @NonNull PID pid) throws Exception {
        checkNotNull(context);
        checkNotNull(pid);

        IPFS ipfs = IPFS.getInstance(context);
        PEERS peers = PEERS.getInstance(context);
        checkNotNull(ipfs, "IPFS not defined");


        if (peers.getUserByPID(pid) == null) {
            PeerInfo info = ipfs.id(pid, 3);
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

        final IPFS ipfs = IPFS.getInstance(context);
        final PEERS peers = PEERS.getInstance(context);

        final EVENTS events = EVENTS.getInstance(context);

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

                if (IPFS.isPubsubEnabled(context)) {
                    PID host = IPFS.getPID(context);
                    checkNotNull(host);

                    Content map = new Content();
                    map.put(Content.EST, "CONNECT");
                    map.put(Content.ALIAS, peers.getUserAlias(host));
                    map.put(Content.PKEY, peers.getUserPublicKey(host));

                    Log.w(TAG, "Send Pubsub Notification to PID :" + user);


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
        IPFS ipfs = IPFS.getInstance(context);
        PEERS peers = PEERS.getInstance(context);
        if (IPFS.isPubsubEnabled(context)) {
            PID host = IPFS.getPID(context);
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
        IPFS ipfs = IPFS.getInstance(context);
        if (IPFS.isPubsubEnabled(context)) {
            Content map = new Content();
            map.put(Content.EST, "SHARE");

            checkNotNull(ipfs, "IPFS not valid");
            ipfs.pubsubPub(topic, gson.toJson(map), 50);
        }
    }


    private static void runUpdatesIfNecessary(@NonNull Context context) {
        try {
            int versionCode = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), 0).versionCode;
            SharedPreferences prefs = context.getSharedPreferences(
                    APP_KEY, Context.MODE_PRIVATE);
            if (prefs.getInt(UPDATE, 0) != versionCode) {

                Preferences.setLoginFlag(context, false); // TODO remove later
                IPFS.deleteConfigFile(context); // TODO remove later


                // Experimental Features
                IPFS.setQUICEnabled(context, true);
                IPFS.setPreferTLS(context, true);


                IPFS.setSwarmPort(context, 4001);
                IPFS.setRoutingType(context, RoutingConfig.TypeEnum.dhtclient);


                IPFS.setAutoNATServiceEnabled(context, false);
                IPFS.setRelayHopEnabled(context, false);
                IPFS.setAutoRelayEnabled(context, true);

                IPFS.setPubsubEnabled(context, true);
                IPFS.setPubsubRouter(context, PubsubConfig.RouterEnum.gossipsub);

                IPFS.setConnMgrConfigType(context, ConnMgrConfig.TypeEnum.basic);
                IPFS.setLowWater(context, 50);
                IPFS.setHighWater(context, 200);
                IPFS.setGracePeriod(context, "10s");


                Preferences.setConnectionTimeout(context, 45);
                EntityService.setTangleTimeout(context, 45);

                IPFS.setMdnsEnabled(context, true);

                IPFS.setRandomSwarmPort(context, true);


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
            final PEERS peers = PEERS.getInstance(context);
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


        final THREADS threads = THREADS.getInstance(context);

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
            PEERS peers = PEERS.getInstance(context);
            IPFS ipfs = IPFS.getInstance(context);
            EVENTS events = EVENTS.getInstance(context);

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

            if (IPFS.isPubsubEnabled(context)) {
                PID host = IPFS.getPID(context);
                checkNotNull(host);
                User hostUser = peers.getUserByPID(host);
                checkNotNull(hostUser);
                Content map = new Content();
                map.put(Content.EST, "CONNECT_REPLY");
                map.put(Content.ALIAS, hostUser.getAlias());
                map.put(Content.PKEY, hostUser.getPublicKey());


                Log.w(TAG, "Send Pubsub Notification to PID :" + senderPid);

                ipfs.pubsubPub(senderPid.getPid(), gson.toJson(map), 50);
            }


        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    public static void replySender(@NonNull Context context,
                                   @NonNull IPFS ipfs,
                                   @NonNull PID sender,
                                   @NonNull Thread thread) {
        try {
            if (IPFS.isPubsubEnabled(context)) {
                CID cid = thread.getContent();
                checkNotNull(cid);

                Content map = new Content();
                map.put(Content.EST, "REPLY");
                map.put(Content.CID, cid.getCid());


                Log.w(TAG, "Send Pubsub Notification to PID :" + sender);

                ipfs.pubsubPub(sender.getPid(), gson.toJson(map), 50);
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }


    private static void cleanStates(@NonNull Context context) {
        checkNotNull(context);

        final THREADS threads = THREADS.getInstance(context);
        final PEERS peers = PEERS.getInstance(context);
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


    @NonNull
    private static String evaluateMimeType(@NonNull Context context, @NonNull String filename) {
        final EVENTS events = EVENTS.getInstance(context);

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


        final THREADS threads = THREADS.getInstance(context);
        final PEERS peers = PEERS.getInstance(context);
        User user = peers.getUserByPID(creator);
        checkNotNull(user);

        Thread thread = threads.createThread(user.getPID(), user.getAlias(),
                Status.INIT, Kind.OUT, parent);
        thread.setContent(cid);
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

    public static long createThread(@NonNull Context context,
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


        final THREADS threads = THREADS.getInstance(context);


        Thread thread = threads.createThread(creator.getPID(),
                creator.getAlias(), threadStatus, Kind.OUT, 0L);
        thread.setContent(cid);


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
        thread.setThumbnail(image);
        return threads.storeThread(thread);
    }

    private static boolean downloadThread(@NonNull Context context,
                                          @NonNull THREADS threads,
                                          @NonNull IPFS ipfs,
                                          @NonNull Thread thread) {

        checkNotNull(context);
        checkNotNull(threads);
        checkNotNull(ipfs);
        checkNotNull(thread);

        CID cid = thread.getContent();
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
            success = ipfs.loadToFile(file, cid,
                    (percent) -> {

                        builder.setProgress(100, percent, false);
                        if (notificationManager != null) {
                            notificationManager.notify(notifyID, builder.build());
                        }


                    }, timeout, size);

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
                    if (thread.getThumbnail() == null) {
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
                if (entry.getParent() == thread.getIdx()) {
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
                if (entry.getStatus() != Status.SEEDING) {

                    boolean success = downloadLink(context, threads, ipfs, entry, link);
                    if (success) {
                        successCounter.incrementAndGet();
                    }

                } else {
                    successCounter.incrementAndGet();
                }
            } else {

                long idx = createThread(context, ipfs,
                        thread.getSender(), cid, link, thread.getIdx());
                entry = threads.getThreadByIdx(idx);
                checkNotNull(entry);
                boolean success = downloadLink(context, threads, ipfs, entry, link);

                if (success) {
                    successCounter.incrementAndGet();
                    threads.setStatus(entry, Status.SEEDING);
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

    public static void downloadMultihash(@NonNull Context context,
                                         @NonNull THREADS threads,
                                         @NonNull IPFS ipfs,
                                         @NonNull Thread thread,
                                         @Nullable PID sender) {
        checkNotNull(context);
        checkNotNull(threads);
        checkNotNull(ipfs);
        checkNotNull(thread);

        final EVENTS events = EVENTS.getInstance(context);

        try {
            threads.setThreadLeaching(thread.getIdx(), true);

            CID cid = thread.getContent();
            checkNotNull(cid);

            List<LinkInfo> links = getLinks(context, ipfs, cid);

            if (links != null) {
                if (links.isEmpty()) {

                    boolean result = downloadThread(context, threads, ipfs, thread);
                    if (result) {
                        threads.setStatus(thread, Status.SEEDING);
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
                        threads.setStatus(thread, Status.SEEDING);
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
                IPFS ipfs = IPFS.getInstance(context);


                while (ipfs.isDaemonRunning()) {
                    java.lang.Thread.sleep(TimeUnit.SECONDS.toMillis(30));
                    JobServiceLoadNotifications.notifications(context);
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
        IPFS ipfs = IPFS.getInstance(context);

        try {
            while (ipfs.isDaemonRunning()) {
                checkPeers(context);
                java.lang.Thread.sleep(1000);
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }

    }

    private static void checkPeers(@NonNull Context context) {
        checkNotNull(context);

        try {

            final PEERS peers = PEERS.getInstance(context);

            final IPFS ipfs = IPFS.getInstance(context);

            List<PID> users = peers.getUsersPIDs();


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


        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }


    ArrayList<String> getEnhancedUserPIDs(@NonNull Context context) {
        checkNotNull(context);


        final PEERS peers = PEERS.getInstance(context);
        final PID pid = IPFS.getPID(context);
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

        THREADS threads = THREADS.getInstance(context);
        try {


            IPFS ipfs = IPFS.getInstance(context);

            threads.setThreadLeaching(thread.getIdx(), false);
            PID host = IPFS.getPID(context);
            checkNotNull(host);
            PID sender = thread.getSender();


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


    private void sharePeer(@NonNull Context context,
                           @NonNull User user,
                           @NonNull CID cid,
                           long start) {
        checkNotNull(context);
        checkNotNull(user);
        checkNotNull(cid);

        final EVENTS events = EVENTS.getInstance(context);
        final IPFS ipfs = IPFS.getInstance(context);

        JobServiceConnect.connect(context, user.getPID());

        try {
            boolean success = false;
            if (user.getType() == UserType.VERIFIED) {

                success = Service.notify(
                        context, user.getPID().getPid(), cid.getCid(), start);
            }
            // just backup
            if (IPFS.isPubsubEnabled(context)) {
                ipfs.connectPubsubTopic(context, user.getPID().getPid());
                if (!success) {

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


        final THREADS threads = THREADS.getInstance(context);
        final IPFS ipfs = IPFS.getInstance(context);
        final CDS contentService = CDS.getInstance(context);
        final PID host = IPFS.getPID(context);
        final EVENTS events = EVENTS.getInstance(context);


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

                    List<Thread> threadList = threads.getThreadsByIdx(idxs);
                    contents.add(threadList);

                    String data = gson.toJson(contents);

                    CID cid = ipfs.storeText(data);
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

    private void init(@NonNull Context context) {
        checkNotNull(context);

        JobServiceLoadNotifications.notifications(context);
        JobServiceDownloader.downloader(context);
        JobServicePublisher.publish(context);
        JobServicePeers.peers(context);
        JobServiceFindPeers.findPeers(context);
        JobServiceAutoConnect.autoConnect(context);
        JobServiceCleanup.cleanup(context);
        ContentsService.contents(context);

        new java.lang.Thread(() -> {
            try {
                Service.cleanStates(context);
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

            final PEERS peers = PEERS.getInstance(context);
            final EVENTS events = EVENTS.getInstance(context);

            try {

                final PID host = IPFS.getPID(context);
                IPFS.setPubsubHandler((message) -> {
                    try {

                        String sender = message.getSenderPid();

                        PID senderPid = PID.create(sender);


                        if (!senderPid.equals(host) && !peers.isUserBlocked(senderPid)) {

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
                Preferences.evaluateException(events, Preferences.EXCEPTION, e);
            }


        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }
}
