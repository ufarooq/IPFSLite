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

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import threads.ipfs.api.PID;

/**
 * Implementation of RTCClient that uses direct TCP connection as the signaling channel.
 * This eliminates the need for an external server. This class does not support loopback
 * connections.
 */
public class RTCClient implements Session.Listener {

    private static final String TAG = "RTCClient";
    private final ExecutorService executor;
    private final SignalingEvents events;
    private final PID user;
    private final int timeout;

    // All alterations of the room state should be done from inside the looper thread.
    private ConnectionState roomState;

    public RTCClient(PID user, SignalingEvents events, int timeout) {
        this.events = events;
        this.user = user;
        this.timeout = timeout;

        Session.getInstance().setListener(this);
        executor = Executors.newSingleThreadExecutor();
        roomState = ConnectionState.NEW;
    }

    // Put a |key|->|value| mapping in |json|.
    private static void jsonPut(JSONObject json, String key, Object value) {
        try {
            json.put(key, value);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    // Converts a Java candidate to a JSONObject.
    private static JSONObject toJsonCandidate(final IceCandidate candidate) {
        JSONObject json = new JSONObject();
        jsonPut(json, "label", candidate.sdpMLineIndex);
        jsonPut(json, "id", candidate.sdpMid);
        jsonPut(json, "candidate", candidate.sdp);
        return json;
    }

    // Converts a JSON candidate to a Java object.
    private static IceCandidate toJavaCandidate(JSONObject json) throws JSONException {
        return new IceCandidate(
                json.getString("id"), json.getInt("label"), json.getString("candidate"));
    }

    @Override
    public void busy(PID pid) {

    }

    @Override
    public void accept(PID pid) {
        roomState = ConnectionState.CONNECTED;

        SignalingParameters parameters = new SignalingParameters(
                // Ice servers are not needed for direct connections.
                new ArrayList<>(),
                true, // Server side acts as the initiator on direct connections.
                null, // clientId
                null, // wssUrl
                null, // wwsPostUrl
                null, // offerSdp
                null // iceCandidates
        );
        events.onConnectedToRoom(parameters);
    }

    @Override
    public void reject(PID pid) {
        events.onChannelClose();
    }

    @Override
    public void offer(PID pid, String sdp) {
        SessionDescription sdpe = new SessionDescription(
                SessionDescription.Type.OFFER, sdp);

        SignalingParameters parameters = new SignalingParameters(
                // Ice servers are not needed for direct connections.
                new ArrayList<>(),
                false, // This code will only be run on the client side. So, we are not the initiator.
                null, // clientId
                null, // wssUrl
                null, // wssPostUrl
                sdpe, // offerSdp
                null // iceCandidates
        );
        roomState = ConnectionState.CONNECTED;
        events.onConnectedToRoom(parameters);
    }

    @Override
    public void answer(PID pid, String sdp, String type) {
        SessionDescription sdpe = new SessionDescription(
                SessionDescription.Type.fromCanonicalForm(type), sdp);
        events.onRemoteDescription(sdpe);
    }

    @Override
    public void candidate(PID pid, String sdp, String mid, String index) {


        events.onRemoteIceCandidate(new IceCandidate(
                mid,
                Integer.valueOf(index),
                sdp));
    }

    @Override
    public void candidate_remove(PID pid, String sdp, String mid, String index) {
        events.onRemoteIceCandidateRemoved(new IceCandidate(
                mid,
                Integer.valueOf(index),
                sdp));
    }

    @Override
    public void close(PID pid) {
        events.onChannelClose();
    }

    @Override
    public void timeout(PID pid) {
        events.onChannelClose();
    }

    /**
     * Connects to the room, roomId in connectionsParameters is required. roomId must be a valid
     * IP address matching IP_PATTERN.
     */

    public void connectToRoom() {
        this.roomState = ConnectionState.NEW;
    }


    public void disconnectFromRoom() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                disconnectFromRoomInternal();
            }
        });
    }


    /**
     * Disconnects from the room.
     * <p>
     * Runs on the looper thread.
     */
    private void disconnectFromRoomInternal() {
        roomState = ConnectionState.CLOSED;

        executor.shutdown();
    }


    public void sendOfferSdp(final SessionDescription sdp) {


        Service.emitSessionOffer(user, sdp, timeout);

      /*
    executor.execute(new Runnable() {
      @Override
      public void run() {
        if (roomState != ConnectionState.CONNECTED) {
          reportError("Sending offer SDP in non connected state.");
          return;
        }
        JSONObject json = new JSONObject();
        jsonPut(json, "sdp", sdp.description);
        jsonPut(json, "type", "offer");
        sendMessage(json.toString());
      }
    });*/
    }


    public void sendAnswerSdp(final SessionDescription sdp) {

        Service.emitSessionAnswer(user, sdp, timeout);
      /*
    executor.execute(new Runnable() {
      @Override
      public void run() {
        JSONObject json = new JSONObject();
        jsonPut(json, "sdp", sdp.description);
        jsonPut(json, "type", "answer");
        sendMessage(json.toString());
      }
    });*/
    }

    // -------------------------------------------------------------------
    // TCPChannelClient event handlers


    public void sendLocalIceCandidate(final IceCandidate candidate) {
        Service.emitIceCandidate(user, candidate, timeout);
      /*
    executor.execute(new Runnable() {
      @Override
      public void run() {
        JSONObject json = new JSONObject();
        jsonPut(json, "type", "candidate");
        jsonPut(json, "label", candidate.sdpMLineIndex);
        jsonPut(json, "id", candidate.sdpMid);
        jsonPut(json, "candidate", candidate.sdp);

        if (roomState != ConnectionState.CONNECTED) {
          reportError("Sending ICE candidate in non connected state.");
          return;
        }
        sendMessage(json.toString());
      }
    });*/
    }

    /**
     * Send removed Ice candidates to the other participant.
     */

    public void sendLocalIceCandidateRemovals(final IceCandidate[] candidates) {
        if (candidates != null) {
            Service.emitIceCandidatesRemove(user, candidates, timeout);
        }
        // TODO
      /*
    executor.execute(new Runnable() {
      @Override
      public void run() {
        JSONObject json = new JSONObject();
        jsonPut(json, "type", "remove-candidates");
        JSONArray jsonArray = new JSONArray();
        for (final IceCandidate candidate : candidates) {
          jsonArray.put(toJsonCandidate(candidate));
        }
        jsonPut(json, "candidates", jsonArray);

        if (roomState != ConnectionState.CONNECTED) {
          reportError("Sending ICE candidate removals in non connected state.");
          return;
        }
        sendMessage(json.toString());
      }
    });*/
    }

    /**
     * If the client is the server side, this will trigger onConnectedToRoom.
     */

    public void onTCPConnected(boolean isServer) {
        if (isServer) {
            roomState = ConnectionState.CONNECTED;

            SignalingParameters parameters = new SignalingParameters(
                    // Ice servers are not needed for direct connections.
                    new ArrayList<>(),
                    isServer, // Server side acts as the initiator on direct connections.
                    null, // clientId
                    null, // wssUrl
                    null, // wwsPostUrl
                    null, // offerSdp
                    null // iceCandidates
            );
            events.onConnectedToRoom(parameters);
        }
    }

    //@Override
    public void onTCPMessage(String msg) {
        try {
            JSONObject json = new JSONObject(msg);
            String type = json.optString("type");
            if (type.equals("candidate")) {
                events.onRemoteIceCandidate(toJavaCandidate(json));
            } else if (type.equals("remove-candidates")) {
                JSONArray candidateArray = json.getJSONArray("candidates");
                IceCandidate[] candidates = new IceCandidate[candidateArray.length()];
                for (int i = 0; i < candidateArray.length(); ++i) {
                    candidates[i] = toJavaCandidate(candidateArray.getJSONObject(i));
                }
                //events.onRemoteIceCandidatesRemoved(candidates);
            } else if (type.equals("answer")) {
                SessionDescription sdp = new SessionDescription(
                        SessionDescription.Type.fromCanonicalForm(type), json.getString("sdp"));
                events.onRemoteDescription(sdp);
            } else if (type.equals("offer")) {
                SessionDescription sdp = new SessionDescription(
                        SessionDescription.Type.fromCanonicalForm(type), json.getString("sdp"));

                SignalingParameters parameters = new SignalingParameters(
                        // Ice servers are not needed for direct connections.
                        new ArrayList<>(),
                        false, // This code will only be run on the client side. So, we are not the initiator.
                        null, // clientId
                        null, // wssUrl
                        null, // wssPostUrl
                        sdp, // offerSdp
                        null // iceCandidates
                );
                roomState = ConnectionState.CONNECTED;
                events.onConnectedToRoom(parameters);
            } else {
                reportError("Unexpected TCP message: " + msg);
            }
        } catch (JSONException e) {
            reportError("TCP message JSON parsing error: " + e.toString());
        }
    }

    public void onTCPError(String description) {
        reportError("TCP connection error: " + description);
    }

    public void onTCPClose() {
        events.onChannelClose();
    }

    // --------------------------------------------------------------------
    // Helper functions.
    private void reportError(final String errorMessage) {
        Log.e(TAG, errorMessage);
        executor.execute(new Runnable() {
            @Override
            public void run() {
                if (roomState != ConnectionState.ERROR) {
                    roomState = ConnectionState.ERROR;
                    events.onChannelError(errorMessage);
                }
            }
        });
    }

    private void sendMessage(final String message) {
        // TODO
      /*
    executor.execute(new Runnable() {
      @Override
      public void run() {
        tcpClient.send(message);
      }
    });*/
    }

    private enum ConnectionState {NEW, CONNECTED, CLOSED, ERROR}


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

        /**
         * Callback fired once channel error happened.
         */
        void onChannelError(final String description);
    }

    /**
     * Struct holding the signaling parameters of an AppRTC room.
     */
    class SignalingParameters {
        public final List<PeerConnection.IceServer> iceServers;
        public final boolean initiator;
        public final String clientId;
        public final String wssUrl;
        public final String wssPostUrl;
        public final SessionDescription offerSdp;
        public final List<IceCandidate> iceCandidates;

        public SignalingParameters(List<PeerConnection.IceServer> iceServers, boolean initiator,
                                   String clientId, String wssUrl, String wssPostUrl, SessionDescription offerSdp,
                                   List<IceCandidate> iceCandidates) {
            this.iceServers = iceServers;
            this.initiator = initiator;
            this.clientId = clientId;
            this.wssUrl = wssUrl;
            this.wssPostUrl = wssPostUrl;
            this.offerSdp = offerSdp;
            this.iceCandidates = iceCandidates;
        }
    }
}
