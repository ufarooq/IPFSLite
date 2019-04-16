/*
 *  Copyright 2016 The WebRTC Project Authors. All rights reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package threads.server;

import org.webrtc.IceCandidate;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import threads.ipfs.api.PID;

import static androidx.core.util.Preconditions.checkNotNull;


public class RTCClient implements Session.Listener {

    private final SignalingEvents events;
    private final PID user;
    private final int timeout;


    public RTCClient(@NonNull PID user, @NonNull SignalingEvents events, int timeout) {
        checkNotNull(user);
        checkNotNull(events);


        this.events = events;
        this.user = user;
        this.timeout = timeout;


        Session.getInstance().setListener(this);
    }

    @Override
    public void busy(@NonNull PID pid) {
        checkNotNull(pid);
        // TODO better handling
        events.onChannelClose();
    }

    @Override
    public void accept(@NonNull PID pid) {
        checkNotNull(pid);
        SignalingParameters parameters = new SignalingParameters(
                // Ice servers are not needed for direct connections.
                new ArrayList<>(),
                true,
                null
        );

        events.onConnectedToRoom(parameters);
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
        SessionDescription sdpe = new SessionDescription(
                SessionDescription.Type.OFFER, sdp);

        SignalingParameters parameters = new SignalingParameters(
                // Ice servers are not needed for direct connections.
                new ArrayList<>(),
                false, // This code will only be run on the client side. So, we are not the initiator.
                sdpe
        );

        events.onConnectedToRoom(parameters);
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
        Session.getInstance().emitSessionOffer(user, sdp, timeout);
    }


    public void sendAnswerSdp(@NonNull final SessionDescription sdp) {
        checkNotNull(sdp);
        Session.getInstance().emitSessionAnswer(user, sdp, timeout);
    }


    public void sendLocalIceCandidate(@NonNull final IceCandidate candidate) {
        checkNotNull(candidate);
        Session.getInstance().emitIceCandidate(user, candidate, timeout);
    }

    public void sendLocalIceCandidateRemovals(final IceCandidate[] candidates) {
        if (candidates != null) {
            Session.getInstance().emitIceCandidatesRemove(user, candidates, timeout);
        }
    }


    /**
     * Callback interface for messages delivered on signaling channel.
     *
     * <p>Methods are guaranteed to be invoked on the UI thread of |activity|.
     */
    public interface SignalingEvents {


        /**
         * Callback fired once the room's signaling parameters
         * SignalingParameters are extracted.
         */
        void onConnectedToRoom(final SignalingParameters params);

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

    }

    /**
     * Struct holding the signaling parameters of an AppRTC room.
     */
    class SignalingParameters {
        public final List<PeerConnection.IceServer> iceServers;
        public final boolean initiator;
        public final SessionDescription offerSdp;

        public SignalingParameters(List<PeerConnection.IceServer> iceServers, boolean initiator,
                                   SessionDescription offerSdp) {
            this.iceServers = iceServers;
            this.initiator = initiator;
            this.offerSdp = offerSdp;
        }
    }
}
