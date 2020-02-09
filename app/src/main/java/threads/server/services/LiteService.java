package threads.server.services;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.util.Log;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;

import com.google.gson.Gson;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import threads.iota.EntityService;
import threads.ipfs.CID;
import threads.ipfs.Encryption;
import threads.ipfs.IPFS;
import threads.ipfs.PID;
import threads.ipfs.PeerInfo;
import threads.server.R;
import threads.server.core.contents.CDS;
import threads.server.core.contents.Contents;
import threads.server.core.events.EVENTS;
import threads.server.core.peers.AddressType;
import threads.server.core.peers.Content;
import threads.server.core.peers.PEERS;
import threads.server.core.peers.User;
import threads.server.core.threads.THREADS;
import threads.server.core.threads.Thread;
import threads.server.jobs.JobServiceCleanup;
import threads.server.jobs.JobServiceConnect;
import threads.server.jobs.JobServiceContents;
import threads.server.jobs.JobServiceDownload;
import threads.server.jobs.JobServiceFindPeers;
import threads.server.jobs.JobServiceIdentity;
import threads.server.jobs.JobServiceLoadPublicKey;
import threads.server.jobs.JobServicePeers;
import threads.server.jobs.JobServicePublish;
import threads.server.jobs.JobServicePublisher;
import threads.server.utils.CodecDecider;
import threads.server.utils.Preferences;
import threads.server.work.BootstrapWorker;
import threads.server.work.DownloadContentsWorker;
import threads.server.work.LoadNotificationsWorker;

import static androidx.core.util.Preconditions.checkArgument;
import static androidx.core.util.Preconditions.checkNotNull;


public class LiteService {

    public static final int RELAYS = 5;
    public static final String PIN_SERVICE_KEY = "pinServiceKey";
    private static final String TAG = LiteService.class.getSimpleName();
    private static final Gson gson = new Gson();
    private static final String APP_KEY = "AppKey";
    private static final String PIN_SERVICE_TIME_KEY = "pinServiceTimeKey";
    private static final String GATEWAY_KEY = "gatewayKey";
    private static final String AUTO_DOWNLOAD_KEY = "autoDownloadKey";
    private static final String SEND_NOTIFICATIONS_ENABLED_KEY = "sendNotificationKey";
    private static final String RECEIVE_NOTIFICATIONS_ENABLED_KEY = "receiveNotificationKey";
    private static final String SUPPORT_PEER_DISCOVERY_KEY = "supportPeerDiscoveryKey";
    private static LiteService INSTANCE = null;

    private LiteService() {
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

    public static int getPublishServiceTime(@NonNull Context context) {
        checkNotNull(context);
        SharedPreferences sharedPref = context.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE);
        return sharedPref.getInt(PIN_SERVICE_TIME_KEY, 6);
    }

    public static void setPublisherServiceTime(@NonNull Context context, int timeout) {
        checkNotNull(context);
        checkArgument(timeout >= 0);
        SharedPreferences sharedPref = context.getSharedPreferences(APP_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(PIN_SERVICE_TIME_KEY, timeout);
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
    public static LiteService getInstance(@NonNull Context context) {
        checkNotNull(context);
        if (INSTANCE == null) {

            synchronized (LiteService.class) {

                if (INSTANCE == null) {
                    INSTANCE = new LiteService();
                    INSTANCE.attachHandler(context);
                    INSTANCE.init(context);
                }
            }

        }
        return INSTANCE;
    }


    private static boolean notify(@NonNull Context context,
                                  @NonNull String pid,
                                  @NonNull String cid,
                                  long startTime) {

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
                if (alias == null) {
                    alias = pid;
                }
                String json = gson.toJson(content);

                try {
                    entityService.insertData(context, address, json);
                    long time = (System.currentTimeMillis() - startTime) / 1000;

                    events.invokeEvent(EVENTS.INFO,
                            context.getString(R.string.success_notification,
                                    alias, String.valueOf(time)));


                } catch (Throwable e) {
                    success = false;
                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
                }

            }
        } catch (Throwable e) {
            success = false;
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
        return success;
    }


    public static void createUnknownUser(@NonNull Context context, @NonNull PID pid) {
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

                        threads.server.core.peers.PeerInfo peerInfo = IdentityService.getPeerInfo(
                                context, pid, false);
                        if (peerInfo != null) {
                            String alias = peerInfo.getAdditionalValue(Content.ALIAS);
                            if (!alias.isEmpty()) {
                                User user = peers.createUser(pid, pubKey, alias);
                                user.setBlocked(true);
                                peers.storeUser(user);
                            }
                        }
                    }
                }
            }
        }
    }

    // todo check if optimize
    public static void connectPeer(@NonNull Context context, @NonNull PID user, boolean addMessage) {
        checkNotNull(context);
        checkNotNull(user);

        final IPFS ipfs = IPFS.getInstance(context);
        final PEERS peers = PEERS.getInstance(context);

        final EVENTS events = EVENTS.getInstance(context);

        if (!peers.existsUser(user)) {

            String alias = user.getPid();

            checkNotNull(ipfs, "IPFS is not valid");
            // todo public key
            User newUser = peers.createUser(user, "", alias);
            peers.storeUser(newUser);

            if (addMessage) {
                events.warning(context.getString(R.string.added_pid_to_peers, user));
            }

        } else {
            events.invokeEvent(EVENTS.WARNING,
                    context.getString(R.string.peer_exists_with_pid));

            return;
        }


        try {
            peers.setUserDialing(user.getPid(), true);

            SwarmService.ConnectInfo info = SwarmService.connect(context, user);
            peers.setUserConnected(user, info.isConnected());

            if (info.isConnected()) {

                if (IPFS.isPubSubEnabled(context)) {

                    Content map = new Content();
                    map.put(Content.EST, "CONNECT");
                    map.put(Content.ALIAS, IPFS.getDeviceName());
                    map.put(Content.PKEY, ipfs.getPublicKey());

                    Log.w(TAG, "Send Pubsub Notification to PID :" + user);


                    ipfs.pubSubPub(user.getPid(), gson.toJson(map), 50);
                }

                if (peers.getUserPublicKey(user).isEmpty()) {

                    JobServiceLoadPublicKey.publicKey(context, user.getPid());
                }

            }

            threads.server.core.peers.PeerInfo peerInfo = IdentityService.getPeerInfo(
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
            peers.setUserDialing(user.getPid(), false);
        }
    }


    private static void sendShareMessage(@NonNull Context context,
                                         @NonNull String topic,
                                         @NonNull CID cid) {
        checkNotNull(context);
        checkNotNull(topic);
        checkNotNull(cid);
        Gson gson = new Gson();
        IPFS ipfs = IPFS.getInstance(context);
        if (IPFS.isPubSubEnabled(context)) {
            Content map = new Content();
            map.put(Content.EST, "SHARE");
            map.put(Content.CID, cid.getCid());
            checkNotNull(ipfs, "IPFS not valid");
            ipfs.pubSubPub(topic, gson.toJson(map), 50);
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


            sender.setPublicKey(pubKey);
            sender.setAlias(alias);

            peers.storeUser(sender);


        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }


    public static String getAddressLink(@NonNull String address) {
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

                sender = peers.createUser(senderPid, pubKey, alias);
                sender.setBlocked(true);

                peers.storeUser(sender);

                events.error(context.getString(R.string.user_connect_try, alias));
            }

            if (IPFS.isPubSubEnabled(context)) {
                PID host = IPFS.getPID(context);
                checkNotNull(host);
                User hostUser = peers.getUserByPID(host);
                checkNotNull(hostUser);
                Content map = new Content();
                map.put(Content.EST, "CONNECT_REPLY");
                map.put(Content.ALIAS, hostUser.getAlias());
                map.put(Content.PKEY, hostUser.getPublicKey());


                Log.w(TAG, "Send Pubsub Notification to PID :" + senderPid);

                ipfs.pubSubPub(senderPid.getPid(), gson.toJson(map), 50);
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

            threads.resetThreadsPublishing();
            threads.resetThreadsLeaching();
            peers.resetUsersDialing();
            peers.resetPeersConnected();
            peers.resetUsersConnected();

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }

    }


    @Nullable
    private static String evaluateMimeType(@NonNull String filename) {
        try {
            Optional<String> extension = ThumbnailService.getExtension(filename);
            if (extension.isPresent()) {
                String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.get());
                if (mimeType != null) {
                    return mimeType;
                }
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
        return null;
    }

    public static long createThread(@NonNull Context context,
                                    @NonNull CID cid,
                                    @Nullable String filename,
                                    long fileSize,
                                    @Nullable String mimeType) {

        checkNotNull(context);
        checkNotNull(cid);


        final THREADS threads = THREADS.getInstance(context);

        Thread thread = threads.createThread(0L);
        thread.setContent(cid);


        if (filename != null) {
            thread.setName(filename);
            if (mimeType != null) {
                thread.setMimeType(mimeType);
            } else {
                String evalMimeType = evaluateMimeType(filename);
                if (evalMimeType != null) {
                    thread.setMimeType(evalMimeType);
                }

            }
        } else {
            if (mimeType != null) {
                thread.setMimeType(mimeType);
            }
            thread.setName(cid.getCid());
        }
        thread.setSize(fileSize);

        return threads.storeThread(thread);
    }


    public static boolean isNightNode(@NonNull Context context) {
        int nightModeFlags =
                context.getResources().getConfiguration().uiMode &
                        Configuration.UI_MODE_NIGHT_MASK;
        switch (nightModeFlags) {
            case Configuration.UI_MODE_NIGHT_YES:
                return true;
            case Configuration.UI_MODE_NIGHT_UNDEFINED:
            case Configuration.UI_MODE_NIGHT_NO:
                return false;
        }
        return false;
    }

    @NonNull
    public static String loadRawData(@NonNull Context context, @RawRes int id) {
        checkNotNull(context);

        try (InputStream inputStream = context.getResources().openRawResource(id)) {
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

                IOUtils.copy(inputStream, outputStream);
                return new String(outputStream.toByteArray());

            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
            return "";
        }
    }

    public static void deleteUser(@NonNull Context context, @NonNull String pid) {
        checkNotNull(context);
        checkNotNull(pid);
        try {
            final PEERS peers = PEERS.getInstance(context);

            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    // todo make faster
                    User user = peers.getUserByPID(PID.create(pid));
                    if (user != null) {
                        peers.removeUser(user);
                    }

                } catch (Throwable e) {
                    EVENTS.getInstance(context).exception(e);
                }
            });
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    public static String getDetailsReport(@NonNull Context context, @NonNull PID peer) {
        String protocolVersion = "n.a.";
        String agentVersion = "n.a.";
        List<String> addresses = new ArrayList<>();
        final int timeout = Preferences.getConnectionTimeout(context);
        final IPFS ipfs = IPFS.getInstance(context);

        PeerInfo info = ipfs.id(peer, timeout);

        if (info != null) {
            agentVersion = info.getAgentVersion();
            protocolVersion = info.getProtocolVersion();
            addresses = info.getMultiAddresses();
        }

        String html = "<html><body style=\"background-color:snow;\"><h3 style=\"text-align:center; color:teal;\">Peer Details</h3>";
        if (LiteService.isNightNode(context)) {
            html = "<html><head><style>body { background-color: DarkSlateGray; color: white;}</style></head><body><h3 style=\"text-align:center; color:teal;\">Peer Details</h3>";
        }

        html = html.concat("<div style=\"width: 80%;" +
                "  word-wrap:break-word;\">").concat(peer.getPid()).concat("</div><br/>");

        html = html.concat("<div style=\"width: 80%;" +
                "  word-wrap:break-word;\">").concat("Protocol Version : ").concat(protocolVersion).concat("</div><br/>");

        html = html.concat("<ul>");

        for (String address : addresses) {
            html = html.concat("<li><div style=\"width: 80%;" +
                    "  word-wrap:break-word;\">").concat(address).concat("</div></li>");
        }


        return html.concat("</ul><br/></body><footer>Agent : <strong style=\"color:teal;\">" + agentVersion + "</strong></footer></html>");

    }

    public ArrayList<String> getEnhancedUserPIDs(@NonNull Context context) {
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

    private void sharePeer(@NonNull Context context,
                           @NonNull User user,
                           @NonNull CID cid,
                           long start) {
        checkNotNull(context);
        checkNotNull(user);
        checkNotNull(cid);

        final IPFS ipfs = IPFS.getInstance(context);

        BootstrapWorker.bootstrap(context);
        JobServiceConnect.connect(context, user.getPID());

        try {
            boolean success = false;
            if (!user.isBlocked()) {

                success = LiteService.notify(
                        context, user.getPID().getPid(), cid.getCid(), start);
            }
            // just backup
            if (IPFS.isPubSubEnabled(context)) {
                ipfs.addPubSubTopic(context, user.getPID().getPid());
                if (!success) {

                    checkNotNull(ipfs, "IPFS not valid");
                    ipfs.pubSubPub(user.getPID().getPid(), cid.getCid(), 50);
                } else {
                    sendShareMessage(context, user.getPID().getPid(), cid);
                }
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }


    }

    public void sendThreads(@NonNull Context context, @NonNull List<User> users, long[] indices) {
        checkNotNull(context);
        checkNotNull(users);
        checkNotNull(indices);


        final THREADS threads = THREADS.getInstance(context);
        final IPFS ipfs = IPFS.getInstance(context);
        final CDS contentService = CDS.getInstance(context);
        final PID host = IPFS.getPID(context);
        final EVENTS events = EVENTS.getInstance(context);


        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                checkNotNull(ipfs, "IPFS not valid");

                if (users.isEmpty()) {
                    events.error(context.getString(R.string.no_sharing_peers));
                } else {
                    checkNotNull(host);

                    threads.setThreadsPublishing(true, indices);


                    long start = System.currentTimeMillis();

                    Contents contents = new Contents();

                    List<Thread> threadList = threads.getThreadsByIdx(indices);

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

                    threads.setThreadsPublishing(false, indices);
                }

            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }
        });

    }

    private void init(@NonNull Context context) {
        checkNotNull(context);


        BootstrapWorker.bootstrap(context);
        LoadNotificationsWorker.notifications(context, 10);
        DownloadContentsWorker.download(context);
        DownloadContentsWorker.periodic(context);

        JobServicePublisher.publish(context);
        JobServicePeers.peers(context);
        JobServiceFindPeers.findPeers(context);
        JobServiceCleanup.cleanup(context);

        new java.lang.Thread(() -> {
            try {
                LiteService.cleanStates(context);
            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }
        }).start();

    }

    private void attachHandler(@NonNull Context context) {
        checkNotNull(context);

        final PEERS peers = PEERS.getInstance(context);
        final EVENTS events = EVENTS.getInstance(context);

        try {

            final PID host = IPFS.getPID(context);
            IPFS.setPubSubHandler((message) -> {
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
                                    CID.create(result.getMultihash()));

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
                                } else if ("SHARE".equals(est)) {
                                    if (content.containsKey(Content.CID)) {
                                        String cid = content.get(Content.CID);
                                        checkNotNull(cid);

                                        JobServiceContents.contents(context,
                                                senderPid, CID.create(cid));
                                    } else {
                                        LoadNotificationsWorker.notifications(context, 3);
                                    }
                                }
                            } else {
                                events.error(context.getString(
                                        R.string.unsupported_pubsub_message,
                                        senderPid.getPid()));
                            }
                        } else if (result.getCodex() == CodecDecider.Codec.UNKNOWN) {

                            events.error(context.getString(
                                    R.string.unsupported_pubsub_message,
                                    senderPid.getPid()));
                        }

                    }
                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
                } finally {
                    Log.e(TAG, "Sender : " + message.getSenderPid() + " Message : " + message.getMessage());
                }


            });

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }

    }

}
