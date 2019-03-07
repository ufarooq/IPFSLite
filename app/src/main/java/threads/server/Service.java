package threads.server;

import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import com.google.common.io.Files;

import java.io.File;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import io.ipfs.multihash.Multihash;
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
import threads.ipfs.api.CID;
import threads.ipfs.api.Link;
import threads.ipfs.api.PID;

import static com.google.common.base.Preconditions.checkNotNull;


class Service {

    private static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(5);


    static void connectRelay(@NonNull IPFS ipfs, @NonNull PID relay) {
        checkNotNull(ipfs);
        checkNotNull(relay);
        try {
            if (!ipfs.swarm_is_connected(relay)) {
                ipfs.swarm_connect(relay, Application.CON_TIMEOUT);
            }
        } catch (Throwable e) {
            // ignore exception occurs when daemon is shutdown
        }
    }

    static void connectPeers(@NonNull Context context) {
        checkNotNull(context);

        final THREADS threads = Singleton.getInstance().getThreads();
        if (Network.isConnected(context)) {

            final IPFS ipfs = Singleton.getInstance().getIpfs();
            if (ipfs != null) {
                List<User> users = threads.getUsers();
                for (User user : users) {
                    if (user.getStatus() != UserStatus.BLOCKED &&
                            user.getStatus() != UserStatus.DIALING) {
                        UserStatus oldStatus = user.getStatus();
                        try {

                            if (ipfs.swarm_is_connected(user.getPID())) {

                                if (UserStatus.ONLINE != oldStatus) {
                                    threads.setStatus(user, UserStatus.ONLINE);

                                }
                            } else {
                                if (UserStatus.OFFLINE != oldStatus) {
                                    threads.setStatus(user, UserStatus.OFFLINE);
                                }

                                if (Application.isAutoConnected(context)) {

                                    threads.setStatus(user, UserStatus.DIALING);


                                    boolean value = threads.connect(ipfs, user.getPID(),
                                            Preferences.getRelay(context), Application.CON_TIMEOUT);
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

            EXECUTOR_SERVICE.submit(() -> {
                try {
                    InputStream inputStream =
                            context.getContentResolver().openInputStream(uri);
                    checkNotNull(inputStream);

                    PID pid = Preferences.getPID(context);
                    checkNotNull(pid);
                    User user = threadsAPI.getUserByPID(pid);
                    checkNotNull(user);


                    byte[] bytes;
                    try {
                        bytes = THREADS.getPreviewImage(context, uri);
                        if (bytes == null) {
                            bytes = THREADS.getImage(context, user.getAlias(),
                                    R.drawable.file_document);
                        }
                    } catch (Throwable e) {
                        // ignore exception
                        bytes = THREADS.getImage(context, user.getAlias(),
                                R.drawable.file_document);
                    }


                    Thread thread = threadsAPI.createThread(user, ThreadStatus.OFFLINE, Kind.IN,
                            fileDetails.getFileName(), null, false, false);

                    CID image = ipfs.add(bytes, true);
                    thread.setImage(image);
                    thread.setMimeType(fileDetails.getMimeType());
                    threadsAPI.storeThread(thread);


                    try {
                        CID cid = ipfs.add(inputStream, fileDetails.getFileName(), true, false);
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
                        Preferences.event(Preferences.THREAD_SELECT_EVENT, thread.getAddress());
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

            String inbox = Preferences.getInbox(context);
            checkNotNull(inbox);
            String publicKey = ipfs.getPublicKey();
            byte[] data = THREADS.getImage(context,
                    pid.getPid(), R.drawable.server_network);

            CID image = ipfs.add(data, true);
            user = threads.createUser(pid, inbox, publicKey,
                    getDeviceName(), UserType.VERIFIED, image, null);
            user.setStatus(UserStatus.BLOCKED);
            threads.storeUser(user);
        }

    }

    static void createRelay(@NonNull Context context, @NonNull IPFS ipfs) throws Exception {
        checkNotNull(context);
        checkNotNull(ipfs);
        PID relay = PID.create("QmchgNzyUFyf2wpfDMmpGxMKHA3PkC1f3H2wUgbs21vXoh");


        THREADS threads = Singleton.getInstance().getThreads();
        User user = threads.getUserByPID(relay);
        if (user == null) {
            byte[] data = THREADS.getImage(context,
                    relay.getPid(), R.drawable.server_network);
            CID image = ipfs.add(data, true);
            user = threads.createUser(relay,
                    relay.getPid(),
                    relay.getPid(),
                    context.getString(R.string.relay), UserType.VERIFIED, image, null);
            user.setStatus(UserStatus.OFFLINE);
            threads.storeUser(user);
        }
    }

    private static boolean shareUser(@NonNull Context context,
                                     @NonNull User user,
                                     @NonNull String... threadAddresses) {
        final THREADS threads = Singleton.getInstance().getThreads();
        final IPFS ipfs = Singleton.getInstance().getIpfs();
        boolean success = false;
        if (ipfs != null) {
            try {
                if (threads.connect(ipfs, user.getPID(),
                        Preferences.getRelay(context), Application.CON_TIMEOUT)) {

                    for (String thread : threadAddresses) {


                        Thread threadObject = threads.getThreadByAddress(thread);
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

    static void shareThreads(@NonNull Context context,
                             @NonNull ShareThreads listener,
                             @NonNull String... threadAddresses) {
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
                                            shareUser(context, user, threadAddresses));
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

    static void deleteThreads(@NonNull String... addresses) {
        checkNotNull(addresses);

        final THREADS threadsAPI = Singleton.getInstance().getThreads();

        final IPFS ipfs = Singleton.getInstance().getIpfs();
        if (ipfs != null) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {

                try {

                    for (String address : addresses) {
                        Thread thread = threadsAPI.getThreadByAddress(address);
                        if (thread != null) {
                            threadsAPI.removeThread(ipfs, thread);
                        }
                    }
                    threadsAPI.repo_gc(ipfs);

                } catch (Throwable e) {
                    Preferences.evaluateException(Preferences.EXCEPTION, e);
                }
            });
        }
    }


    static void downloadMultihash(@NonNull Context context,
                                  @NonNull PID creator,
                                  @NonNull String multihash) {

        checkNotNull(context);
        checkNotNull(creator);
        checkNotNull(multihash);

        final THREADS threads = Singleton.getInstance().getThreads();

        final IPFS ipfs = Singleton.getInstance().getIpfs();
        if (ipfs != null) {

            EXECUTOR_SERVICE.submit(() -> {
                try {
                    // check if multihash is valid
                    try {
                        Multihash.fromBase58(multihash);
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
                                        entry.getAddress());
                            }
                        }

                    } else {
                        Thread thread = createThread(context, ipfs, creator, cid);
                        downloadMultihash(context, threads, ipfs, thread);

                    }

                } catch (Throwable e) {
                    Preferences.evaluateException(Preferences.EXCEPTION, e);
                }
            });
        }
    }

    private static Thread createThread(@NonNull Context context,
                                       @NonNull IPFS ipfs,
                                       @NonNull PID creator,
                                       @NonNull CID cid) {
        final THREADS threads = Singleton.getInstance().getThreads();
        User user = threads.getUserByPID(creator);
        checkNotNull(user);


        Thread thread = threads.createThread(user, ThreadStatus.OFFLINE, Kind.OUT,
                "", cid, false, false);
        try {
            byte[] image = THREADS.getImage(context.getApplicationContext(),
                    user.getAlias(), R.drawable.file_document);
            thread.setImage(ipfs.add(image, true));
        } catch (Throwable e) {
            Preferences.evaluateException(Preferences.EXCEPTION, e);
        }

        thread.setMimeType("");
        threads.storeThread(thread);
        return thread;
    }

    static void localDownloadThread(@NonNull Context context, @NonNull String thread) {
        checkNotNull(context);
        checkNotNull(thread);
        final THREADS threadsAPI = Singleton.getInstance().getThreads();
        final DownloadManager downloadManager = (DownloadManager)
                context.getSystemService(Context.DOWNLOAD_SERVICE);
        checkNotNull(downloadManager);

        final IPFS ipfs = Singleton.getInstance().getIpfs();

        if (ipfs != null) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    Thread threadObject = threadsAPI.getThreadByAddress(thread);
                    checkNotNull(threadObject);

                    CID cid = threadObject.getCid();
                    checkNotNull(cid);

                    List<Link> links = threadsAPI.getLinks(ipfs, threadObject,
                            Application.CON_TIMEOUT, true);


                    Uri uri;
                    String path;
                    if (links.isEmpty() || links.size() > 1) {
                        path = cid.getCid(); // TODO get real name
                        uri = Uri.parse(Preferences.getGateway(context) + cid.getCid());
                    } else {
                        Link link = links.get(0);
                        path = link.getPath();
                        uri = Uri.parse(Preferences.getGateway(context) +
                                cid.getCid() + "/" + path);
                    }
                    DownloadManager.Request request = new DownloadManager.Request(uri);


                    request.setTitle(path);

                    request.setNotificationVisibility(
                            DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    request.setDestinationInExternalPublicDir(
                            Environment.DIRECTORY_DOWNLOADS, path);

                    downloadManager.enqueue(request);

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
            EXECUTOR_SERVICE.submit(() -> {
                try {

                    downloadMultihash(context, threadsAPI, ipfs, thread);

                } catch (Throwable e) {
                    Preferences.evaluateException(Preferences.EXCEPTION, e);
                }
            });
        }
    }


    private static void downloadCID(@NonNull Context context,
                                    @NonNull THREADS threads,
                                    @NonNull IPFS ipfs,
                                    @NonNull Thread thread) {
        // UPDATE UI
        Preferences.event(Preferences.THREAD_SELECT_EVENT, thread.getAddress());


        CID cid = thread.getCid();
        checkNotNull(cid);


        File file = getCacheFile(context, cid.getCid());

        boolean success = true;
        try {

            threads.store(ipfs, file, cid, true, false);

            try {
                byte[] image = THREADS.getPreviewImage(context, file);
                if (image != null) {
                    threads.setImage(ipfs, thread, image);
                }
            } catch (Throwable e) {
                // no exception will be reported
            } finally {
                threads.setStatus(thread, ThreadStatus.ONLINE);
            }


        } catch (Throwable e) {
            success = false;
            threads.setStatus(thread, ThreadStatus.ERROR);
            Preferences.evaluateException(Preferences.EXCEPTION, e);
        } finally {
            file.delete();
        }

        if (success) {
            NotificationSender.showNotification(context.getApplicationContext(), cid.getCid());
        }

    }


    private static boolean downloadLink(@NonNull Context context,
                                        @NonNull THREADS threads,
                                        @NonNull IPFS ipfs,
                                        @NonNull Thread thread,
                                        @NonNull Link link) {
        // UPDATE UI
        Preferences.event(Preferences.THREAD_SELECT_EVENT, thread.getAddress());

        String filename = link.getPath();
        if (thread.getTitle().isEmpty()) {
            threads.setTitle(thread, filename);
        }

        if (thread.getMimeType().isEmpty()) {
            String extension = Files.getFileExtension(filename);
            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            if (mimeType != null) {
                threads.setMimeType(thread, mimeType);
            }
        }


        NotificationCompat.Builder builder =
                NotificationSender.createDownloadProgressNotification(
                        context.getApplicationContext(), link.getPath());

        final NotificationManager notificationManager = (NotificationManager)
                context.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        int notifyID = NotificationSender.NOTIFICATIONS_COUNTER.incrementAndGet();
        Notification notification = builder.build();
        if (notificationManager != null) {
            notificationManager.notify(notifyID, notification);
        }

        File file = getCacheFile(context, System.currentTimeMillis() + filename);

        boolean success;
        try {

            success = threads.store(ipfs, file,
                    link.getCid(), link.getSize(), true, false, (percent) -> {
                        builder.setProgress(100, percent, false);
                        if (notificationManager != null) {
                            notificationManager.notify(notifyID, builder.build());
                        }

                    });


            if (success) {
                try {
                    byte[] image = THREADS.getPreviewImage(context, file);
                    if (image != null) {
                        threads.setImage(ipfs, thread, image);
                    }
                } catch (Throwable e) {
                    // no exception will be reported
                } finally {
                    threads.setStatus(thread, ThreadStatus.ONLINE);
                }

            } else {
                threads.setStatus(thread, ThreadStatus.ERROR);
            }

        } catch (Throwable e) {
            success = false;
            threads.setStatus(thread, ThreadStatus.ERROR);
        } finally {
            file.delete();
            if (notificationManager != null) {
                notificationManager.cancel(notifyID);
            }
        }

        if (success) {
            NotificationSender.showNotification(context.getApplicationContext(), link.getPath());
        }

        return success;
    }

    private static void downloadLinks(@NonNull Context context,
                                      @NonNull THREADS threads,
                                      @NonNull IPFS ipfs,
                                      @NonNull Thread thread,
                                      @NonNull List<Link> links) {
        // UPDATE UI
        Preferences.event(Preferences.THREAD_SELECT_EVENT, thread.getAddress());


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
                                entry.getAddress());
                        success = true;
                    }
                }
            } else {
                Thread entry = createThread(context, ipfs, thread.getSenderPid(), cid);
                success = downloadLink(context, threads, ipfs, entry, link);

            }

            if (success) {
                successCounter.incrementAndGet();
            }
        }
        if (successCounter.get() == links.size()) {
            threads.setStatus(thread, ThreadStatus.ONLINE);
        } else {
            threads.setStatus(thread, ThreadStatus.ERROR);
        }
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

        List<Link> links = threads.getLinks(ipfs, thread, Application.CON_TIMEOUT, false);

        if (links.isEmpty()) {
            threads.setTitle(thread, cid.getCid());
            threads.setMimeType(thread, Application.QR_CODE_MIME_TYPE);
            try {
                CID image = THREADS.createResourceImage(context, ipfs, R.drawable.qrcode);
                threads.setImage(thread, image);
            } catch (Throwable e) {
                Preferences.evaluateException(Preferences.EXCEPTION, e);
            }
            downloadCID(context, threads, ipfs, thread);

        } else if (links.size() > 1) {

            // real directory
            threads.setTitle(thread, cid.getCid());
            threads.setMimeType(thread, Application.QR_CODE_MIME_TYPE);

            try {
                CID image = THREADS.createResourceImage(context, ipfs, R.drawable.qrcode);
                threads.setImage(thread, image);
            } catch (Throwable e) {
                Preferences.evaluateException(Preferences.EXCEPTION, e);
            }
            downloadLinks(context, threads, ipfs, thread, links);
        } else {

            Link link = links.get(0);

            downloadLink(context, threads, ipfs, thread, link);
        }


    }

    @NonNull
    private static File getCacheFile(@NonNull Context context, @NonNull String name) {
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

    public interface ShareThreads {
        void done();
    }
}
