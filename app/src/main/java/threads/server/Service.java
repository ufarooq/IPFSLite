package threads.server;

import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.google.common.io.Files;

import java.io.File;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
import threads.ipfs.IPFS;
import threads.ipfs.api.CID;
import threads.ipfs.api.Link;
import threads.ipfs.api.PID;

import static com.google.common.base.Preconditions.checkNotNull;


class Service {

    private static final String TAG = Service.class.getSimpleName();
    private static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(5);


    public static void storeData(@NonNull Context context, @NonNull Uri uri) {
        checkNotNull(context);
        checkNotNull(uri);
        Cursor returnCursor = context.getContentResolver().query(
                uri, null, null, null, null);

        checkNotNull(returnCursor);
        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        returnCursor.moveToFirst();

        final String filename = returnCursor.getString(nameIndex);
        returnCursor.close();
        String mimeType = context.getContentResolver().getType(uri);
        checkNotNull(mimeType);

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
                            filename, filename, bytes, false, false);
                    thread.setMimeType(mimeType);
                    threadsAPI.storeThread(thread);


                    try {
                        CID cid = ipfs.add(inputStream, filename, true);
                        checkNotNull(cid);

                        // cleanup of entries with same CID
                        List<Thread> sameEntries = threadsAPI.getThreadsByCid(cid);
                        for (Thread entry : sameEntries) {
                            threadsAPI.removeThread(entry);
                        }


                        threadsAPI.setStatus(thread, ThreadStatus.ONLINE);
                        threadsAPI.setCID(thread, cid);

                    } catch (Throwable e) {
                        threadsAPI.setStatus(thread, ThreadStatus.ERROR);
                        throw e;
                    }

                } catch (Throwable e) {
                    Preferences.evaluateException(Preferences.EXCEPTION, e);
                }
            });
        }
    }


    static void shareThreads(@NonNull Context context, @NonNull ShareThreads listener, @NonNull String... threadAddresses) {
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
                    AtomicInteger counter = new AtomicInteger(0);
                    for (User user : users) {
                        if (user.getStatus() != UserStatus.BLOCKED) {
                            PID userPID = PID.create(user.getPid());
                            if (!userPID.equals(host)) {
                                if (threads.connect(ipfs, userPID, null)) {
                                    counter.incrementAndGet();

                                    for (String thread : threadAddresses) {


                                        Thread threadObject = threads.getThreadByAddress(thread);
                                        checkNotNull(threadObject);


                                        String multihash = threadObject.getCid();


                                        ipfs.pubsub_pub(user.getPid(),
                                                multihash.concat(System.lineSeparator()));
                                    }
                                }
                            }
                        }
                    }

                    if (users.isEmpty()) {
                        Preferences.warning(context.getString(R.string.no_peers_connected));
                    } else {
                        Preferences.warning(context.getString(R.string.data_shared_with_peers,
                                String.valueOf(counter.get())));
                    }

                } catch (Throwable e) {
                    Preferences.evaluateException(Preferences.EXCEPTION, e);
                } finally {
                    listener.done();
                }
            });


        }
    }

    static void deleteThreads(@NonNull Context context, @NonNull String... addresses) {
        checkNotNull(context);
        checkNotNull(addresses);

        final THREADS threadsAPI = Singleton.getInstance().getThreads();

        final IPFS ipfs = Singleton.getInstance().getIpfs();
        if (ipfs != null) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {

                try {

                    for (String address : addresses) {
                        Thread thread = threadsAPI.getThreadByAddress(address);
                        checkNotNull(thread);
                        String cid = thread.getCid();

                        threadsAPI.removeThread(thread);

                        try {
                            threadsAPI.pin_rm(ipfs, cid, false);
                        } catch (Throwable e) {
                            // for now ignore exception
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

                    List<Thread> entries = threads.getThreadsByCid(CID.create(multihash));
                    if (!entries.isEmpty()) {
                        for (Thread entry : entries) {
                            if (entry.getStatus() != ThreadStatus.ONLINE) {
                                downloadMultihash(context, threads, ipfs, entry);
                            } else {
                                threads.setDate(entry, new Date());
                            }
                        }
                    } else {
                        User user = threads.getUserByPID(creator);
                        checkNotNull(user);


                        byte[] image = THREADS.getImage(context.getApplicationContext(),
                                user.getAlias(), R.drawable.file_document);

                        Thread thread = threads.createThread(user, ThreadStatus.OFFLINE, Kind.OUT,
                                "", multihash, image, false, false);
                        thread.setMimeType("");
                        threads.storeThread(thread);

                        downloadMultihash(context, threads, ipfs, thread);
                    }


                } catch (Throwable e) {
                    Preferences.evaluateException(Preferences.EXCEPTION, e);
                }
            });
        }
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

                    String cid = threadObject.getCid();
                    List<Link> links = threadsAPI.getLinks(ipfs, threadObject, 20);
                    Link link = links.get(0);
                    String path = link.getPath();

                    Uri uri = Uri.parse(Preferences.getGateway(context) + cid + "/" + path);

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

    private static void downloadMultihash(@NonNull Context context,
                                          @NonNull THREADS threads,
                                          @NonNull IPFS ipfs,
                                          @NonNull Thread thread) throws Exception {
        checkNotNull(context);
        checkNotNull(threads);
        checkNotNull(ipfs);
        checkNotNull(thread);

        String multihash = thread.getCid();

        List<Link> links = threads.getLinks(ipfs, thread, 20);

        if (links.isEmpty()) {
            threads.setStatus(thread, ThreadStatus.ERROR);
            return;
        }

        if (links.size() > 1) {
            threads.setStatus(thread, ThreadStatus.ERROR);
            Preferences.warning(context.getString(R.string.sorry_not_yet_implemented));
            return;
        }
        Link link = links.get(0);

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

        File file = getCacheFile(context, multihash + filename);

        try {

            boolean success = threads.store(ipfs, file,
                    link.getCid().getCid(), link.getSize(), (percent) -> {
                        builder.setProgress(100, percent, false);
                        if (notificationManager != null) {
                            notificationManager.notify(notifyID, builder.build());
                        }

                    });


            if (success) {

                threads.pin_add(ipfs, multihash); // pin the content so that it is not deleted

                try {
                    byte[] image = THREADS.getPreviewImage(context, file);
                    if (image != null) {
                        threads.setImage(thread, image);
                    }
                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
                } finally {
                    threads.setStatus(thread, ThreadStatus.ONLINE);
                }

            } else {
                threads.setStatus(thread, ThreadStatus.ERROR);
            }

        } catch (Throwable e) {
            threads.setStatus(thread, ThreadStatus.ERROR);
            throw e;
        } finally {
            file.delete();
            if (notificationManager != null) {
                notificationManager.cancel(notifyID);
            }
        }


        NotificationSender.showLinkNotification(context.getApplicationContext(), link);
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
