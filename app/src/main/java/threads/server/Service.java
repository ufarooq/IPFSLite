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

import org.iota.jota.pow.pearldiver.PearlDiverLocalPoW;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
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
import threads.ipfs.api.RoutingConfig;
import threads.share.ConnectService;
import threads.share.RTCSession;
import threads.share.RelayService;

import static androidx.core.util.Preconditions.checkArgument;
import static androidx.core.util.Preconditions.checkNotNull;


public class Service {
    private static final String TAG = Service.class.getSimpleName();
    private static final Gson gson = new Gson();
    private static final ExecutorService UPLOAD_SERVICE = Executors.newFixedThreadPool(3);
    private static final ExecutorService DOWNLOAD_SERVICE = Executors.newFixedThreadPool(2);
    private static final String APP_KEY = "AppKey";
    private static final String UPDATE = "UPDATE";
    private static Service SINGLETON = null;
    private final AtomicBoolean peerCheckFlag = new AtomicBoolean(false);

    private Service() {
    }

    @NonNull
    public static Service getInstance(@NonNull Context context) {
        checkNotNull(context);
        if (SINGLETON == null) {

            runUpdatesIfNecessary(context);


            ProgressChannel.createProgressChannel(context);
            RTCSession.createRTCChannel(context);

            Singleton singleton = Singleton.getInstance(context);
            singleton.setAesKey(() -> "");
            singleton.setNotificationServer(NotificationFCMServer.getInstance());

            SINGLETON = new Service();
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

        final THREADS threadsAPI = Singleton.getInstance(context).getThreads();


        Singleton.getInstance(context).getConsoleListener().debug(
                "Token : " + Preferences.getToken(context));


        ipfs.pubsubSub(pid.getPid(), (message) -> {

            try {

                String sender = message.getSenderPid();
                PID senderPid = PID.create(sender);


                if (!threadsAPI.isAccountBlocked(senderPid)) {

                    String code = message.getMessage().trim();

                    CodecDecider result = CodecDecider.evaluate(code);

                    if (result.getCodex() == CodecDecider.Codec.MULTIHASH) {
                        Service.downloadMultihash(context, senderPid,
                                result.getMultihash(), null, null);
                    } else if (result.getCodex() == CodecDecider.Codec.URI) {
                        Service.downloadMultihash(context, senderPid,
                                result.getMultihash(), null, null);
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
                                    String filename = content.get(Content.FILENAME);
                                    String filesize = content.get(Content.FILESIZE);
                                    Service.downloadMultihash(context, senderPid, cid, filename, filesize);
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
                                String filename = content.get(Content.FILENAME);
                                String filesize = content.get(Content.FILESIZE);
                                Service.downloadMultihash(context, senderPid, cid, filename, filesize);
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
            } finally {
                Log.e(TAG, "Receive : " + message.getMessage());
            }

        });

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
                    // TODO IOTA not running
                }

            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    private static void runUpdatesIfNecessary(@NonNull Context context) {
        try {
            int versionCode = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
            SharedPreferences prefs = context.getSharedPreferences(
                    APP_KEY, Context.MODE_PRIVATE);
            if (prefs.getInt(UPDATE, 0) != versionCode) {

                Preferences.setPubsubEnabled(context, true);

                // Experimental Features
                Preferences.setQUICEnabled(context, true);
                Preferences.setFilestoreEnabled(context, false);
                Preferences.setPreferTLS(context, false);


                Preferences.setSwarmPort(context, 4001);
                Preferences.setRoutingType(context, RoutingConfig.TypeEnum.dhtclient);


                Preferences.setAutoNATServiceEnabled(context, false);
                Preferences.setRelayHopEnabled(context, true);
                Preferences.setAutoRelayEnabled(context, true);

                Preferences.setPubsubRouter(context, PubsubConfig.RouterEnum.gossipsub);
                Preferences.setReproviderInterval(context, "0");

                Preferences.setConnMgrConfigType(context, ConnMgrConfig.TypeEnum.basic);
                Preferences.setLowWater(context, 30);
                Preferences.setHighWater(context, 100);
                Preferences.setGracePeriod(context, "10s");


                Preferences.setConnectionTimeout(context, 45000);
                Preferences.setAutoConnectRelay(context, true);

                Preferences.setTangleTimeout(context, 15);

                Preferences.setMdnsEnabled(context, true);

                Preferences.setReportMode(context, true);
                Preferences.setDialRelay(context, true);
                Preferences.setDebugMode(context, false);

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
        threads.setAdditional(thread, Content.FILENAME, filename, false);
        long size = link.getSize();

        threads.setAdditional(thread, Content.FILESIZE, String.valueOf(size), false);
        threads.setMimeType(thread, DocumentsContract.Document.MIME_TYPE_DIR);
        threads.setAdditional(thread, Preferences.THREAD_KIND, ThreadKind.NODE.name(), true);

        try {
            CID image = THREADS.createResourceImage(context, threads, ipfs,
                    R.drawable.folder_outline, "");
            checkNotNull(image);
            threads.setImage(thread, image);
        } catch (Throwable e) {
            Preferences.evaluateException(threads, Preferences.EXCEPTION, e);
        }
        try {
            CID cid = thread.getCid();
            if (cid != null) {
                threads.pin_add(ipfs, cid, -1, true);
            }
        } catch (Throwable e) {
            Preferences.evaluateException(threads, Preferences.EXCEPTION, e);
        }

        List<LinkInfo> links = getLinks(context, ipfs, link.getCid());
        if (links != null) {
            return downloadLinks(context, threads, ipfs, thread, links);
        } else {
            return false;
        }

    }

    private static void startPubsub(@NonNull Context context) {
        checkNotNull(context);
        final IPFS ipfs = Singleton.getInstance(context).getIpfs();
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
            final THREADS threadsAPI = Singleton.getInstance(context).getThreads();
            final IPFS ipfs = Singleton.getInstance(context).getIpfs();
            if (ipfs != null) {
                User sender = threadsAPI.getUserByPID(senderPid);
                checkNotNull(sender);

                // create a new user which is blocked (User has to unblock and verified the user)
                byte[] data = THREADS.getImage(context, alias, R.drawable.server_network);
                CID image = ipfs.add(data, "", true);


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
            final THREADS threads = Singleton.getInstance(context).getThreads();
            final IPFS ipfs = Singleton.getInstance(context).getIpfs();
            if (ipfs != null) {

                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.submit(() -> {
                    // check if multihash is valid
                    try {
                        Multihash.fromBase58(multihash);
                    } catch (Throwable e) {
                        Preferences.error(threads, context.getString(R.string.multihash_not_valid));
                        return;
                    }

                    User user = threads.getUserByPID(sender);
                    if (user == null) {
                        Preferences.error(threads, context.getString(R.string.unknown_peer_sends_data));
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
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
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
            final THREADS threadsAPI = Singleton.getInstance(context).getThreads();
            final IPFS ipfs = Singleton.getInstance(context).getIpfs();
            if (ipfs != null) {
                User sender = threadsAPI.getUserByPID(senderPid);
                if (sender == null) {

                    // create a new user which is blocked (User has to unblock and verified the user)
                    byte[] data = THREADS.getImage(context, alias, R.drawable.server_network);
                    CID image = ipfs.add(data, "", true);

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


                    ipfs.pubsubPub(senderPid.getPid(), gson.toJson(map), 50);

                    Preferences.error(threadsAPI, context.getString(R.string.user_connect_try, alias));
                }
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    private static void replySender(@NonNull IPFS ipfs, @NonNull PID sender, @NonNull Thread thread) {
        try {
            CID cid = thread.getCid();
            checkNotNull(cid);

            Content map = new Content();
            map.put(Content.EST, "REPLY");
            map.put(Content.CID, cid.getCid());


            Log.e(TAG, "Send : " + map.toString());
            ipfs.pubsubPub(sender.getPid(), gson.toJson(map), 50);
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    static void storeData(@NonNull Context context, @NonNull Uri uri) {
        checkNotNull(context);
        checkNotNull(uri);

        final THREADS threads = Singleton.getInstance(context).getThreads();
        THREADS.FileDetails fileDetails = THREADS.getFileDetails(context, uri);
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

                    String name = fileDetails.getFileName();
                    long size = fileDetails.getFileSize();

                    Thread thread = threads.createThread(host, ThreadStatus.OFFLINE, Kind.IN,
                            "", null, 0L, false);
                    thread.addAdditional(Content.FILENAME, name, false);
                    thread.addAdditional(Preferences.THREAD_KIND, ThreadKind.LEAF.name(), false);
                    thread.addAdditional(Content.FILESIZE, String.valueOf(size), false);

                    CID image = ipfs.add(bytes, "", true);
                    thread.setImage(image);
                    thread.setMimeType(fileDetails.getMimeType());
                    long idx = threads.storeThread(thread);

                    Preferences.event(threads, Preferences.THREAD_SCROLL_EVENT, "");

                    thread = threads.getThreadByIdx(idx); // TODO optimize here
                    checkNotNull(thread);
                    try {

                        threads.setStatus(thread, ThreadStatus.LEACHING);

                        CID cid = ipfs.add(inputStream, "", true);
                        checkNotNull(cid);

                        // cleanup of entries with same CID
                        List<Thread> sameEntries = threads.getThreadsByCID(cid);
                        for (Thread entry : sameEntries) {
                            threads.removeThread(entry);
                        }

                        threads.setCID(thread, cid);
                        threads.setStatus(thread, ThreadStatus.ONLINE);
                    } catch (Throwable e) {
                        threads.setStatus(thread, ThreadStatus.ERROR);
                    } finally {
                        Preferences.event(threads, Preferences.THREAD_SCROLL_EVENT, "");
                    }

                } catch (Throwable e) {
                    Preferences.evaluateException(threads, Preferences.EXCEPTION, e);
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
                    String publicKey = ipfs.getRawPublicKey();
                    byte[] data = THREADS.getImage(context, pid.getPid(), R.drawable.server_network);

                    CID image = ipfs.add(data, "", true);

                    user = threads.createUser(pid, publicKey,
                            getDeviceName(), UserType.VERIFIED, image);

                    threads.storeUser(user);
                    threads.blockUserByPID(pid);
                }
            } catch (Throwable e) {
                Preferences.evaluateException(threads, Preferences.EXCEPTION, e);
            }
        }
    }

    static void deleteThreads(@NonNull Context context, @NonNull List<Long> idxs) {
        checkNotNull(context);
        checkNotNull(idxs);
        final THREADS threadsAPI = Singleton.getInstance(context).getThreads();

        final IPFS ipfs = Singleton.getInstance(context).getIpfs();
        if (ipfs != null) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {

                try {

                    for (long idx : idxs) {
                        threadsAPI.setThreadStatus(idx, ThreadStatus.DELETING);
                    }

                    for (long idx : idxs) {
                        deleteThread(context, ipfs, idx);
                    }
                    threadsAPI.repo_gc(ipfs);

                } catch (Throwable e) {
                    Preferences.evaluateException(threadsAPI, Preferences.EXCEPTION, e);
                }
            });
        }
    }

    static void deleteThread(@NonNull Context context, @NonNull IPFS ipfs, long idx) {
        checkNotNull(ipfs);
        final THREADS threadsAPI = Singleton.getInstance(context).getThreads();
        try {
            Thread thread = threadsAPI.getThreadByIdx(idx);
            if (thread != null) {
                threadsAPI.removeThread(ipfs, thread);
            }
        } catch (Throwable e) {
            Preferences.evaluateException(threadsAPI, Preferences.EXCEPTION, e);
        }
    }

    static void downloadMultihash(@NonNull Context context,
                                  @NonNull PID sender,
                                  @NonNull String multihash,
                                  @Nullable String filename,
                                  @Nullable String filesize) {

        checkNotNull(context);
        checkNotNull(sender);
        checkNotNull(multihash);

        final THREADS threads = Singleton.getInstance(context).getThreads();

        final PID pid = Preferences.getPID(context);
        checkNotNull(pid);

        final IPFS ipfs = Singleton.getInstance(context).getIpfs();
        if (ipfs != null) {

            DOWNLOAD_SERVICE.submit(() -> {
                try {
                    // check if multihash is valid
                    try {
                        Multihash.fromBase58(multihash);
                    } catch (Throwable e) {
                        Preferences.error(threads, context.getString(R.string.multihash_not_valid));
                        return;
                    }

                    User user = threads.getUserByPID(sender);
                    if (user == null) {
                        Preferences.error(threads, context.getString(R.string.unknown_peer_sends_data));
                        return;
                    }


                    CID cid = CID.create(multihash);
                    List<Thread> entries = threads.getThreadsByCID(cid);

                    if (!entries.isEmpty()) {
                        for (Thread entry : entries) {
                            if (entry.getThread() == 0L) {
                                if (entry.getStatus() == ThreadStatus.DELETING ||
                                        entry.getStatus() == ThreadStatus.ONLINE ||
                                        entry.getStatus() == ThreadStatus.PUBLISHING) {
                                    replySender(ipfs, sender, entry);
                                    return;
                                } else {
                                    downloadMultihash(context, threads, ipfs, entry, sender);
                                    return;
                                }
                            }
                        }

                    }
                    long idx = createThread(
                            context, ipfs, sender, cid, filename, filesize, 0L);

                    Preferences.event(threads, Preferences.THREAD_SCROLL_EVENT, "");
                    Thread thread = threads.getThreadByIdx(idx);
                    checkNotNull(thread);
                    downloadMultihash(context, threads, ipfs, thread, sender);
                    Preferences.event(threads, Preferences.THREAD_SCROLL_EVENT, "");

                    // Now check if MIME TYPE of thread can be re-evaluated
                    if (threads.getMimeType(thread).equals(Preferences.OCTET_MIME_TYPE)) {
                        ContentInfo contentInfo = ipfs.getContentInfo(cid, "");
                        if (contentInfo != null) {
                            threads.setMimeType(thread, contentInfo.getMimeType());
                            // THIS can be wrong with the filename
                            threads.setAdditional(thread, Content.FILENAME,
                                    cid.getCid() + "." + contentInfo.getName(), true);
                        }
                    }


                } catch (Throwable e) {
                    Preferences.evaluateException(threads, Preferences.EXCEPTION, e);
                }
            });
        }
    }

    @NonNull
    private static String evaluateMimeType(@NonNull Context context, @NonNull String filename) {
        final THREADS threads = Singleton.getInstance(context).getThreads();
        try {
            Optional<String> extension = THREADS.getExtension(filename);
            if (extension.isPresent()) {
                String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.get());
                if (mimeType != null) {
                    return mimeType;
                }
            }
        } catch (Throwable e) {
            Preferences.evaluateException(threads, Preferences.EXCEPTION, e);
        }
        return Preferences.OCTET_MIME_TYPE;
    }

    private static long createThread(@NonNull Context context,
                                     @NonNull IPFS ipfs,
                                     @NonNull PID creator,
                                     @NonNull CID cid,
                                     @Nullable String filename,
                                     @Nullable String filesize,
                                     long parent) {

        checkNotNull(context);
        checkNotNull(ipfs);
        checkNotNull(creator);
        checkNotNull(cid);


        final THREADS threads = Singleton.getInstance(context).getThreads();

        User user = threads.getUserByPID(creator);
        checkNotNull(user);

        Thread thread = threads.createThread(user, ThreadStatus.OFFLINE, Kind.OUT,
                "", cid, parent, false);
        thread.addAdditional(Preferences.THREAD_KIND, ThreadKind.LEAF.name(), false); // not known yet
        if (filename != null) {
            thread.addAdditional(Content.FILENAME, filename, false);
            thread.setMimeType(evaluateMimeType(context, filename));
        } else {
            thread.setMimeType(Preferences.OCTET_MIME_TYPE); // not known yet
            thread.addAdditional(Content.FILENAME, cid.getCid(), false);
        }
        if (filesize != null) {
            thread.addAdditional(Content.FILESIZE, filesize, false);
        } else {
            thread.addAdditional(Content.FILESIZE, "-1", false);
        }


        try {
            byte[] image = THREADS.getImage(context.getApplicationContext(),
                    user.getAlias(), R.drawable.file_document);
            thread.setImage(ipfs.add(image, "", true));
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
                                    false, true,
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

        threads.setAdditional(thread, Preferences.THREAD_KIND, ThreadKind.LEAF.name(), true);


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
            threads.setStatus(thread, ThreadStatus.LEACHING); // make sure
            int timeout = Preferences.getConnectionTimeout(context);
            success = threads.receive(ipfs, cid, "",
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

            if (success) {
                try {
                    File file = ipfs.get(cid, "");
                    if (!file.isDirectory()) {
                        byte[] image = THREADS.getPreviewImage(context, cid, "");
                        if (image != null) {
                            threads.setImage(ipfs, thread, image);
                        }
                    } else {
                        threads.setAdditional(thread,
                                Preferences.THREAD_KIND, ThreadKind.NODE.name(), true);
                        threads.setMimeType(thread, DocumentsContract.Document.MIME_TYPE_DIR);

                    }
                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
                }
            } else {
                try {
                    CID image = THREADS.createResourceImage(
                            context, threads, ipfs, R.drawable.file_document, "");
                    checkNotNull(image);
                    threads.setImage(thread, image);
                } catch (Throwable e) {
                    Preferences.evaluateException(threads, Preferences.EXCEPTION, e);
                }
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


    private static boolean handleContentLink(@NonNull Context context,
                                             @NonNull THREADS threads,
                                             @NonNull IPFS ipfs,
                                             @NonNull Thread thread,
                                             @NonNull LinkInfo link) {

        String filename = link.getName();
        String filesize = String.valueOf(link.getSize());

        threads.setAdditional(thread, Content.FILENAME, filename, false);
        threads.setAdditional(thread, Content.FILESIZE, filesize, false);
        threads.setMimeType(thread, evaluateMimeType(context, filename));
        threads.setAdditional(thread, Preferences.THREAD_KIND, ThreadKind.LEAF.name(), true);


        return download(context, threads, ipfs, thread, link.getCid(), link.getName(), link.getSize());

    }

    private static boolean downloadLink(@NonNull Context context,
                                        @NonNull THREADS threads,
                                        @NonNull IPFS ipfs,
                                        @NonNull Thread thread,
                                        @NonNull LinkInfo link) {
        if (link.isDirectory()) {
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

        AtomicInteger successCounter = new AtomicInteger(0);
        for (LinkInfo link : links) {


            CID cid = link.getCid();

            List<Thread> entries = threads.getThreadsByCID(cid);
            if (!entries.isEmpty()) {
                for (Thread entry : entries) {
                    if (entry.getThread() == thread.getThread()) {
                        if (entry.getStatus() != ThreadStatus.ONLINE) {

                            boolean success = downloadLink(context, threads, ipfs, entry, link);
                            if (success) {
                                successCounter.incrementAndGet();
                            }
                            break;
                        } else {
                            successCounter.incrementAndGet();
                            break;
                        }
                    }
                }
            }
            // TODO rethink creation of thread (TODO add directory info)
            long idx = createThread(context, ipfs,
                    thread.getSenderPid(), cid, link.getName(),
                    String.valueOf(link.getSize()), thread.getIdx());
            Thread entry = threads.getThreadByIdx(idx);
            checkNotNull(entry);
            boolean success = downloadLink(context, threads, ipfs, entry, link);

            if (success) {
                successCounter.incrementAndGet();
                threads.setStatus(entry, ThreadStatus.ONLINE);
            } else {
                threads.setStatus(entry, ThreadStatus.ERROR);
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

        CID cid = thread.getCid();
        checkNotNull(cid);

        List<LinkInfo> links = getLinks(context, ipfs, cid);

        if (links != null) {
            if (links.isEmpty()) {

                boolean result = downloadThread(context, threads, ipfs, thread);
                if (result) {
                    threads.setStatus(thread, ThreadStatus.ONLINE);
                    if (sender != null) {
                        replySender(ipfs, sender, thread);
                    }
                } else {
                    threads.setStatus(thread, ThreadStatus.ERROR);
                }

            } else {

                // thread is directory
                threads.setMimeType(thread, DocumentsContract.Document.MIME_TYPE_DIR);
                threads.setAdditional(thread, Preferences.THREAD_KIND,
                        ThreadKind.NODE.name(), true);

                try {
                    CID image = THREADS.createResourceImage(context, threads, ipfs,
                            R.drawable.folder_outline, "");
                    if (image != null) {
                        threads.setImage(thread, image);
                    }
                } catch (Throwable e) {
                    Preferences.evaluateException(threads, Preferences.EXCEPTION, e);
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
            PID hostPID = Preferences.getPID(context);
            final THREADS threads = Singleton.getInstance(context).getThreads();


            final IPFS ipfs = Singleton.getInstance(context).getIpfs();
            if (ipfs != null) {

                List<User> users = threads.getUsers();

                if (hostPID != null) {
                    User host = threads.getUserByPID(hostPID);
                    users.remove(host);
                }

                for (User user : users) {
                    UserStatus currentStatus = user.getStatus();
                    if (currentStatus != UserStatus.BLOCKED &&
                            currentStatus != UserStatus.DIALING) {
                        try {
                            boolean value = ipfs.isConnected(user.getPID());

                            currentStatus = user.getStatus();
                            if (currentStatus != UserStatus.BLOCKED &&
                                    currentStatus != UserStatus.DIALING) {
                                if (value) {
                                    if (currentStatus != UserStatus.ONLINE) {
                                        threads.setStatus(user, UserStatus.ONLINE);
                                    }
                                } else {
                                    if (currentStatus != UserStatus.OFFLINE) {
                                        threads.setStatus(user, UserStatus.OFFLINE);
                                    }
                                }
                            }
                        } catch (Throwable e) {
                            if (threads.getStatus(user) != UserStatus.DIALING) {
                                threads.setStatus(user, UserStatus.OFFLINE);
                            }
                        }
                    }
                }
            }

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    void downloadThread(@NonNull Context context, @NonNull Thread thread) {

        checkNotNull(context);
        checkNotNull(thread);

        final THREADS threads = Singleton.getInstance(context).getThreads();
        final IPFS ipfs = Singleton.getInstance(context).getIpfs();
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
        final THREADS threads = Singleton.getInstance(context).getThreads();
        final IPFS ipfs = Singleton.getInstance(context).getIpfs();
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
                        String filename = threadObject.getAdditional(Content.FILENAME);
                        String filesize = threadObject.getAdditional(Content.FILESIZE);
                        map.put(Content.FILENAME, filename);
                        map.put(Content.FILESIZE, filesize);
                        map.put(Content.CID, cid.getCid());
                        Log.e(TAG, "Send : " + map.toString());
                        ipfs.pubsubPub(user.getPID().getPid(), gson.toJson(map), 50);
                    }

                    success = true;
                }
            } catch (Throwable e) {
                Preferences.evaluateException(threads, Preferences.EXCEPTION, e);
            }
        }
        return success;
    }

    void sendThreads(@NonNull Context context,
                     @NonNull List<Long> idxs) {
        checkNotNull(context);
        checkNotNull(idxs);

        final PID host = Preferences.getPID(context.getApplicationContext());

        final THREADS threads = Singleton.getInstance(context).getThreads();
        final IPFS ipfs = Singleton.getInstance(context).getIpfs();

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
                        Preferences.warning(threads, context.getString(R.string.no_peers_connected));
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

                        Preferences.warning(threads,
                                context.getString(R.string.data_shared_with_peers,
                                        String.valueOf(counter)));


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

                    ipfs.daemon(Preferences.isPubsubEnabled(context));

                } catch (Throwable e) {
                    Preferences.evaluateException(threads, Preferences.IPFS_START_FAILURE, e);
                }

                if (ipfs.isDaemonRunning()) {
                    new java.lang.Thread(() -> startPubsub(context)).start();
                }

                new java.lang.Thread(() -> RelayService.publishPeer(context)).start();

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
