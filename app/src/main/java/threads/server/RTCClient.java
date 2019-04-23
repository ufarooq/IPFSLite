package threads.server;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

import threads.ipfs.api.PID;

import static androidx.core.util.Preconditions.checkNotNull;


public class RTCClient implements RTCSession.Listener {

    private final SignalingEvents events;
    private final PID user;
    private final int timeout;


    public RTCClient(@NonNull PID user, @NonNull SignalingEvents events, int timeout) {
        checkNotNull(user);
        checkNotNull(events);


        this.events = events;
        this.user = user;
        this.timeout = timeout;


        RTCSession.getInstance().setListener(this);
    }

    @Override
    public void busy(@NonNull PID pid) {
        checkNotNull(pid);
        // TODO better handling
        events.onChannelClose();
    }

    @Override
    public void accept(@NonNull PID pid, @Nullable String[] ices) {
        checkNotNull(pid);
        events.onAcceptedToRoom(ices);
    }

    @Override
    public void reject(@NonNull PID pid) {
        checkNotNull(pid);
        // TODO better handling
        events.onChannelClose();
    }

    @Override
    public void offer(@NonNull PID pid, @NonNull String sdp) {
        checkNotNull(pid);
        checkNotNull(sdp);

        events.onConnectedToRoom(new SessionDescription(
                SessionDescription.Type.OFFER, sdp));
    }

    @Override
    public void answer(@NonNull PID pid,
                       @NonNull String sdp,
                       @NonNull String type) {
        checkNotNull(pid);
        checkNotNull(sdp);
        checkNotNull(type);
        events.onRemoteDescription(new SessionDescription(
                SessionDescription.Type.fromCanonicalForm(type), sdp));
    }

    @Override
    public void candidate(@NonNull PID pid,
                          @NonNull String sdp,
                          @NonNull String mid,
                          @NonNull String index) {
        checkNotNull(pid);
        checkNotNull(sdp);
        checkNotNull(mid);
        checkNotNull(index);
        events.onRemoteIceCandidate(new IceCandidate(
                mid,
                Integer.valueOf(index),
                sdp));
    }

    @Override
    public void candidate_remove(@NonNull PID pid,
                                 @NonNull String sdp,
                                 @NonNull String mid,
                                 @NonNull String index) {
        checkNotNull(pid);
        checkNotNull(sdp);
        checkNotNull(mid);
        checkNotNull(index);
        events.onRemoteIceCandidateRemoved(new IceCandidate(
                mid,
                Integer.valueOf(index),
                sdp));
    }

    @Override
    public void close(@NonNull PID pid) {
        checkNotNull(pid);
        // TODO better handling
        events.onChannelClose();
    }

    @Override
    public void timeout(@NonNull PID pid) {
        checkNotNull(pid);
        // TODO better handling
        events.onChannelClose();
    }


    public void sendOfferSdp(@NonNull final SessionDescription sdp) {
        checkNotNull(sdp);
        RTCSession.getInstance().emitSessionOffer(user, events, sdp, timeout);
    }


    public void sendAnswerSdp(@NonNull final SessionDescription sdp) {
        checkNotNull(sdp);
        RTCSession.getInstance().emitSessionAnswer(user, events, sdp, timeout);
    }


    public void sendLocalIceCandidate(@NonNull final IceCandidate candidate) {
        checkNotNull(candidate);
        RTCSession.getInstance().emitIceCandidate(user, events, candidate, timeout);
    }

    public void sendLocalIceCandidateRemovals(final IceCandidate[] candidates) {
        if (candidates != null) {
            RTCSession.getInstance().emitIceCandidatesRemove(user, events, candidates, timeout);
        }
    }


    /**
     * Callback interface for messages delivered on signaling channel.
     *
     * <p>Methods are guaranteed to be invoked on the UI thread of |activity|.
     */
    public interface SignalingEvents extends ConnectionEvents {


        /**
         * Callback fired once the room's signaling parameters
         * SignalingParameters are extracted.
         */
        void onConnectedToRoom(final SessionDescription sdp);

        /**
         * Callback fired once remote SDP is received.
         */
        void onRemoteDescription(final SessionDescription sdp);

        /**
         * Callback fired once remote Ice candidate is received.
         */
        void onRemoteIceCandidate(final IceCandidate candidate);

        /**
         * Callback fired once remote Ice candidate removals are received.
         */
        void onRemoteIceCandidateRemoved(final IceCandidate candidate);

        /**
         * Callback fired once channel is closed.
         */
        void onChannelClose();

        void onAcceptedToRoom(@Nullable String[] ices);
    }

    public interface ConnectionEvents {
        void onConnectionFailure();
    }


}
