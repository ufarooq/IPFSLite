package threads.server;

import androidx.annotation.Nullable;
import threads.ipfs.api.PID;

public class Session {

    private static Session INSTANCE = new Session();
    @Nullable
    private Listener listener = null;

    private Session() {
    }

    public static Session getInstance() {
        return INSTANCE;
    }

    public void busy(PID pid) {
        if (listener != null) {
            listener.busy(pid);
        }
    }

    public void accept(PID pid) {
        if (listener != null) {
            listener.accept(pid);
        }
    }

    public void close(PID pid) {
        if (listener != null) {
            listener.close(pid);
        }
    }

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    public void reject(PID pid) {
        if (listener != null) {
            listener.reject(pid);
        }
    }

    public void offer(PID pid, String sdp) {
        if (listener != null) {
            listener.offer(pid, sdp);
        }
    }

    public void answer(PID pid, String sdp, String type) {
        if (listener != null) {
            listener.answer(pid, sdp, type);
        }
    }

    public void candidate(PID pid, String sdp, String mid, String index) {
        if (listener != null) {
            listener.candidate(pid, sdp, mid, index);
        }
    }


    public void candidate_remove(PID pid, String sdp, String mid, String index) {
        if (listener != null) {
            listener.candidate(pid, sdp, mid, index);
        }
    }

    public void timeout(PID pid) {
        if (listener != null) {
            listener.timeout(pid);
        }
    }

    public interface Listener {
        void busy(PID pid);

        void accept(PID pid);

        void reject(PID pid);

        void offer(PID pid, String sdp);

        void answer(PID pid, String sdp, String type);

        void candidate(PID pid, String sdp, String mid, String index);

        void candidate_remove(PID pid, String sdp, String mid, String index);

        void close(PID pid);

        void timeout(PID pid);
    }
}
