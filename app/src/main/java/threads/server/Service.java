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
import android.webkit.MimeTypeMap;

import com.google.common.io.Files;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import threads.core.Preferences;
import threads.core.Singleton;
import threads.core.THREADS;
import threads.core.api.Kind;
import threads.core.api.Thread;
import threads.core.api.ThreadStatus;
import threads.core.api.User;
import threads.core.api.UserStatus;
import threads.core.api.UserType;
import threads.ipfs.IPFS;
import threads.ipfs.Network;
import threads.ipfs.api.Base58;
import threads.ipfs.api.CID;
import threads.ipfs.api.Link;
import threads.ipfs.api.PID;
import threads.share.ConnectService;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;


class Service {

    private static final ExecutorService UPLOAD_SERVICE = Executors.newFixedThreadPool(5);
    private static final ExecutorService DOWNLOAD_SERVICE = Executors.newFixedThreadPool(5);

    static void connectPeers(@NonNull Context context) {
        checkNotNull(context);

        final THREADS threads = Singleton.getInstance().getThreads();
        if (Network.isConnected(context)) {

            final IPFS ipfs = Singleton.getInstance().getIpfs();
            if (ipfs != null) {
                List<User> users = threads.getUsers();
                if (DaemonService.DAEMON_RUNNING.get()) {
                    for (User user : users) {
                        if (user.getStatus() != UserStatus.BLOCKED &&
                                user.getStatus() != UserStatus.DIALING) {
                            UserStatus oldStatus = user.getStatus();
                            try {

                                if (ipfs.swarm_peer(user.getPID()) != null) {

                                    if (UserStatus.ONLINE != oldStatus) {
                                        threads.setStatus(user, UserStatus.ONLINE);

                                    }
                                } else {
                                    if (UserStatus.OFFLINE != oldStatus) {
                                        threads.setStatus(user, UserStatus.OFFLINE);
                                    }

                                    if (Application.isAutoConnected(context)) {

                                        threads.setStatus(user, UserStatus.DIALING);


                                        boolean value = ConnectService.connect(ipfs,
                                                user.getPID(), Application.CON_TIME_OUT);
                                        if (value) {
                                            threads.setStatus(user, UserStatus.ONLINE);
                                        } else {
                                            threads.setStatus(user, UserStatus.OFFLINE);
                                        }
                                    }

                                }
                            } catch (Throwable e) {
                                if (UserStatus.OFFLINE != oldStatus) {
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
                            pid.getPid(), "", null, false);

                    thread.addAdditional(Application.TITLE, fileDetails.getFileName(), false);
                    thread.addAdditional(Application.THREAD_KIND, ThreadKind.LEAF.name(), true);
                    CID image = ipfs.add(bytes, true);
                    thread.setImage(image);
                    thread.setMimeType(fileDetails.getMimeType());
                    long idx = threadsAPI.storeThread(thread);


                    thread = threadsAPI.getThreadByIdx(idx); // TODO optimize here
                    checkNotNull(thread);
                    try {
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
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        }
        return capitalize(manufacturer) + " " + model;
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

    static void createHost(@NonNull Context context, @NonNull IPFS ipfs) throws Exception {
        checkNotNull(context);
        checkNotNull(ipfs);
        PID pid = Preferences.getPID(context);
        checkNotNull(pid);
        THREADS threads = Singleton.getInstance().getThreads();

        User user = threads.getUserByPID(pid);
        if (user == null) {
            String publicKey = ipfs.getPublicKey();
            byte[] data = THREADS.getImage(context,
                    pid.getPid(), R.drawable.server_network);

            CID image = ipfs.add(data, true);
            user = threads.createUser(pid, publicKey,
                    getDeviceName(), UserType.VERIFIED, image, null);
            user.setStatus(UserStatus.BLOCKED);
            threads.storeUser(user);
        }

    }


    private static boolean shareUser(@NonNull User user, @NonNull Long... idxs) {
        final THREADS threads = Singleton.getInstance().getThreads();
        final IPFS ipfs = Singleton.getInstance().getIpfs();
        boolean success = false;
        if (ipfs != null) {
            try {
                if (ConnectService.connect(ipfs, user.getPID(), Application.CON_TIME_OUT)) {

                    for (long idx : idxs) {
                        Thread threadObject = threads.getThreadByIdx(idx);
                        checkNotNull(threadObject);

                        CID cid = threadObject.getCid();
                        checkNotNull(cid);

                        ipfs.pubsub_pub(user.getPID().getPid(),
                                cid.getCid().concat(System.lineSeparator()));
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
                            @NonNull ShareThreads listener,
                            @NonNull Long... idxs) {
        checkNotNull(context);
        checkNotNull(listener);

        final PID host = Preferences.getPID(context.getApplicationContext());
        final THREADS threads = Singleton.getInstance().getThreads();
        final IPFS ipfs = Singleton.getInstance().getIpfs();

        if (ipfs != null) {

            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {

                    List<User> users = threads.getUsers();
                    if (users.isEmpty()) {
                        Preferences.warning(context.getString(R.string.no_peers_connected));
                    } else {

                        ExecutorService sharedExecutor = Executors.newFixedThreadPool(5);

                        LinkedList<Future<Boolean>> futures = new LinkedList<>();
                        for (User user : users) {
                            if (user.getStatus() != UserStatus.BLOCKED) {
                                PID userPID = user.getPID();
                                if (!userPID.equals(host)) {

                                    Future<Boolean> future = sharedExecutor.submit(() ->
                                            shareUser(user, idxs));
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
                } finally {
                    listener.done();
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
        final THREADS threadsAPI = Singleton.getInstance().getThreads();
        Thread thread = threadsAPI.getThreadByIdx(idx);
        if (thread != null) {
            deleteThread(ipfs, thread);
        }
    }

    private static void deleteThread(@NonNull IPFS ipfs, @NonNull Thread thread) {
        checkNotNull(ipfs);
        checkNotNull(thread);
        final THREADS threadsAPI = Singleton.getInstance().getThreads();

        String threadKind = thread.getAdditional(Application.THREAD_KIND);
        checkNotNull(threadKind);
        Service.ThreadKind kind = Service.ThreadKind.valueOf(threadKind);
        threadsAPI.setStatus(thread, ThreadStatus.DELETING);

        if (kind == Service.ThreadKind.NODE) {

            CID cid = thread.getCid();
            if (cid != null) {
                List<Thread> entries = threadsAPI.getThreadsByAddress(cid.getCid());
                for (Thread entry : entries) {
                    deleteThread(ipfs, entry);
                }
            }
        }

        threadsAPI.removeThread(ipfs, thread);

    }

    static void downloadMultihash(@NonNull Context context,
                                  @NonNull PID creator,
                                  @NonNull String multihash) {

        checkNotNull(context);
        checkNotNull(creator);
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
                        Base58.decode(multihash);
                    } catch (Throwable e) {
                        Preferences.error(context.getString(R.string.multihash_not_valid));
                        return;
                    }

                    // check if thread exists with multihash
                    CID cid = CID.create(multihash);
                    List<Thread> entries = threads.getThreadsByCID(cid);
                    if (!entries.isEmpty()) {
                        for (Thread entry : entries) {
                            if (entry.getStatus() != ThreadStatus.ONLINE) {
                                downloadMultihash(context, threads, ipfs, entry);
                            } else {
                                // UPDATE UI
                                Preferences.event(Preferences.THREAD_SELECT_EVENT,
                                        String.valueOf(entry.getIdx()));
                            }
                        }

                    } else {
                        long idx = createThread(context, ipfs, creator, cid, pid.getPid());
                        Thread thread = threads.getThreadByIdx(idx);
                        checkNotNull(thread);
                        downloadMultihash(context, threads, ipfs, thread);

                    }

                } catch (Throwable e) {
                    Preferences.evaluateException(Preferences.EXCEPTION, e);
                }
            });
        }
    }

    private static long createThread(@NonNull Context context,
                                     @NonNull IPFS ipfs,
                                     @NonNull PID creator,
                                     @NonNull CID cid,
                                     @NonNull String address) {

        checkNotNull(context);
        checkNotNull(ipfs);
        checkNotNull(creator);
        checkNotNull(cid);
        checkNotNull(address);


        final THREADS threads = Singleton.getInstance().getThreads();


        User user = threads.getUserByPID(creator);
        checkNotNull(user);

        Thread thread = threads.createThread(user, ThreadStatus.OFFLINE, Kind.OUT,
                address, "", cid, false);

        try {
            byte[] image = THREADS.getImage(context.getApplicationContext(),
                    user.getAlias(), R.drawable.file_document);
            thread.setImage(ipfs.add(image, true));
        } catch (Throwable e) {
            Preferences.evaluateException(Preferences.EXCEPTION, e);
        }
        thread.setMimeType(Application.OCTET_MIME_TYPE); // not known yet
        return threads.storeThread(thread);
    }

    static void localDownloadThread(@NonNull Context context, long idx) {
        checkNotNull(context);

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

                    List<Link> links = threadsAPI.getLinks(ipfs, threadObject,
                            Application.CON_TIME_OUT, true);

                    if (links.size() == 1) {
                        Link link = links.get(0);
                        cid = link.getCid();
                    }
                    String title = threadObject.getAdditional(Application.TITLE);

                    File dir = Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DOWNLOADS);
                    File file = new File(dir, title);


                    NotificationCompat.Builder builder =
                            NotificationSender.createDownloadProgressNotification(
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
                                        return false;
                                    }
                                }, Application.MAX_CON_TIME_OUT);

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
    }

    static void downloadThread(@NonNull Context context, @NonNull Thread thread) {

        checkNotNull(context);

        final THREADS threadsAPI = Singleton.getInstance().getThreads();

        final IPFS ipfs = Singleton.getInstance().getIpfs();
        if (ipfs != null) {
            DOWNLOAD_SERVICE.submit(() -> {
                try {

                    downloadMultihash(context, threadsAPI, ipfs, thread);

                } catch (Throwable e) {
                    Preferences.evaluateException(Preferences.EXCEPTION, e);
                }
            });
        }
    }

    private static void downloadThread(@NonNull Context context,
                                       @NonNull THREADS threads,
                                       @NonNull IPFS ipfs,
                                       @NonNull Thread thread) {
        // UPDATE UI
        Preferences.event(Preferences.THREAD_SELECT_EVENT, String.valueOf(thread.getIdx()));


        CID cid = thread.getCid();
        checkNotNull(cid);

        String title = thread.getAdditional(Application.TITLE);


        NotificationCompat.Builder builder =
                NotificationSender.createDownloadProgressNotification(
                        context, title);

        final NotificationManager notificationManager = (NotificationManager)
                context.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        int notifyID = cid.getCid().hashCode();
        Notification notification = builder.build();
        if (notificationManager != null) {
            notificationManager.notify(notifyID, notification);
        }


        File file = getCacheFile(context, cid.getCid());

        boolean success = true;
        try {

            success = threads.download(ipfs, file, cid, true, false, thread.getSesKey(), new THREADS.Progress() {
                @Override
                public void setProgress(int percent) {
                    builder.setProgress(100, percent, false);
                    if (notificationManager != null) {
                        notificationManager.notify(notifyID, builder.build());
                    }
                }

                @Override
                public boolean isStopped() {
                    return false;
                }
            }, Application.MAX_CON_TIME_OUT);

            try {
                byte[] image = THREADS.getPreviewImage(context, file);
                if (image != null) {
                    threads.setImage(ipfs, thread, image);
                }
            } catch (Throwable e) {
                // no exception will be reported
            }

        } catch (Throwable e) {
            success = false;
            threads.setStatus(thread, ThreadStatus.ERROR);
            Preferences.evaluateException(Preferences.EXCEPTION, e);
        } finally {
            file.delete();
            if (notificationManager != null) {
                notificationManager.cancel(notifyID);
            }
        }

        if (success) {
            NotificationSender.showNotification(context, cid.getCid(), cid.hashCode());
        }

    }

    private static void setTitle(@NonNull THREADS threads, @NonNull Thread thread, @NonNull String title) {
        thread.addAdditional(Application.TITLE, title, false);
        threads.updateThread(thread);
    }

    private static boolean handleDirectoryLink(@NonNull Context context,
                                               @NonNull THREADS threads,
                                               @NonNull IPFS ipfs,
                                               @NonNull Thread thread,
                                               @NonNull Link link) {

        String filename = link.getPath();
        setTitle(threads, thread, filename.substring(0, filename.length() - 1));// remove "/"
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


        List<Link> links = getLinks(ipfs, link, Application.CON_TIME_OUT, false);

        return downloadLinks(context, threads, ipfs, thread, links);

    }

    public static List<Link> getLinks(@NonNull IPFS ipfs,
                                      @NonNull Link link,
                                      long timeout,
                                      boolean offline) {
        checkNotNull(ipfs);
        checkNotNull(link);
        checkArgument(timeout > 0);
        CID cid = link.getCid();
        List<Link> links = new ArrayList<>();
        try {
            links.addAll(ipfs.ls(cid, timeout, offline));
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
        setTitle(threads, thread, filename);


        String extension = Files.getFileExtension(filename);
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        if (mimeType != null) {
            threads.setMimeType(thread, mimeType);
        } else {
            threads.setMimeType(thread, Application.OCTET_MIME_TYPE); // not know what type
        }
        threads.setAdditional(thread, Application.THREAD_KIND, ThreadKind.LEAF.name(), true);

        Long size = link.getSize();
        checkNotNull(size);

        NotificationCompat.Builder builder =
                NotificationSender.createDownloadProgressNotification(
                        context, link.getPath());

        final NotificationManager notificationManager = (NotificationManager)
                context.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        int notifyID = link.getCid().hashCode();
        Notification notification = builder.build();
        if (notificationManager != null) {
            notificationManager.notify(notifyID, notification);
        }

        File file = getCacheFile(context, System.currentTimeMillis() + filename);

        boolean success;
        try {

            success = threads.download(ipfs, file,
                    link.getCid(), true, false, thread.getSesKey(), new THREADS.Progress() {
                        @Override
                        public void setProgress(int percent) {
                            builder.setProgress(100, percent, false);
                            if (notificationManager != null) {
                                notificationManager.notify(notifyID, builder.build());
                            }
                        }

                        @Override
                        public boolean isStopped() {
                            return false;
                        }
                    }, Application.MAX_CON_TIME_OUT);


            if (success) {
                try {
                    byte[] image = THREADS.getPreviewImage(context, file);
                    if (image != null) {
                        threads.setImage(ipfs, thread, image);
                    }
                } catch (Throwable e) {
                    // no exception will be reported
                }
            }

        } catch (Throwable e) {
            success = false;
        } finally {
            file.delete();
            if (notificationManager != null) {
                notificationManager.cancel(notifyID);
            }
        }

        if (success) {
            NotificationSender.showNotification(context,
                    link.getPath(), link.getCid().hashCode());
        }

        return success;
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
                CID threadCid = thread.getCid();
                checkNotNull(threadCid);

                long idx = createThread(context, ipfs, thread.getSenderPid(), cid, threadCid.getCid());
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
                                          @NonNull Thread thread) {
        checkNotNull(context);
        checkNotNull(threads);
        checkNotNull(ipfs);
        checkNotNull(thread);


        CID cid = thread.getCid();
        checkNotNull(cid);
        threads.setStatus(thread, ThreadStatus.OFFLINE);

        List<Link> links = threads.getLinks(ipfs, thread, Application.CON_TIME_OUT, false);

        if (links.isEmpty()) {
            setTitle(threads, thread, cid.getCid());
            threads.setAdditional(thread, Application.THREAD_KIND, ThreadKind.LEAF.name(), true);
            threads.setMimeType(thread, Application.OCTET_MIME_TYPE);

            try {
                CID image = THREADS.createResourceImage(
                        context, threads, ipfs, "", R.drawable.file_document);
                threads.setImage(thread, image);
            } catch (Throwable e) {
                Preferences.evaluateException(Preferences.EXCEPTION, e);
            }
            downloadThread(context, threads, ipfs, thread);

        } else if (links.size() > 1) {

            // real directory
            setTitle(threads, thread, cid.getCid());
            threads.setMimeType(thread, Application.OCTET_MIME_TYPE);
            threads.setAdditional(thread, Application.THREAD_KIND, ThreadKind.NODE.name(), true);
            try {
                CID image = THREADS.createResourceImage(
                        context, threads, ipfs, "", R.drawable.file_document);
                threads.setImage(thread, image);
            } catch (Throwable e) {
                Preferences.evaluateException(Preferences.EXCEPTION, e);
            }
            boolean result = downloadLinks(context, threads, ipfs, thread, links);
            if (result) {
                threads.setStatus(thread, ThreadStatus.ONLINE);
            } else {
                threads.setStatus(thread, ThreadStatus.ERROR);
            }
        } else {

            Link link = links.get(0);

            boolean result = downloadLink(context, threads, ipfs, thread, link);
            if (result) {
                threads.setStatus(thread, ThreadStatus.ONLINE);
            } else {
                threads.setStatus(thread, ThreadStatus.ERROR);
            }
        }


    }


    @NonNull
    public static File getCacheFile(@NonNull Context context, @NonNull String name) {
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

    public static void closeTasks(@NonNull Context context) {
        checkNotNull(context);

        final THREADS threads = Singleton.getInstance().getThreads();


        List<Thread> entries = threads.getThreadsByKindAndThreadStatus(
                Kind.OUT, ThreadStatus.OFFLINE);
        for (Thread entry : entries) {
            threads.setStatus(entry, ThreadStatus.ERROR);
        }
    }

    public enum ThreadKind {
        LEAF, NODE
    }

    public interface ShareThreads {
        void done();
    }
}
