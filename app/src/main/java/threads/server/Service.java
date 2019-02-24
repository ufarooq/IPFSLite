package threads.server;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import io.ipfs.multihash.Multihash;
import threads.core.IThreadsAPI;
import threads.core.Preferences;
import threads.core.Singleton;
import threads.core.api.Kind;
import threads.core.api.Thread;
import threads.core.api.ThreadStatus;
import threads.core.api.User;
import threads.ipfs.IPFS;
import threads.ipfs.api.CID;
import threads.ipfs.api.Link;
import threads.ipfs.api.PID;

import static com.google.common.base.Preconditions.checkNotNull;


class Service {

    static void deleteThreads(@NonNull Context context, @NonNull String... addresses) {
        checkNotNull(context);
        checkNotNull(addresses);

        final IThreadsAPI threadsAPI = Singleton.getInstance().getThreadsAPI();

        final IPFS ipfs = Singleton.getInstance().getIpfs();
        if (ipfs != null) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {

                try {
                    List<Thread> threads = new ArrayList<>();
                    for (String address : addresses) {
                        Thread thread = threadsAPI.getThreadByAddress(address);
                        checkNotNull(thread);
                        thread.setStatus(ThreadStatus.DELETING);
                        threadsAPI.updateThread(thread);
                        threads.add(thread);

                        //TODO threadsAPI.pin_rm(ipfs, thread.getCid(), false);
                    }

                    threadsAPI.removeThreads(threads);

                    //TODO threadsAPI.repo_gc(ipfs);

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

        final IThreadsAPI threadsAPI = Singleton.getInstance().getThreadsAPI();

        final IPFS ipfs = Singleton.getInstance().getIpfs();
        if (ipfs != null) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    // check if multihash is valid
                    try {
                        Multihash.fromBase58(multihash);
                    } catch (Throwable e) {
                        Preferences.error(context.getString(R.string.multihash_not_valid));
                        return;
                    }

                    User user = threadsAPI.getUserByPID(creator);
                    checkNotNull(user);


                    CID cid = CID.create(multihash);
                    List<Link> links = ipfs.ls(cid);
                    if (links.isEmpty() || links.size() > 1) {
                        Preferences.warning(context.getString(R.string.sorry_not_yet_implemented));
                        return;
                    }
                    Link link = links.get(0);

                    String filename = link.getPath();


                    byte[] image = IThreadsAPI.getImage(context.getApplicationContext(),
                            user.getAlias(), R.drawable.file_document);

                    Thread thread = threadsAPI.createThread(user, ThreadStatus.OFFLINE, Kind.OUT,
                            filename, multihash, image, false, false);
                    threadsAPI.storeThread(thread);

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


                    try {
                        boolean success = threadsAPI.preload(ipfs,
                                link.getCid().getCid(), link.getSize(), (percent) -> {


                                    builder.setProgress(100, percent, false);
                                    if (notificationManager != null) {
                                        notificationManager.notify(notifyID, builder.build());
                                    }

                                });


                        if (success) {
                            threadsAPI.setStatus(thread, ThreadStatus.ONLINE);
                        } else {
                            threadsAPI.setStatus(thread, ThreadStatus.ERROR);
                        }

                    } catch (Throwable e) {
                        threadsAPI.setStatus(thread, ThreadStatus.ERROR);
                        throw e;
                    } finally {

                        if (notificationManager != null) {
                            notificationManager.cancel(notifyID);
                        }
                    }

                    NotificationSender.showLinkNotification(context.getApplicationContext(), link);

                } catch (Throwable e) {
                    Preferences.evaluateException(Preferences.EXCEPTION, e);
                }
            });
        }
    }


    static void downloadThread(@NonNull Context context, @NonNull Thread thread) {

        checkNotNull(context);

        final IThreadsAPI threadsAPI = Singleton.getInstance().getThreadsAPI();

        final IPFS ipfs = Singleton.getInstance().getIpfs();
        if (ipfs != null) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    String multihash = thread.getCid();
                    CID cid = CID.create(multihash);
                    List<Link> links = ipfs.ls(cid);
                    if (links.isEmpty() || links.size() > 1) {
                        Preferences.warning(context.getString(R.string.sorry_not_yet_implemented));
                        return;
                    }
                    Link link = links.get(0);

                    threadsAPI.setStatus(thread, ThreadStatus.OFFLINE);


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


                    try {
                        boolean success = threadsAPI.preload(ipfs,
                                link.getCid().getCid(), link.getSize(), (percent) -> {


                                    builder.setProgress(100, percent, false);
                                    if (notificationManager != null) {
                                        notificationManager.notify(notifyID, builder.build());
                                    }

                                });


                        if (success) {
                            threadsAPI.setStatus(thread, ThreadStatus.ONLINE);
                        } else {
                            threadsAPI.setStatus(thread, ThreadStatus.ERROR);
                        }

                    } catch (Throwable e) {
                        threadsAPI.setStatus(thread, ThreadStatus.ERROR);
                        throw e;
                    } finally {

                        if (notificationManager != null) {
                            notificationManager.cancel(notifyID);
                        }
                    }

                    NotificationSender.showLinkNotification(context.getApplicationContext(), link);

                } catch (Throwable e) {
                    Preferences.evaluateException(Preferences.EXCEPTION, e);
                }
            });
        }
    }
}
