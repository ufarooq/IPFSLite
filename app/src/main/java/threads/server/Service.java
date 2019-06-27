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
import com.google.gson.JsonSyntaxException;
import com.j256.simplemagic.ContentInfo;

import org.apache.commons.lang3.StringUtils;
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
import threads.share.PeerService;
import threads.share.RTCSession;

import static androidx.core.util.Preconditions.checkNotNull;


public class Service {
    static final String OFFER = "OFFER";
    static final String PROVIDE = "PROVIDE";
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
    static Service getInstance(@NonNull Context context) {
        checkNotNull(context);
        if (SINGLETON == null) {

            runUpdatesIfNecessary(context);


            ProgressChannel.createProgressChannel(context);
            RTCSession.createRTCChannel(context);

            Singleton singleton = Singleton.getInstance(context);
            singleton.setAesKey(() -> "");
            singleton.setNotificationServer(NotificationServer.getInstance());

            SINGLETON = new Service();
            SINGLETON.startDaemon(context);
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
                sender.setType(UserType.VERIFIED);

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

                    Preferences.error(threadsAPI, context.getString(R.string.user_connect_try, alias));
                }

                PID host = Preferences.getPID(context);
                checkNotNull(host);
                User hostUser = threadsAPI.getUserByPID(host);
                checkNotNull(hostUser);
                Content map = new Content();
                map.put(Content.EST, "CONNECT_REPLY");
                map.put(Content.ALIAS, hostUser.getAlias());
                map.put(Content.PKEY, hostUser.getPublicKey());

                ipfs.pubsubPub(senderPid.getPid(), gson.toJson(map), 50);


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


    static void downloadMultihash(@NonNull Context context,
                                  @NonNull PID sender,
                                  @NonNull String multihash) {

        checkNotNull(context);
        checkNotNull(sender);
        checkNotNull(multihash);

        final THREADS threads = Singleton.getInstance(context).getThreads();

        final PID pid = Preferences.getPID(context);
        checkNotNull(pid);
        final int timeout = Preferences.getConnectionTimeout(context);
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

                    byte[] content = ipfs.get(cid, "", timeout, false);
                    try {
                        ContentFiles files = gson.fromJson(new String(content), ContentFiles.class);
                        checkNotNull(files);
                        downloadFiles(context, sender, files);
                    } catch (JsonSyntaxException jse) {
                        downloadMultihash(context, sender, multihash, null, null);
                    }


                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
                }
            });
        }
    }

    private static void downloadFiles(@NonNull Context context,
                                      @NonNull PID sender,
                                      @NonNull ContentFiles files) {
        for (ContentFile file : files) {
            downloadMultihash(context, sender, file.getCid(), file.getFilename(), file.getSize());
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
                    long idx = createThread(context, ipfs, sender, cid, filename, filesize);

                    Preferences.event(threads, Preferences.THREAD_SCROLL_EVENT, "");
                    Thread thread = threads.getThreadByIdx(idx);
                    checkNotNull(thread);
                    downloadMultihash(context, threads, ipfs, thread, sender);
                    Preferences.event(threads, Preferences.THREAD_SCROLL_EVENT, "");


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
                "", cid, parent, false);

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
                CID image = THREADS.createResourceImage(context, threads, ipfs,
                        R.drawable.folder_outline, "");
                if (image != null) {
                    thread.setImage(image);
                }
            } catch (Throwable e) {
                Preferences.evaluateException(threads, Preferences.EXCEPTION, e);
            }
        } else {
            try {
                byte[] image = THREADS.getImage(context.getApplicationContext(),
                        user.getAlias(), R.drawable.file_document);
                thread.setImage(ipfs.add(image, "", true));
            } catch (Throwable e) {
                Preferences.evaluateException(threads, Preferences.EXCEPTION, e);
            }
        }
        return threads.storeThread(thread);
    }

    private static long createThread(@NonNull Context context,
                                     @NonNull IPFS ipfs,
                                     @NonNull PID creator,
                                     @NonNull CID cid,
                                     @Nullable String filename,
                                     @Nullable String filesize) {

        checkNotNull(context);
        checkNotNull(ipfs);
        checkNotNull(creator);
        checkNotNull(cid);


        final THREADS threads = Singleton.getInstance(context).getThreads();

        User user = threads.getUserByPID(creator);
        checkNotNull(user);

        Thread thread = threads.createThread(user, ThreadStatus.OFFLINE, Kind.OUT,
                "", cid, 0L, false);
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
                if (threads.getMimeType(thread).equals(Preferences.OCTET_MIME_TYPE)) {
                    ContentInfo contentInfo = ipfs.getContentInfo(file);
                    if (contentInfo != null) {
                        String mimeType = contentInfo.getMimeType();
                        if (mimeType != null) {
                            threads.setMimeType(thread, mimeType);
                        }
                    }
                }

                try {
                    byte[] image = THREADS.getPreviewImage(context, file, filename);
                    if (image != null) {
                        threads.setImage(ipfs, thread, image);
                    }
                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
                }
            } else {
                success = false;
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
                    checkNotNull(image);
                    threads.setImage(thread, image);
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

                    // TODO when text is a link (html etc)
                    String name = StringUtils.substring(text, 0, 20);
                    long size = text.length();

                    Thread thread = threads.createThread(host, ThreadStatus.OFFLINE, Kind.IN,
                            "", null, 0L, false);
                    thread.addAdditional(Content.FILENAME, name, false);
                    thread.addAdditional(Preferences.THREAD_KIND, ThreadKind.LEAF.name(), false);
                    thread.addAdditional(Content.FILESIZE, String.valueOf(size), false);

                    byte[] bytes = THREADS.getImage(context, host.getAlias(),
                            R.drawable.file_document);
                    CID image = ipfs.add(bytes, "", true);
                    thread.setImage(image);
                    thread.setMimeType(Preferences.PLAIN_MIME_TYPE);
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


    ArrayList<String> getEnhancedUserPIDs(@NonNull Context context) {
        checkNotNull(context);

        final THREADS threads = Singleton.getInstance(context).getThreads();
        final PID pid = Preferences.getPID(context);
        ArrayList<String> users = new ArrayList<>();
        checkNotNull(pid);
        for (User user : threads.getUsers()) {

            if (!user.getPID().equals(pid)) {
                if (user.getStatus() != UserStatus.BLOCKED) {
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

                if (!ConnectService.connectUserTimeout(context, sender, pubsubCheck)) {
                    Hashtable<String, String> params = new Hashtable<>();
                    CID cid = thread.getCid();
                    checkNotNull(cid);
                    params.put(Content.CID, cid.getCid());
                    params.put(Content.EST, Service.PROVIDE);
                    ConnectService.notifyUser(context, thread.getSenderPid(), params, pubsubCheck);
                }

            }

            Service.downloadMultihash(context, threads, ipfs, thread, null);
        }

    }

    private void shareUser(@NonNull Context context, @NonNull User user, long[] idxs) {
        checkNotNull(user);
        checkNotNull(idxs);
        final THREADS threads = Singleton.getInstance(context).getThreads();
        final IPFS ipfs = Singleton.getInstance(context).getIpfs();

        if (ipfs != null) {
            try {
                final boolean pubsubEnabled = Preferences.isPubsubEnabled(context);

                final boolean pubsubCheck = pubsubEnabled && !
                        user.getPublicKey().isEmpty();


                ContentFiles files = new ContentFiles();
                for (long idx : idxs) {
                    Thread threadObject = threads.getThreadByIdx(idx);
                    checkNotNull(threadObject);
                    files.add(threadObject);
                }

                CID cid = ipfs.add(gson.toJson(files), "", true);
                checkNotNull(cid);


                if (ConnectService.connectUserTimeout(context, user.getPID(), pubsubCheck)) {
                    ipfs.pubsubPub(user.getPID().getPid(), cid.getCid(), 50);
                } else {
                    Hashtable<String, String> params = new Hashtable<>();
                    params.put(Content.CID, cid.getCid());
                    params.put(Content.EST, Service.OFFER);
                    ConnectService.notifyUser(context, user.getPID(), params, pubsubCheck);
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
                    // clean-up
                    for (long idx : idxs) {
                        threads.resetUnreadNotesNumber(idx);
                    }

                    if (users.isEmpty()) {
                        Preferences.error(threads, context.getString(R.string.no_sharing_peers));
                    } else {
                        checkNotNull(host);

                        ExecutorService sharedExecutor = Executors.newFixedThreadPool(5);
                        LinkedList<Future> futures = new LinkedList<>();
                        for (User user : users) {
                            if (user.getStatus() != UserStatus.BLOCKED) {
                                PID userPID = user.getPID();
                                if (!userPID.equals(host)) {

                                    Future future = sharedExecutor.submit(() ->
                                            shareUser(context, user, idxs));
                                    futures.add(future);
                                }
                            }
                        }
                        int counter = 0;
                        for (Future future : futures) {
                            future.get();
                            counter++;
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

                    ipfs.daemon((message) -> {

                        try {

                            String sender = message.getSenderPid();
                            PID senderPid = PID.create(sender);


                            if (!threads.isAccountBlocked(senderPid)) {

                                String code = message.getMessage().trim();

                                CodecDecider result = CodecDecider.evaluate(code);

                                if (result.getCodex() == CodecDecider.Codec.MULTIHASH) {
                                    Service.downloadMultihash(context, senderPid,
                                            result.getMultihash());
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
                                                adaptUser(context, senderPid, alias, pubKey);
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

                    }, Preferences.isPubsubEnabled(context), false);

                } catch (Throwable e) {
                    Preferences.evaluateException(threads, Preferences.IPFS_START_FAILURE, e);
                }

                new java.lang.Thread(() -> PeerService.publishPeer(context)).start();

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
