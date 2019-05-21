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

import org.iota.jota.pow.pearldiver.PearlDiverLocalPoW;

import java.io.File;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import threads.core.Network;
import threads.core.Preferences;
import threads.core.Singleton;
import threads.core.THREADS;
import threads.core.api.Content;
import threads.core.api.Kind;
import threads.core.api.Thread;
import threads.core.api.ThreadStatus;
import threads.core.api.User;
import threads.core.api.UserStatus;
import threads.core.api.UserType;
import threads.iota.IOTA;
import threads.ipfs.IPFS;
import threads.ipfs.api.CID;
import threads.ipfs.api.ConnMgrConfig;
import threads.ipfs.api.LinkInfo;
import threads.ipfs.api.Multihash;
import threads.ipfs.api.PID;
import threads.ipfs.api.PubsubConfig;
import threads.share.ConnectService;
import threads.share.RTCSession;
import threads.share.RelayService;

import static androidx.core.util.Preconditions.checkArgument;
import static androidx.core.util.Preconditions.checkNotNull;


public class Service {
    private static final Service SINGLETON = new Service();
    private static final String TAG = Service.class.getSimpleName();
    private static final Gson gson = new Gson();
    private static final ExecutorService UPLOAD_SERVICE = Executors.newFixedThreadPool(3);
    private static final ExecutorService DOWNLOAD_SERVICE = Executors.newFixedThreadPool(1);
    private final AtomicBoolean initDone = new AtomicBoolean(false);
    private static final String APP_KEY = "AppKey";
    private static final String UPDATE = "UPDATE";


    private final AtomicBoolean peerCheckFlag = new AtomicBoolean(false);
    private Service() {
    }

    public static boolean isInitialized() {
        return SINGLETON.initDone.get();
    }

    public static Service getInstance(@NonNull Context context) {
        if (!SINGLETON.initDone.getAndSet(true)) {

            runUpdatesIfNecessary(context);


            Preferences.createPublicPrivateKeys(context);
            ProgressChannel.createProgressChannel(context);
            RTCSession.createRTCChannel(context);

            try {

                Singleton.getInstance().init(context, () -> "",
                        NotificationFCMServer.getInstance(context), true);


                Preferences.setConfigChanged(context, false);

            } catch (Throwable e) {
                Preferences.evaluateException(Preferences.IPFS_INSTALL_FAILURE, e);
            }


            SINGLETON.startDaemon(context);
            SINGLETON.init(context);
        }
        return SINGLETON;
    }

    private static void pubsubDaemon(@NonNull Context context,
                                     @NonNull IPFS ipfs,
                                     @NonNull PID pid) throws Exception {
        checkNotNull(context);
        checkNotNull(ipfs);
        checkNotNull(pid);

        final THREADS threadsAPI = Singleton.getInstance().getThreads();


        Preferences.debug("Token : " + Preferences.getToken(context));
        Preferences.debug("Start Pubsub Daemon :" + pid.getPid());


        ipfs.pubsubSub(pid.getPid(), (message) -> {

            try {

                String sender = message.getSenderPid();
                PID senderPid = PID.create(sender);


                if (!threadsAPI.isAccountBlocked(senderPid)) {

                    String code = message.getMessage().trim();

                    CodecDecider result = CodecDecider.evaluate(code);

                    if (result.getCodex() == CodecDecider.Codec.MULTIHASH) {
                        Service.downloadMultihash(context, senderPid, result.getMultihash(), null);
                    } else if (result.getCodex() == CodecDecider.Codec.URI) {
                        Service.downloadMultihash(context, senderPid, result.getMultihash(), null);
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
                                    adaptUser(context, senderPid,
                                            alias, pubKey, UserType.VERIFIED);
                                }
                            } else if ("SHARE".equals(est)) {

                                if (content.containsKey(Content.CID)) {
                                    String cid = content.get(Content.CID);
                                    checkNotNull(cid);
                                    String title = content.get(Content.TITLE);
                                    Service.downloadMultihash(context, senderPid, cid, title);
                                }

                            } else if ("REPLY".equals(est)) {

                                if (content.containsKey(Content.CID)) {
                                    String cid = content.get(Content.CID);
                                    checkNotNull(cid);
                                    Service.publishReply(context, senderPid, cid);
                                }
                            } else {

                                RTCSession.handleContent(context, senderPid, content);
                            }


                        } else {
                            if (content.containsKey(Content.ALIAS)) {
                                String alias = content.get(Content.ALIAS);
                                checkNotNull(alias);
                                String pubKey = content.get(Content.PKEY);
                                if (pubKey == null) {
                                    pubKey = "";
                                }

                                createUser(context, senderPid, alias, pubKey, UserType.VERIFIED);
                            } else if (content.containsKey(Content.CID)) {
                                String cid = content.get(Content.CID);
                                checkNotNull(cid);
                                String title = content.get(Content.TITLE);
                                Service.downloadMultihash(context, senderPid, cid, title);
                            }
                        }
                    } else if (result.getCodex() == CodecDecider.Codec.UNKNOWN) {
                        // check content if might be a name
                        String name = senderPid.getPid();

                        try {
                            String alias = message.getMessage().trim();
                            if (alias.length() < name.length()) { // small shitty security
                                name = alias;
                            }
                        } catch (Throwable e) {
                            // ignore exception
                        }

                        createUser(context, senderPid, name, "", UserType.UNKNOWN);
                    }

                }
            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }

        });

    }

    private static void checkTangleServer(@NonNull Context context) {
        checkNotNull(context);
        try {
            if (Network.isConnected(context)) {
                IOTA iota = Singleton.getInstance().getIota();
                if (iota != null) {
                    try {
                        if (!IOTA.remotePoW(iota.getNodeInfo())) {
                            iota.setLocalPoW(new PearlDiverLocalPoW());
                        }
                    } catch (Throwable e) {
                        // TODO IOTA not running
                    }
                }
            }
        } catch (Throwable e) {
            Preferences.evaluateException(Preferences.EXCEPTION, e);
        }
    }

    private static void runUpdatesIfNecessary(@NonNull Context context) {
        try {
            int versionCode = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
            SharedPreferences prefs = context.getSharedPreferences(
                    APP_KEY, Context.MODE_PRIVATE);
            if (prefs.getInt(UPDATE, 0) != versionCode) {

                Preferences.setConfigChanged(context, true);

                Preferences.setPubsubEnabled(context, true);

                // Experimental Features
                Preferences.setQUICEnabled(context, true);
                Preferences.setFilestoreEnabled(context, false);


                Preferences.setApiPort(context, 5001);
                Preferences.setSwarmPort(context, 4001);


                Preferences.setAutoNATServiceEnabled(context, false);
                Preferences.setRelayHopEnabled(context, true); // TODO check
                Preferences.setAutoRelayEnabled(context, true); // TODO check

                Preferences.setPubsubRouter(context, PubsubConfig.RouterEnum.gossipsub);

                Preferences.setConnMgrConfigType(context, ConnMgrConfig.TypeEnum.basic);
                Preferences.setLowWater(context, 30);
                Preferences.setHighWater(context, 80);
                Preferences.setGracePeriod(context, "5s");


                Preferences.setConnectionTimeout(context, 30000);
                Preferences.setAutoConnectRelay(context, true); // TODO check

                Preferences.setTangleTimeout(context, 10);

                Preferences.setMdnsEnabled(context, false); // does not work anyway now TODO
                Preferences.setSilent(context, true); // TODO check
                Preferences.setDialRelay(context, true); // TODO check
                Preferences.setDebugMode(context, true); // TODO change in release mode

                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt(UPDATE, versionCode);
                editor.apply();
            }
        } catch (Throwable e) {
            // ignore exception
        }
    }

    private static boolean handleDirectoryLink(@NonNull Context context,
                                               @NonNull THREADS threads,
                                               @NonNull IPFS ipfs,
                                               @NonNull Thread thread,
                                               @NonNull LinkInfo link) {

        String filename = link.getName();
        threads.setAdditional(thread, Content.TITLE,
                filename.substring(0, filename.length() - 1), false);
        threads.setMimeType(thread, DocumentsContract.Document.MIME_TYPE_DIR);
        threads.setAdditional(thread, Preferences.THREAD_KIND, ThreadKind.NODE.name(), true);

        try {
            CID image = THREADS.createResourceImage(context, threads, ipfs, "",
                    R.drawable.folder_outline);
            threads.setImage(thread, image);
        } catch (Throwable e) {
            Preferences.evaluateException(Preferences.EXCEPTION, e);
        }
        try {
            CID cid = thread.getCid();
            if (cid != null) {
                threads.pin_add(ipfs, cid, -1, true);
            }
        } catch (Throwable e) {
            Preferences.evaluateException(Preferences.EXCEPTION, e);
        }

        int timeout = Preferences.getConnectionTimeout(context);
        List<LinkInfo> links = getLinks(ipfs, link, timeout);
        if (links != null) {
            return downloadLinks(context, threads, ipfs, thread, links);
        } else {
            return false;
        }

    }

    public void peersCheckEnable(boolean value) {
        peerCheckFlag.set(value);
    }

    private static void startPubsub(@NonNull Context context) {
        checkNotNull(context);
        final IPFS ipfs = Singleton.getInstance().getIpfs();
        if (ipfs != null) {
            if (ipfs.isDaemonRunning()) {
                if (Preferences.isPubsubEnabled(context)) {
                    final PID pid = Preferences.getPID(context);
                    checkNotNull(pid);
                    checkArgument(!pid.getPid().isEmpty());
                    try {
                        pubsubDaemon(context, ipfs, pid);
                    } catch (Throwable e) {
                        // IGNORE exception occurs when daemon is shutdown
                    }
                }
            }
        }
    }

    private static void adaptUser(@NonNull Context context,
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
            final THREADS threadsAPI = Singleton.getInstance().getThreads();
            final IPFS ipfs = Singleton.getInstance().getIpfs();
            if (ipfs != null) {
                User sender = threadsAPI.getUserByPID(senderPid);
                checkNotNull(sender);

                // create a new user which is blocked (User has to unblock and verified the user)
                byte[] data = THREADS.getImage(context, alias, R.drawable.server_network);
                CID image = ipfs.add(data, true);


                sender.setStatus(UserStatus.ONLINE);
                sender.setPublicKey(pubKey);
                sender.setAlias(alias);
                sender.setImage(image);
                sender.setType(userType);

                threadsAPI.storeUser(sender);

            }
        } catch (Throwable e) {
            // ignore exception
        }
    }

    private static void publishReply(@NonNull Context context,
                                     @NonNull PID sender,
                                     @NonNull String multihash) {
        checkNotNull(context);

        checkNotNull(sender);
        checkNotNull(multihash);

        try {
            final THREADS threads = Singleton.getInstance().getThreads();
            final IPFS ipfs = Singleton.getInstance().getIpfs();
            if (ipfs != null) {

                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.submit(() -> {
                    // check if multihash is valid
                    try {
                        Multihash.fromBase58(multihash);
                    } catch (Throwable e) {
                        Preferences.error(context.getString(R.string.multihash_not_valid));
                        return;
                    }

                    User user = threads.getUserByPID(sender);
                    if (user == null) {
                        Preferences.error(context.getString(R.string.unknown_peer_sends_data));
                        return;
                    }


                    CID cid = CID.create(multihash);
                    List<Thread> entries = threads.getThreadsByCID(cid);
                    for (Thread thread : entries) {
                        threads.incrementUnreadNotesNumber(thread);
                    }
                });
            }
        } catch (Throwable e) {
            Preferences.evaluateException(Preferences.EXCEPTION, e);
        }

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
            final THREADS threadsAPI = Singleton.getInstance().getThreads();
            final IPFS ipfs = Singleton.getInstance().getIpfs();
            if (ipfs != null) {
                User sender = threadsAPI.getUserByPID(senderPid);
                if (sender == null) {

                    // create a new user which is blocked (User has to unblock and verified the user)
                    byte[] data = THREADS.getImage(context, alias, R.drawable.server_network);
                    CID image = ipfs.add(data, true);

                    sender = threadsAPI.createUser(senderPid, pubKey, alias, userType, image);
                    sender.setStatus(UserStatus.BLOCKED);
                    threadsAPI.storeUser(sender);


                    PID host = Preferences.getPID(context);
                    checkNotNull(host);
                    User hostUser = threadsAPI.getUserByPID(host);
                    checkNotNull(hostUser);
                    Content map = new Content();
                    map.put(Content.EST, "CONNECT_REPLY");
                    map.put(Content.ALIAS, hostUser.getAlias());
                    map.put(Content.PKEY, hostUser.getPublicKey());


                    ipfs.pubsubPub(senderPid.getPid(), gson.toJson(map));

                    Preferences.error(context.getString(R.string.user_connect_try, alias));
                }
            }
        } catch (Throwable e) {
            // ignore exception
        }
    }

    public void checkPeersOnlineStatus(@NonNull Context context) {
        checkNotNull(context);
        final IPFS ipfs = Singleton.getInstance().getIpfs();
        if (ipfs != null) {
            try {
                if (ipfs.isDaemonRunning()) {
                    while (peerCheckFlag.get()) {
                        checkPeers(context);
                        java.lang.Thread.sleep(1000);
                    }
                }
            } catch (Throwable e) {
                // IGNORE exception occurs when daemon is shutdown
            }
        }
    }

    private static void replySender(@NonNull IPFS ipfs, @NonNull PID sender, @NonNull Thread thread) {
        CID cid = thread.getCid();
        checkNotNull(cid);

        Content map = new Content();
        map.put(Content.EST, "REPLY");
        map.put(Content.CID, cid.getCid());

        try {
            Log.e(TAG, "Send : " + map.toString());
            ipfs.pubsubPub(sender.getPid(), gson.toJson(map));
        } catch (Throwable e) {
            Preferences.evaluateException(Preferences.EXCEPTION, e);
        }
    }

    static void storeData(@NonNull Context context, @NonNull Uri uri) {
        checkNotNull(context);
        checkNotNull(uri);


        THREADS.FileDetails fileDetails = THREADS.getFileDetails(context, uri);
        if (fileDetails == null) {
            Preferences.error(context.getString(R.string.file_not_supported));
            return;
        }

        final IPFS ipfs = Singleton.getInstance().getIpfs();
        final THREADS threadsAPI = Singleton.getInstance().getThreads();
        if (ipfs != null) {

            UPLOAD_SERVICE.submit(() -> {
                try {
                    InputStream inputStream =
                            context.getContentResolver().openInputStream(uri);
                    checkNotNull(inputStream);

                    PID pid = Preferences.getPID(context);
                    checkNotNull(pid);
                    User host = threadsAPI.getUserByPID(pid);
                    checkNotNull(host);


                    byte[] bytes;
                    try {
                        bytes = THREADS.getPreviewImage(context, uri);
                        if (bytes == null) {
                            bytes = THREADS.getImage(context, host.getAlias(),
                                    R.drawable.file_document);
                        }
                    } catch (Throwable e) {
                        // ignore exception
                        bytes = THREADS.getImage(context, host.getAlias(),
                                R.drawable.file_document);
                    }

                    Thread thread = threadsAPI.createThread(host, ThreadStatus.OFFLINE, Kind.IN,
                            "", null, 0L, false);
                    thread.addAdditional(Content.TITLE, fileDetails.getFileName(), false);
                    thread.addAdditional(Preferences.THREAD_KIND, ThreadKind.LEAF.name(), false);
                    CID image = ipfs.add(bytes, true);
                    thread.setImage(image);
                    thread.setMimeType(fileDetails.getMimeType());
                    long idx = threadsAPI.storeThread(thread);


                    thread = threadsAPI.getThreadByIdx(idx); // TODO optimize here
                    checkNotNull(thread);
                    try {

                        threadsAPI.setStatus(thread, ThreadStatus.LEACHING);

                        CID cid = ipfs.add(inputStream, fileDetails.getFileName(),
                                true, true);
                        checkNotNull(cid);

                        // cleanup of entries with same CID
                        List<Thread> sameEntries = threadsAPI.getThreadsByCID(cid);
                        for (Thread entry : sameEntries) {
                            threadsAPI.removeThread(entry);
                        }

                        threadsAPI.setCID(thread, cid);
                        threadsAPI.setStatus(thread, ThreadStatus.ONLINE);
                    } catch (Throwable e) {
                        threadsAPI.setStatus(thread, ThreadStatus.ERROR);
                    } finally {
                        Preferences.event(Preferences.THREAD_SELECT_EVENT,
                                String.valueOf(thread.getIdx()));
                    }

                } catch (Throwable e) {
                    Preferences.evaluateException(Preferences.EXCEPTION, e);
                }
            });
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
            Preferences.evaluateException(Preferences.EXCEPTION, e);
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

        final THREADS threads = Singleton.getInstance().getThreads();

        try {
            threads.setUserStatus(UserStatus.DIALING, UserStatus.OFFLINE);
            threads.setThreadStatus(ThreadStatus.LEACHING, ThreadStatus.ERROR);
            threads.setThreadStatus(ThreadStatus.OFFLINE, ThreadStatus.ERROR);
        } catch (Throwable e) {
            Preferences.evaluateException(Preferences.EXCEPTION, e);
        }

    }

    private static void createHost(@NonNull Context context) {
        checkNotNull(context);

        final THREADS threads = Singleton.getInstance().getThreads();
        final IPFS ipfs = Singleton.getInstance().getIpfs();
        if (ipfs != null) {
            try {
                PID pid = Preferences.getPID(context);
                checkNotNull(pid);

                User user = threads.getUserByPID(pid);
                if (user == null) {
                    String publicKey = Preferences.getPublicKey(context);
                    byte[] data = THREADS.getImage(context, pid.getPid(), R.drawable.server_network);

                    CID image = ipfs.add(data, true);

                    user = threads.createUser(pid, publicKey,
                            getDeviceName(), UserType.VERIFIED, image);
                    user.setStatus(UserStatus.BLOCKED);
                    threads.storeUser(user);
                }
            } catch (Throwable e) {
                Preferences.evaluateException(Preferences.EXCEPTION, e);
            }
        }
    }

    static void deleteThreads(@NonNull List<Long> idxs) {
        checkNotNull(idxs);
        final THREADS threadsAPI = Singleton.getInstance().getThreads();

        final IPFS ipfs = Singleton.getInstance().getIpfs();
        if (ipfs != null) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {

                try {

                    for (long idx : idxs) {
                        threadsAPI.setThreadStatus(idx, ThreadStatus.DELETING);
                    }

                    for (long idx : idxs) {
                        deleteThread(ipfs, idx);
                    }
                    threadsAPI.repo_gc(ipfs);

                } catch (Throwable e) {
                    Preferences.evaluateException(Preferences.EXCEPTION, e);
                }
            });
        }
    }

    static void deleteThread(@NonNull IPFS ipfs, long idx) {
        checkNotNull(ipfs);
        try {
            final THREADS threadsAPI = Singleton.getInstance().getThreads();
            Thread thread = threadsAPI.getThreadByIdx(idx);
            if (thread != null) {
                threadsAPI.removeThread(ipfs, thread);
            }
        } catch (Throwable e) {
            Preferences.evaluateException(Preferences.EXCEPTION, e);
        }
    }

    static void downloadMultihash(@NonNull Context context,
                                  @NonNull PID sender,
                                  @NonNull String multihash,
                                  @Nullable String filename) {

        checkNotNull(context);
        checkNotNull(sender);
        checkNotNull(multihash);

        final THREADS threads = Singleton.getInstance().getThreads();

        final PID pid = Preferences.getPID(context);
        checkNotNull(pid);

        final IPFS ipfs = Singleton.getInstance().getIpfs();
        if (ipfs != null) {

            DOWNLOAD_SERVICE.submit(() -> {
                try {
                    // check if multihash is valid
                    try {
                        Multihash.fromBase58(multihash);
                    } catch (Throwable e) {
                        Preferences.error(context.getString(R.string.multihash_not_valid));
                        return;
                    }

                    User user = threads.getUserByPID(sender);
                    if (user == null) {
                        Preferences.error(context.getString(R.string.unknown_peer_sends_data));
                        return;
                    }


                    CID cid = CID.create(multihash);
                    List<Thread> entries = threads.getThreadsByCID(cid);
                    if (!entries.isEmpty()) {
                        for (Thread entry : entries) {
                            if (entry.getStatus() == ThreadStatus.DELETING ||
                                    entry.getStatus() == ThreadStatus.ONLINE ||
                                    entry.getStatus() == ThreadStatus.PUBLISHING) {
                                replySender(ipfs, sender, entry);
                            } else {
                                downloadMultihash(context, threads, ipfs, entry, sender);
                            }
                        }

                    } else {
                        long idx = createThread(context, ipfs, sender, cid, filename, 0L);
                        Thread thread = threads.getThreadByIdx(idx);
                        checkNotNull(thread);
                        downloadMultihash(context, threads, ipfs, thread, sender);

                    }

                } catch (Throwable e) {
                    Preferences.evaluateException(Preferences.EXCEPTION, e);
                }
            });
        }
    }

    private static void evaluateMimeType(@NonNull Thread thread, @NonNull String filename) {
        final THREADS threads = Singleton.getInstance().getThreads();
        try {
            Optional<String> extension = THREADS.getExtension(filename);
            if (extension.isPresent()) {
                String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.get());
                if (mimeType != null) {
                    threads.setMimeType(thread, mimeType);
                } else {
                    threads.setMimeType(thread, Preferences.OCTET_MIME_TYPE); // not know what type
                }
            } else {
                threads.setMimeType(thread, Preferences.OCTET_MIME_TYPE); // not know what type
            }
        } catch (Throwable e) {
            Preferences.evaluateException(Preferences.EXCEPTION, e);
        }
    }

    private static long createThread(@NonNull Context context,
                                     @NonNull IPFS ipfs,
                                     @NonNull PID creator,
                                     @NonNull CID cid,
                                     @Nullable String filename,
                                     long parent) {

        checkNotNull(context);
        checkNotNull(ipfs);
        checkNotNull(creator);
        checkNotNull(cid);


        final THREADS threads = Singleton.getInstance().getThreads();

        User user = threads.getUserByPID(creator);
        checkNotNull(user);

        Thread thread = threads.createThread(user, ThreadStatus.OFFLINE, Kind.OUT,
                "", cid, parent, false);
        thread.addAdditional(Preferences.THREAD_KIND, ThreadKind.LEAF.name(), false); // not known yet
        if (filename != null) {
            thread.addAdditional(Content.TITLE, filename, false);
            evaluateMimeType(thread, filename);
        } else {
            thread.setMimeType(Preferences.OCTET_MIME_TYPE); // not known yet
            thread.addAdditional(Content.TITLE, cid.getCid(), false);
        }


        try {
            byte[] image = THREADS.getImage(context.getApplicationContext(),
                    user.getAlias(), R.drawable.file_document);
            thread.setImage(ipfs.add(image, true));
        } catch (Throwable e) {
            Preferences.evaluateException(Preferences.EXCEPTION, e);
        }

        return threads.storeThread(thread);
    }

    static void localDownloadThread(@NonNull Context context, long idx) {
        checkNotNull(context);
        try {
            final THREADS threadsAPI = Singleton.getInstance().getThreads();
            final DownloadManager downloadManager = (DownloadManager)
                    context.getSystemService(Context.DOWNLOAD_SERVICE);
            checkNotNull(downloadManager);

            final IPFS ipfs = Singleton.getInstance().getIpfs();

            if (ipfs != null) {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.submit(() -> {
                    try {
                        Thread threadObject = threadsAPI.getThreadByIdx(idx);
                        checkNotNull(threadObject);

                        CID cid = threadObject.getCid();
                        checkNotNull(cid);

                        int timeout = Preferences.getConnectionTimeout(context);

                        List<LinkInfo> links = threadsAPI.getLinks(ipfs, threadObject,
                                timeout, true);
                        checkNotNull(links);
                        int size = -1;
                        if (links.size() == 1) {
                            LinkInfo link = links.get(0);
                            cid = link.getCid();
                            size = link.getSize();
                        }
                        String title = threadObject.getAdditional(Content.TITLE);

                        File dir = Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_DOWNLOADS);
                        File file = new File(dir, title);


                        NotificationCompat.Builder builder =
                                ProgressChannel.createProgressNotification(
                                        context, title);

                        final NotificationManager notificationManager = (NotificationManager)
                                context.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
                        int notifyID = cid.hashCode();
                        Notification notification = builder.build();
                        if (notificationManager != null) {
                            notificationManager.notify(notifyID, notification);
                        }

                        try {
                            boolean finished = threadsAPI.download(ipfs, file, cid,
                                    false, true, threadObject.getSesKey(),
                                    new THREADS.Progress() {
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
                            Preferences.evaluateException(Preferences.EXCEPTION, e);
                        } finally {

                            if (notificationManager != null) {
                                notificationManager.cancel(notifyID);
                            }
                        }

                    } catch (Throwable e) {
                        Preferences.evaluateException(Preferences.EXCEPTION, e);
                    }
                });
            }
        } catch (Throwable e) {
            Preferences.evaluateException(Preferences.EXCEPTION, e);
        }
    }

    private static boolean downloadUnknown(@NonNull Context context,
                                           @NonNull THREADS threads,
                                           @NonNull IPFS ipfs,
                                           @NonNull Thread thread) {

        checkNotNull(context);
        checkNotNull(threads);
        checkNotNull(ipfs);
        checkNotNull(thread);

        // UPDATE UI
        Preferences.event(Preferences.THREAD_SELECT_EVENT, String.valueOf(thread.getIdx()));


        CID cid = thread.getCid();
        checkNotNull(cid);

        threads.setAdditional(thread, Preferences.THREAD_KIND, ThreadKind.LEAF.name(), true);
        String filename = thread.getAdditional(Content.TITLE);
        return download(context, threads, ipfs, thread, cid, filename, -1); // size not known
    }

    private static boolean download(@NonNull Context context,
                                    @NonNull THREADS threads,
                                    @NonNull IPFS ipfs,
                                    @NonNull Thread thread,
                                    @NonNull CID cid,
                                    @NonNull String filename,
                                    @NonNull Integer size) {

        checkNotNull(context);
        checkNotNull(threads);
        checkNotNull(ipfs);
        checkNotNull(thread);
        checkNotNull(cid);
        checkNotNull(filename);
        checkNotNull(size);

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
        File file = getCacheFile(context, System.currentTimeMillis() + filename);

        boolean success;
        try {
            threads.setStatus(thread, ThreadStatus.LEACHING); // make sure
            int timeout = Preferences.getConnectionTimeout(context);
            success = threads.download(ipfs, file, cid, true, false,
                    thread.getSesKey(), new THREADS.Progress() {
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

            if (success) {
                try {
                    byte[] image = THREADS.getPreviewImage(context, file);
                    if (image != null) {
                        threads.setImage(ipfs, thread, image, true);
                    }
                } catch (Throwable e) {
                    // no exception will be reported
                }
            } else {
                try {
                    CID image = THREADS.createResourceImage(
                            context, threads, ipfs, "", R.drawable.file_document);
                    threads.setImage(thread, image);
                } catch (Throwable e) {
                    Preferences.evaluateException(Preferences.EXCEPTION, e);
                }
            }
        } catch (Throwable e) {
            success = false;
        } finally {
            if (notificationManager != null) {
                notificationManager.cancel(notifyID);
            }
            if (file.exists()) {
                checkArgument(file.delete());
            }
        }

        return success;
    }

    private void checkPeers(@NonNull Context context) {
        checkNotNull(context);
        PID hostPID = Preferences.getPID(context);
        try {
            final THREADS threads = Singleton.getInstance().getThreads();
            if (Network.isConnected(context)) {

                final IPFS ipfs = Singleton.getInstance().getIpfs();
                if (ipfs != null) {

                    List<User> users = threads.getUsers();

                    if (hostPID != null) {
                        User host = threads.getUserByPID(hostPID);
                        users.remove(host);
                    }
                    if (Network.isConnected(context)) {
                        for (User user : users) {
                            UserStatus currentStatus = user.getStatus();
                            if (currentStatus != UserStatus.BLOCKED &&
                                    currentStatus != UserStatus.DIALING) {
                                try {
                                    boolean value = ipfs.swarmConnected(user.getPID());
                                    if (value) {
                                        if (threads.getStatus(user) != UserStatus.DIALING) {
                                            threads.setStatus(user, UserStatus.ONLINE);
                                        }
                                    } else {
                                        if (threads.getStatus(user) != UserStatus.DIALING) {
                                            threads.setStatus(user, UserStatus.OFFLINE);
                                        }
                                    }
                                } catch (Throwable e) {
                                    if (threads.getStatus(user) != UserStatus.DIALING) {
                                        threads.setStatus(user, UserStatus.OFFLINE);
                                    }
                                }
                            }
                        }
                    } else {
                        for (User user : users) {
                            if (UserStatus.OFFLINE != user.getStatus()) {
                                threads.setStatus(user, UserStatus.OFFLINE);
                            }

                        }
                    }
                }
            } else {
                List<User> users = threads.getUsers();
                for (User user : users) {

                    if (UserStatus.OFFLINE != user.getStatus()) {
                        threads.setStatus(user, UserStatus.OFFLINE);
                    }

                }
            }
        } catch (Throwable e) {
            Preferences.evaluateException(Preferences.EXCEPTION, e);
        }
    }

    @Nullable
    private static List<LinkInfo> getLinks(@NonNull IPFS ipfs, @NonNull LinkInfo link, int timeout) {
        checkNotNull(ipfs);
        checkNotNull(link);

        CID cid = link.getCid();
        List<LinkInfo> links = null;
        try {
            links = ipfs.ls(cid, timeout, false);
        } catch (Throwable e) {
            // ignore exception
        }
        return links;
    }

    private static boolean handleContentLink(@NonNull Context context,
                                             @NonNull THREADS threads,
                                             @NonNull IPFS ipfs,
                                             @NonNull Thread thread,
                                             @NonNull LinkInfo link) {

        String filename = link.getName();
        threads.setAdditional(thread, Content.TITLE, filename, false);
        evaluateMimeType(thread, filename);
        threads.setAdditional(thread, Preferences.THREAD_KIND, ThreadKind.LEAF.name(), true);


        return download(context, threads, ipfs, thread, link.getCid(), link.getName(), link.getSize());

    }

    private static boolean downloadLink(@NonNull Context context,
                                        @NonNull THREADS threads,
                                        @NonNull IPFS ipfs,
                                        @NonNull Thread thread,
                                        @NonNull LinkInfo link) {
        // UPDATE UI
        Preferences.event(Preferences.THREAD_SELECT_EVENT, String.valueOf(thread.getIdx()));

        String filename = link.getName();


        if (filename.endsWith("/")) {
            // assume this is a directory
            return handleDirectoryLink(context, threads, ipfs, thread, link);
        } else {
            return handleContentLink(context, threads, ipfs, thread, link);
        }


    }

    private static boolean downloadLinks(@NonNull Context context,
                                         @NonNull THREADS threads,
                                         @NonNull IPFS ipfs,
                                         @NonNull Thread thread,
                                         @NonNull List<LinkInfo> links) {
        // UPDATE UI
        Preferences.event(Preferences.THREAD_SELECT_EVENT, String.valueOf(thread.getIdx()));


        AtomicInteger successCounter = new AtomicInteger(0);
        for (LinkInfo link : links) {

            boolean success = false;
            CID cid = link.getCid();
            List<Thread> entries = threads.getThreadsByCID(cid);
            if (!entries.isEmpty()) {
                for (Thread entry : entries) {
                    if (entry.getStatus() != ThreadStatus.ONLINE) {
                        success = downloadLink(context, threads, ipfs, entry, link);
                    } else {
                        // UPDATE UI
                        Preferences.event(Preferences.THREAD_SELECT_EVENT,
                                String.valueOf(entry.getIdx()));
                        success = true;
                    }
                }
            } else {

                long idx = createThread(context, ipfs,
                        thread.getSenderPid(), cid, null, thread.getIdx());
                Thread entry = threads.getThreadByIdx(idx);
                checkNotNull(entry);
                success = downloadLink(context, threads, ipfs, entry, link);

                if (success) {
                    threads.setStatus(entry, ThreadStatus.ONLINE);
                } else {
                    threads.setStatus(entry, ThreadStatus.ERROR);
                }

            }

            if (success) {
                successCounter.incrementAndGet();
            }
        }

        return successCounter.get() == links.size();
    }

    private static void downloadMultihash(@NonNull Context context,
                                          @NonNull THREADS threads,
                                          @NonNull IPFS ipfs,
                                          @NonNull Thread thread,
                                          @Nullable PID sender) {
        checkNotNull(context);
        checkNotNull(threads);
        checkNotNull(ipfs);
        checkNotNull(thread);


        threads.setStatus(thread, ThreadStatus.LEACHING);
        int timeout = Preferences.getConnectionTimeout(context);
        List<LinkInfo> links = threads.getLinks(ipfs, thread, timeout, false);

        if (links != null) {
            if (links.isEmpty()) {
                boolean result = downloadUnknown(context, threads, ipfs, thread);
                if (result) {
                    threads.setStatus(thread, ThreadStatus.ONLINE);
                    if (sender != null) {
                        replySender(ipfs, sender, thread);
                    }
                } else {
                    threads.setStatus(thread, ThreadStatus.ERROR);
                }

            } else if (links.size() > 1) {

                // thread is directory
                threads.setMimeType(thread, DocumentsContract.Document.MIME_TYPE_DIR);
                threads.setAdditional(thread, Preferences.THREAD_KIND, ThreadKind.NODE.name(), true);

                try {
                    CID image = THREADS.createResourceImage(context, threads, ipfs, "",
                            R.drawable.folder_outline);
                    threads.setImage(thread, image);
                } catch (Throwable e) {
                    Preferences.evaluateException(Preferences.EXCEPTION, e);
                }

                boolean result = downloadLinks(context, threads, ipfs, thread, links);
                if (result) {
                    threads.setStatus(thread, ThreadStatus.ONLINE);
                    if (sender != null) {
                        replySender(ipfs, sender, thread);
                    }
                } else {
                    threads.setStatus(thread, ThreadStatus.ERROR);
                }
            } else {

                LinkInfo link = links.get(0);

                boolean result = downloadLink(context, threads, ipfs, thread, link);
                if (result) {
                    threads.setStatus(thread, ThreadStatus.ONLINE);
                    if (sender != null) {
                        replySender(ipfs, sender, thread);
                    }
                } else {
                    threads.setStatus(thread, ThreadStatus.ERROR);
                }
            }
        } else {
            threads.setStatus(thread, ThreadStatus.ERROR);
        }

    }

    @NonNull
    static File getCacheFile(@NonNull Context context, @NonNull String name) {
        checkNotNull(context);
        checkNotNull(name);
        File dir = context.getCacheDir();
        File file = new File(dir, name);
        if (!file.exists()) {
            try {
                if (!file.createNewFile()) {
                    throw new RuntimeException("File couldn't be created.");
                }
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
        return file;
    }

    void downloadThread(@NonNull Context context, @NonNull Thread thread) {

        checkNotNull(context);
        checkNotNull(thread);

        final THREADS threads = Singleton.getInstance().getThreads();
        final IPFS ipfs = Singleton.getInstance().getIpfs();
        if (ipfs != null) {
            threads.setStatus(thread, ThreadStatus.LEACHING);
            PID host = Preferences.getPID(context);
            checkNotNull(host);
            PID sender = thread.getSenderPid();
            if (!host.equals(sender)) {

                final boolean pubsubEnabled = Preferences.isPubsubEnabled(context);
                boolean pubsubCheck = false;
                if (pubsubEnabled) {
                    User user = threads.getUserByPID(sender);
                    if (user != null) {
                        pubsubCheck = !user.getPublicKey().isEmpty();
                    }
                }
                Hashtable<String, String> params = new Hashtable<>();
                ConnectService.wakeupConnectCall(
                        context, sender, params, pubsubCheck);

            }

            Service.downloadMultihash(context, threads, ipfs, thread, null);
        }

    }

    private boolean shareUser(@NonNull Context context,
                              @NonNull User user,
                              @NonNull List<Long> idxs) {
        checkNotNull(user);
        checkNotNull(idxs);
        final THREADS threads = Singleton.getInstance().getThreads();
        final IPFS ipfs = Singleton.getInstance().getIpfs();
        boolean success = false;
        if (ipfs != null) {
            try {
                final boolean pubsubEnabled = Preferences.isPubsubEnabled(context);

                final boolean pubsubCheck = pubsubEnabled && !
                        user.getPublicKey().isEmpty();
                Hashtable<String, String> params = new Hashtable<>();
                if (ConnectService.wakeupConnectCall(context, user.getPID(), params, pubsubCheck)) {

                    for (long idx : idxs) {
                        Thread threadObject = threads.getThreadByIdx(idx);
                        checkNotNull(threadObject);

                        CID cid = threadObject.getCid();
                        checkNotNull(cid);

                        Content map = new Content();
                        map.put(Content.EST, "SHARE");
                        String title = threadObject.getAdditional(Content.TITLE);
                        if (!title.isEmpty()) {
                            map.put(Content.TITLE, title);
                        }
                        map.put(Content.CID, cid.getCid());
                        Log.e(TAG, "Send : " + map.toString());
                        ipfs.pubsubPub(user.getPID().getPid(), gson.toJson(map));
                    }

                    success = true;
                }
            } catch (Throwable e) {
                Preferences.evaluateException(Preferences.EXCEPTION, e);
            }
        }
        return success;
    }

    void sendThreads(@NonNull Context context,
                     @NonNull List<Long> idxs) {
        checkNotNull(context);

        final PID host = Preferences.getPID(context.getApplicationContext());

        final THREADS threads = Singleton.getInstance().getThreads();
        final IPFS ipfs = Singleton.getInstance().getIpfs();

        if (ipfs != null) {

            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    // clean-up
                    for (long idx : idxs) {
                        threads.resetUnreadNotesNumber(idx);
                    }

                    List<User> users = threads.getUsers();
                    if (users.isEmpty()) {
                        Preferences.warning(context.getString(R.string.no_peers_connected));
                    } else {
                        checkNotNull(host);

                        ExecutorService sharedExecutor = Executors.newFixedThreadPool(5);
                        LinkedList<Future<Boolean>> futures = new LinkedList<>();
                        for (User user : users) {
                            if (user.getStatus() != UserStatus.BLOCKED) {
                                PID userPID = user.getPID();
                                if (!userPID.equals(host)) {

                                    Future<Boolean> future = sharedExecutor.submit(() ->
                                            shareUser(context, user, idxs));
                                    futures.add(future);
                                }
                            }
                        }
                        int counter = 0;
                        for (Future<Boolean> future : futures) {
                            Boolean send = future.get();
                            if (send) {
                                counter++;
                            }
                        }

                        Preferences.warning(context.getString(R.string.data_shared_with_peers,
                                String.valueOf(counter)));


                    }

                } catch (Throwable e) {
                    Preferences.evaluateException(Preferences.EXCEPTION, e);
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
                Preferences.evaluateException(Preferences.EXCEPTION, e);
            }
        }).start();

    }

    private void startDaemon(@NonNull Context context) {
        checkNotNull(context);
        try {
            final IPFS ipfs = Singleton.getInstance().getIpfs();
            if (ipfs != null) {

                try {
                    Preferences.debug("Start Daemon");

                    ipfs.daemon(Preferences.isPubsubEnabled(context));

                } catch (Throwable e) {

                    Preferences.evaluateException(Preferences.IPFS_START_FAILURE, e);
                }

                if (ipfs.isDaemonRunning()) {
                    new java.lang.Thread(() -> startPubsub(context)).start();
                }

                if (Preferences.isAutoConnectRelay(context)) {
                    int timeout = Preferences.getConnectionTimeout(context);
                    new java.lang.Thread(() -> RelayService.connectRelays(
                            context, 10000, timeout)).start();
                }
                new java.lang.Thread(() -> checkTangleServer(context)).start();
            }
        } catch (Throwable e) {
            Preferences.evaluateException(Preferences.EXCEPTION, e);
        }
    }


    public enum ThreadKind {
        LEAF, NODE
    }


}
