package threads.server;

import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.google.gson.Gson;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
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
import threads.ipfs.IPFS;
import threads.ipfs.Network;
import threads.ipfs.api.CID;
import threads.ipfs.api.Link;
import threads.ipfs.api.Multihash;
import threads.ipfs.api.PID;
import threads.share.ConnectService;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;


class Service {
    private static final String TAG = Service.class.getSimpleName();
    private static final Gson gson = new Gson();
    private static final ExecutorService UPLOAD_SERVICE = Executors.newFixedThreadPool(3);
    private static final ExecutorService DOWNLOAD_SERVICE = Executors.newFixedThreadPool(1);

    private static void startPeers(@NonNull Context context) {
        try {
            while (Preferences.isDaemonRunning(context)) {
                threads.server.Service.checkPeers(context);
                if (Network.isConnected(context)) {
                    java.lang.Thread.sleep(1000);
                } else {
                    java.lang.Thread.sleep(30000);
                }
            }
        } catch (Throwable e) {
            // IGNORE exception occurs when daemon is shutdown
        } finally {
            threads.server.Service.checkPeers(context);
        }
    }


    private static void startPubsub(@NonNull Context context) {
        checkNotNull(context);
        final IPFS ipfs = Singleton.getInstance().getIpfs();
        if (ipfs != null) {
            if (Preferences.isDaemonRunning(context)) {
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

    private static void pubsubDaemon(@NonNull Context context,
                                     @NonNull IPFS ipfs,
                                     @NonNull PID pid) throws Exception {
        checkNotNull(context);
        checkNotNull(ipfs);
        checkNotNull(pid);

        final THREADS threadsAPI = Singleton.getInstance().getThreads();


        if (Preferences.DEBUG_MODE) {
            Log.e(TAG, "Pubsub Daemon :" + pid.getPid());
        }

        ipfs.pubsub_sub(pid.getPid(), false, (message) -> {

            try {


                PID senderPid = PID.create(message.getSenderPid());


                if (!threadsAPI.isAccountBlocked(senderPid)) {

                    String code = message.getMessage().trim();

                    CodecDecider result = CodecDecider.evaluate(code);

                    if (result.getCodex() == CodecDecider.Codec.MULTIHASH) {
                        Service.downloadMultihash(context, senderPid, result.getMultihash(), null);
                    } else if (result.getCodex() == CodecDecider.Codec.URI) {
                        Service.downloadMultihash(context, senderPid, result.getMultihash(), null);
                    } else if (result.getCodex() == CodecDecider.Codec.JSON_MAP) {
                        Map<String, String> map = result.getMap();

                        if (map.containsKey(Content.EST)) {
                            String est = map.get(Content.EST);
                            Message type = Message.valueOf(est);
                            switch (type) {
                                case CONNECT: {
                                    if (map.containsKey(Content.ALIAS)) {
                                        String alias = map.get(Content.ALIAS);
                                        checkNotNull(alias);
                                        createUser(context, senderPid, alias);
                                    }
                                    break;
                                }
                                case SHARE: {
                                    if (map.containsKey(Content.CID)) {
                                        String cid = map.get(Content.CID);
                                        checkNotNull(cid);
                                        String title = map.get(Content.TITLE);
                                        Service.downloadMultihash(context, senderPid, cid, title);
                                    }
                                    break;
                                }
                                case REPLY: {
                                    if (map.containsKey(Content.CID)) {
                                        String cid = map.get(Content.CID);
                                        checkNotNull(cid);
                                        Service.publishReply(context, senderPid, cid);
                                    }
                                }
                            }
                        } else {
                            if (map.containsKey(Content.ALIAS)) {
                                String alias = map.get(Content.ALIAS);
                                checkNotNull(alias);
                                createUser(context, senderPid, alias);
                            } else if (map.containsKey(Content.CID)) {
                                String cid = map.get(Content.CID);
                                checkNotNull(cid);
                                String title = map.get(Content.TITLE);
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

                        createUser(context, senderPid, name);
                    }

                }
            } catch (Throwable e) {
                if (Preferences.DEBUG_MODE) {
                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
                }
            } finally {
                if (Preferences.DEBUG_MODE) {
                    Log.e(TAG, "Received : " + message.toString());
                }
            }


        });

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
                                   @NonNull String alias) {
        checkNotNull(context);
        checkNotNull(senderPid);
        checkNotNull(alias);

        try {
            final THREADS threadsAPI = Singleton.getInstance().getThreads();
            final IPFS ipfs = Singleton.getInstance().getIpfs();
            if (ipfs != null) {
                User sender = threadsAPI.getUserByPID(senderPid);
                if (sender == null) {

                    // create a new user which is blocked (User has to unblock and verified the user)
                    byte[] data = THREADS.getImage(context, alias, R.drawable.server_network);
                    CID image = ipfs.add(data, true, false);

                    sender = threadsAPI.createUser(senderPid,
                            senderPid.getPid(), // TODO public key
                            alias,
                            UserType.UNKNOWN,
                            image);
                    sender.setStatus(UserStatus.BLOCKED);
                    threadsAPI.storeUser(sender);


                    Preferences.error(context.getString(R.string.user_connect_try, alias));
                }
            }
        } catch (Throwable e) {
            // ignore exception
        }
    }

    static void startDaemon(@NonNull Context context) {
        checkNotNull(context);
        try {
            final IPFS ipfs = Singleton.getInstance().getIpfs();
            if (ipfs != null) {

                try {
                    ipfs.daemon(Preferences.isPubsubEnabled(context));
                    Preferences.setDaemonRunning(context, true);
                } catch (Throwable e) {
                    Preferences.setDaemonRunning(context, false);
                    Preferences.evaluateException(Preferences.IPFS_START_FAILURE, e);
                }

                if (Preferences.isDaemonRunning(context)) {
                    new java.lang.Thread(() -> startPubsub(context)).start();
                }

                if (Preferences.isDaemonRunning(context)) {
                    new java.lang.Thread(() -> startPeers(context)).start();
                }

                if (ConnectService.isAutoConnectRelay(context)) {
                    new java.lang.Thread(() -> ConnectService.connectRelays(context, 10000)).start();
                }
            }
        } catch (Throwable e) {
            Preferences.evaluateException(Preferences.EXCEPTION, e);
        }
    }

    static void checkPeers(@NonNull Context context) {
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
                                    boolean value = ipfs.swarm_peer(user.getPID()) != null;
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
                            "", false, null, 0L);
                    thread.addAdditional(Content.TITLE, fileDetails.getFileName(), false);
                    thread.addAdditional(Application.THREAD_KIND, ThreadKind.LEAF.name(), false);
                    CID image = ipfs.add(bytes, true, false);
                    thread.setImage(image);
                    thread.setMimeType(fileDetails.getMimeType());
                    long idx = threadsAPI.storeThread(thread);


                    thread = threadsAPI.getThreadByIdx(idx); // TODO optimize here
                    checkNotNull(thread);
                    try {

                        threadsAPI.setStatus(thread, ThreadStatus.LEACHING);

                        CID cid = ipfs.add(inputStream, fileDetails.getFileName(),
                                true, true, false);
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

    static void cleanStates(@NonNull Context context) {
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

    static void createHost(@NonNull Context context) {
        checkNotNull(context);

        final THREADS threads = Singleton.getInstance().getThreads();
        final IPFS ipfs = Singleton.getInstance().getIpfs();
        if (ipfs != null) {
            try {
                PID pid = Preferences.getPID(context);
                checkNotNull(pid);

                User user = threads.getUserByPID(pid);
                if (user == null) {
                    String publicKey = ipfs.getPublicKey();
                    byte[] data = THREADS.getImage(context, pid.getPid(), R.drawable.server_network);

                    CID image = ipfs.add(data, true, false);

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


    private static boolean shareUser(@NonNull User user, int timeout,
                                     @NonNull Long... idxs) {
        checkNotNull(user);
        final THREADS threads = Singleton.getInstance().getThreads();
        final IPFS ipfs = Singleton.getInstance().getIpfs();
        boolean success = false;
        if (ipfs != null) {
            try {
                if (ConnectService.connectUser(user.getPID(), timeout)) {

                    for (long idx : idxs) {
                        Thread threadObject = threads.getThreadByIdx(idx);
                        checkNotNull(threadObject);

                        CID cid = threadObject.getCid();
                        checkNotNull(cid);

                        Map<String, String> map = new HashMap<>();
                        map.put(Content.EST, Message.SHARE.name());
                        String title = threadObject.getAdditional(Content.TITLE);
                        if (!title.isEmpty()) {
                            map.put(Content.TITLE, title);
                        }
                        map.put(Content.CID, cid.getCid());
                        ipfs.pubsub_pub(user.getPID().getPid(), gson.toJson(map));
                    }

                    success = true;
                }
            } catch (Throwable e) {
                Preferences.evaluateException(Preferences.EXCEPTION, e);
            }
        }
        return success;
    }

    static void sendThreads(@NonNull Context context,
                            @NonNull Long... idxs) {
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
                        ExecutorService sharedExecutor = Executors.newFixedThreadPool(5);
                        int timeout = ConnectService.getConnectionTimeout(context);
                        LinkedList<Future<Boolean>> futures = new LinkedList<>();
                        for (User user : users) {
                            if (user.getStatus() != UserStatus.BLOCKED) {
                                PID userPID = user.getPID();
                                if (!userPID.equals(host)) {

                                    Future<Boolean> future = sharedExecutor.submit(() ->
                                            shareUser(user, timeout, idxs));
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

    static void deleteThreads(Long... idxs) {
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
                "", false, cid, parent);
        thread.addAdditional(Application.THREAD_KIND, ThreadKind.LEAF.name(), false); // not known yet
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
            thread.setImage(ipfs.add(image, true, false));
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

                        int timeout = ConnectService.getConnectionTimeout(context);

                        List<Link> links = threadsAPI.getLinks(ipfs, threadObject,
                                timeout, true);
                        checkNotNull(links);
                        if (links.size() == 1) {
                            Link link = links.get(0);
                            cid = link.getCid();
                        }
                        String title = threadObject.getAdditional(Content.TITLE);

                        File dir = Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_DOWNLOADS);
                        File file = new File(dir, title);


                        NotificationCompat.Builder builder =
                                NotificationSender.createProgressNotification(
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
                                    }, timeout);

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

        threads.setAdditional(thread, Application.THREAD_KIND, ThreadKind.LEAF.name(), true);
        String filename = thread.getAdditional(Content.TITLE);
        return download(context, threads, ipfs, thread, cid, filename);
    }

    private static boolean download(@NonNull Context context,
                                    @NonNull THREADS threads,
                                    @NonNull IPFS ipfs,
                                    @NonNull Thread thread,
                                    @NonNull CID cid,
                                    @NonNull String filename) {

        checkNotNull(context);
        checkNotNull(threads);
        checkNotNull(ipfs);
        checkNotNull(thread);
        checkNotNull(cid);
        checkNotNull(filename);


        NotificationCompat.Builder builder =
                NotificationSender.createProgressNotification(
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
            int timeout = ConnectService.getConnectionTimeout(context);
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
                    }, timeout);

            if (success) {
                try {
                    byte[] image = THREADS.getPreviewImage(context, file);
                    if (image != null) {
                        threads.setImage(ipfs, thread, image, true, false);
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

    private static boolean handleDirectoryLink(@NonNull Context context,
                                               @NonNull THREADS threads,
                                               @NonNull IPFS ipfs,
                                               @NonNull Thread thread,
                                               @NonNull Link link) {

        String filename = link.getPath();
        threads.setAdditional(thread, Content.TITLE,
                filename.substring(0, filename.length() - 1), false);
        threads.setMimeType(thread, DocumentsContract.Document.MIME_TYPE_DIR);
        threads.setAdditional(thread, Application.THREAD_KIND, ThreadKind.NODE.name(), true);

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
                threads.pin_add(ipfs, cid);
            }
        } catch (Throwable e) {
            Preferences.evaluateException(Preferences.EXCEPTION, e);
        }

        long timeout = ConnectService.getConnectionTimeout(context);
        List<Link> links = getLinks(ipfs, link, timeout);
        if (links != null) {
            return downloadLinks(context, threads, ipfs, thread, links);
        } else {
            return false;
        }

    }

    @Nullable
    private static List<Link> getLinks(@NonNull IPFS ipfs, @NonNull Link link, long timeout) {
        checkNotNull(ipfs);
        checkNotNull(link);

        CID cid = link.getCid();
        List<Link> links = null;
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
                                             @NonNull Link link) {

        String filename = link.getPath();
        threads.setAdditional(thread, Content.TITLE, filename, false);
        evaluateMimeType(thread, filename);
        threads.setAdditional(thread, Application.THREAD_KIND, ThreadKind.LEAF.name(), true);


        return download(context, threads, ipfs, thread, link.getCid(), link.getPath());

    }

    private static boolean downloadLink(@NonNull Context context,
                                        @NonNull THREADS threads,
                                        @NonNull IPFS ipfs,
                                        @NonNull Thread thread,
                                        @NonNull Link link) {
        // UPDATE UI
        Preferences.event(Preferences.THREAD_SELECT_EVENT, String.valueOf(thread.getIdx()));

        String filename = link.getPath();


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
                                         @NonNull List<Link> links) {
        // UPDATE UI
        Preferences.event(Preferences.THREAD_SELECT_EVENT, String.valueOf(thread.getIdx()));


        AtomicInteger successCounter = new AtomicInteger(0);
        for (Link link : links) {

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

    private static void replySender(@NonNull IPFS ipfs, @NonNull PID sender, @NonNull Thread thread) {
        CID cid = thread.getCid();
        checkNotNull(cid);

        Map<String, String> map = new HashMap<>();
        map.put(Content.EST, Message.REPLY.name());
        map.put(Content.CID, cid.getCid());

        try {
            ipfs.pubsub_pub(sender.getPid(), gson.toJson(map));
        } catch (Throwable e) {
            Preferences.evaluateException(Preferences.EXCEPTION, e);
        }
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
        int timeout = ConnectService.getConnectionTimeout(context);
        List<Link> links = threads.getLinks(ipfs, thread, timeout, false);

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
                threads.setAdditional(thread, Application.THREAD_KIND, ThreadKind.NODE.name(), true);

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

                Link link = links.get(0);

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


    public enum ThreadKind {
        LEAF, NODE
    }


}
