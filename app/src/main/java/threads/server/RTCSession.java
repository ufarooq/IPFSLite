package threads.server;

import com.google.gson.Gson;

import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Preconditions;
import threads.core.Preferences;
import threads.core.Singleton;
import threads.core.api.Content;
import threads.ipfs.IPFS;
import threads.ipfs.api.PID;
import threads.server.RTCClient.ConnectionEvents;
import threads.share.ConnectService;

import static com.google.common.base.Preconditions.checkNotNull;

public class RTCSession {
    private static final Gson gson = new Gson();
    private static RTCSession INSTANCE = new RTCSession();
    @Nullable
    private Listener listener = null;

    private RTCSession() {
    }

    public static RTCSession getInstance() {
        return INSTANCE;
    }

    public void emitSessionAnswer(@NonNull PID user,
                                  @NonNull ConnectionEvents events,
                                  @NonNull SessionDescription message,
                                  long timeout) {
        checkNotNull(user);
        checkNotNull(events);
        checkNotNull(message);
        final IPFS ipfs = Singleton.getInstance().getIpfs();
        if (ipfs != null) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    boolean value = ConnectService.connectUser(user, timeout);
                    if (value) {

                        HashMap<String, String> map = new HashMap<>();
                        map.put(Content.EST, Message.SESSION_ANSWER.name());
                        map.put(Content.SDP, message.description);
                        map.put(Content.ESK, message.type.name());

                        ipfs.pubsub_pub(user.getPid(), gson.toJson(map));
                    } else {
                        events.onFailure();
                    }
                } catch (Throwable e) {
                    Preferences.evaluateException(Preferences.EXCEPTION, e);
                }
            });
        }
    }

    public void busy(@NonNull PID pid) {
        if (listener != null) {
            listener.busy(pid);
        }
    }

    public void accept(@NonNull PID pid) {
        if (listener != null) {
            listener.accept(pid);
        }
    }

    public void close(@NonNull PID pid) {
        if (listener != null) {
            listener.close(pid);
        }
    }

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    public void reject(@NonNull PID pid) {
        if (listener != null) {
            listener.reject(pid);
        }
    }

    public void offer(@NonNull PID pid, @NonNull String sdp) {
        if (listener != null) {
            listener.offer(pid, sdp);
        }
    }

    public void answer(@NonNull PID pid, @NonNull String sdp, @NonNull String type) {
        if (listener != null) {
            listener.answer(pid, sdp, type);
        }
    }

    public void candidate(@NonNull PID pid, @NonNull String sdp,
                          @NonNull String mid, @NonNull String index) {
        if (listener != null) {
            listener.candidate(pid, sdp, mid, index);
        }
    }

    public void candidate_remove(@NonNull PID pid, @NonNull String sdp,
                                 @NonNull String mid, @NonNull String index) {
        if (listener != null) {
            listener.candidate_remove(pid, sdp, mid, index);
        }
    }

    public void timeout(PID pid) {
        if (listener != null) {
            listener.timeout(pid);
        }
    }

    public void emitSessionOffer(@NonNull PID user,
                                 @NonNull ConnectionEvents events,
                                 @NonNull SessionDescription message,
                                 long timeout) {
        checkNotNull(user);
        checkNotNull(events);
        checkNotNull(message);
        final IPFS ipfs = Singleton.getInstance().getIpfs();
        if (ipfs != null) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    boolean value = ConnectService.connectUser(user, timeout);
                    if (value) {

                        HashMap<String, String> map = new HashMap<>();
                        map.put(Content.EST, Message.SESSION_OFFER.name());
                        map.put(Content.SDP, message.description);

                        ipfs.pubsub_pub(user.getPid(), gson.toJson(map));
                    } else {
                        events.onFailure();
                    }
                } catch (Throwable e) {
                    Preferences.evaluateException(Preferences.EXCEPTION, e);
                }
            });
        }
    }

    public void emitSessionCall(@NonNull PID user) {
        checkNotNull(user);
        try {
            IPFS ipfs = Singleton.getInstance().getIpfs();
            Preconditions.checkNotNull(ipfs);
            HashMap<String, String> map = new HashMap<>();
            map.put(Content.EST, Message.SESSION_CALL.name());
            ipfs.pubsub_pub(user.getPid(), gson.toJson(map));
        } catch (Throwable e) {
            Preferences.evaluateException(Preferences.EXCEPTION, e);
        }

    }


    public void emitSessionBusy(@NonNull PID user, @NonNull ConnectionEvents events, long timeout) {
        checkNotNull(user);
        checkNotNull(events);
        final IPFS ipfs = Singleton.getInstance().getIpfs();
        if (ipfs != null) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    boolean value = ConnectService.connectUser(user, timeout);
                    if (value) {
                        HashMap<String, String> map = new HashMap<>();
                        map.put(Content.EST, Message.SESSION_BUSY.name());
                        ipfs.pubsub_pub(user.getPid(), gson.toJson(map));
                    } else {
                        events.onFailure();
                    }
                } catch (Throwable e) {
                    Preferences.evaluateException(Preferences.EXCEPTION, e);
                }
            });
        }
    }

    public void emitSessionTimeout(@NonNull PID user, @NonNull ConnectionEvents events, long timeout) {
        checkNotNull(user);
        checkNotNull(events);
        final IPFS ipfs = Singleton.getInstance().getIpfs();
        if (ipfs != null) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    boolean value = ConnectService.connectUser(user, timeout);
                    if (value) {
                        HashMap<String, String> map = new HashMap<>();
                        map.put(Content.EST, Message.SESSION_TIMEOUT.name());
                        ipfs.pubsub_pub(user.getPid(), gson.toJson(map));
                    } else {
                        events.onFailure();
                    }
                } catch (Throwable e) {
                    Preferences.evaluateException(Preferences.EXCEPTION, e);
                }
            });
        }

    }

    public void emitSessionReject(@NonNull PID user, @NonNull ConnectionEvents events, long timeout) {
        checkNotNull(user);
        checkNotNull(events);
        final IPFS ipfs = Singleton.getInstance().getIpfs();
        if (ipfs != null) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    boolean value = ConnectService.connectUser(user, timeout);
                    if (value) {
                        HashMap<String, String> map = new HashMap<>();
                        map.put(Content.EST, Message.SESSION_REJECT.name());
                        ipfs.pubsub_pub(user.getPid(), gson.toJson(map));
                    } else {
                        events.onFailure();
                    }
                } catch (Throwable e) {
                    Preferences.evaluateException(Preferences.EXCEPTION, e);
                }
            });
        }

    }

    public void emitSessionAccept(@NonNull PID user, @NonNull ConnectionEvents events, long timeout) {
        checkNotNull(user);
        checkNotNull(events);
        final IPFS ipfs = Singleton.getInstance().getIpfs();
        if (ipfs != null) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    boolean value = ConnectService.connectUser(user, timeout);
                    if (value) {
                        HashMap<String, String> map = new HashMap<>();
                        map.put(Content.EST, Message.SESSION_ACCEPT.name());
                        ipfs.pubsub_pub(user.getPid(), gson.toJson(map));
                    } else {
                        events.onFailure();
                    }
                } catch (Throwable e) {
                    Preferences.evaluateException(Preferences.EXCEPTION, e);
                }

            });
        }


    }

    public void emitSessionClose(@NonNull PID user, @NonNull ConnectionEvents events, long timeout) {
        checkNotNull(user);
        checkNotNull(events);
        final IPFS ipfs = Singleton.getInstance().getIpfs();
        if (ipfs != null) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    boolean value = ConnectService.connectUser(user, timeout);
                    if (value) {
                        HashMap<String, String> map = new HashMap<>();
                        map.put(Content.EST, Message.SESSION_CLOSE.name());
                        ipfs.pubsub_pub(user.getPid(), gson.toJson(map));
                    } else {
                        events.onFailure();
                    }
                } catch (Throwable e) {
                    Preferences.evaluateException(Preferences.EXCEPTION, e);
                }
            });
        }
    }

    public void emitIceCandidatesRemove(@NonNull PID user,
                                        @NonNull ConnectionEvents events,
                                        @NonNull IceCandidate[] candidates,
                                        int timeout) {
        checkNotNull(user);
        checkNotNull(events);
        checkNotNull(candidates);
        final IPFS ipfs = Singleton.getInstance().getIpfs();
        if (ipfs != null) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    boolean value = ConnectService.connectUser(user, timeout);
                    if (value) {
                        for (IceCandidate candidate : candidates) {
                            HashMap<String, String> map = new HashMap<>();
                            map.put(Content.EST, Message.SESSION_CANDIDATE_REMOVE.name());
                            map.put(Content.SDP, candidate.sdp);
                            map.put(Content.MID, candidate.sdpMid);
                            map.put(Content.INDEX, String.valueOf(candidate.sdpMLineIndex));

                            ipfs.pubsub_pub(user.getPid(), gson.toJson(map));
                        }
                    } else {
                        events.onFailure();
                    }
                } catch (Throwable e) {
                    Preferences.evaluateException(Preferences.EXCEPTION, e);
                }
            });
        }

    }

    public void emitIceCandidate(@NonNull PID user,
                                 @NonNull ConnectionEvents events,
                                 @NonNull IceCandidate candidate, int timeout) {
        checkNotNull(user);
        checkNotNull(events);
        checkNotNull(candidate);
        final IPFS ipfs = Singleton.getInstance().getIpfs();
        if (ipfs != null) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    boolean value = ConnectService.connectUser(user, timeout);
                    if (value) {
                        HashMap<String, String> map = new HashMap<>();
                        map.put(Content.EST, Message.SESSION_CANDIDATE.name());
                        map.put(Content.SDP, candidate.sdp);
                        map.put(Content.MID, candidate.sdpMid);
                        map.put(Content.INDEX, String.valueOf(candidate.sdpMLineIndex));

                        ipfs.pubsub_pub(user.getPid(), gson.toJson(map));
                    } else {
                        events.onFailure();
                    }
                } catch (Throwable e) {
                    Preferences.evaluateException(Preferences.EXCEPTION, e);
                }
            });
        }

    }


    public interface Listener {
        void busy(@NonNull PID pid);

        void accept(@NonNull PID pid);

        void reject(@NonNull PID pid);

        void offer(@NonNull PID pid, @NonNull String sdp);

        void answer(@NonNull PID pid, @NonNull String sdp, @NonNull String type);

        void candidate(@NonNull PID pid, @NonNull String sdp,
                       @NonNull String mid, @NonNull String index);

        void candidate_remove(@NonNull PID pid, @NonNull String sdp,
                              @NonNull String mid, @NonNull String index);

        void close(@NonNull PID pid);

        void timeout(@NonNull PID pid);
    }
}
