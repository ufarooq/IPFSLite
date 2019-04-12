package threads.server;

import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.Logging;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProviders;
import threads.core.Preferences;
import threads.core.api.Content;
import threads.core.mdl.EventViewModel;
import threads.ipfs.api.PID;

public class CallActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "CallActivity";


    PID user;
    PeerConnectionFactory peerConnectionFactory;
    MediaConstraints audioConstraints;
    MediaConstraints videoConstraints;
    MediaConstraints sdpConstraints;
    VideoSource videoSource;
    VideoTrack localVideoTrack;
    AudioSource audioSource;
    AudioTrack localAudioTrack;
    SurfaceViewRenderer localVideoView;
    SurfaceViewRenderer remoteVideoView;
    Button hangup;
    PeerConnection localPeer;
    EglBase rootEglBase;

    boolean gotUserMedia;
    List<PeerConnection.IceServer> peerIceServers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webrtc);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        initViews();
        initVideos();

        Intent intent = getIntent();
        String pid = intent.getStringExtra(Content.USER);
        user = PID.create(pid);
        boolean initiator = intent.getBooleanExtra(Content.INITIATOR, true);

        start();

        onTryToStart(initiator);


        EventViewModel eventViewModel = ViewModelProviders.of(this).get(EventViewModel.class);

        eventViewModel.getEvent(Message.SESSION_OFFER.name()).observe(this, (event) -> {
            try {
                if (event != null) {
                    onOfferReceived(event.getContent());
                }
            } catch (Throwable e) {
                Preferences.evaluateException(Preferences.EXCEPTION, e);
            }

        });


        eventViewModel.getEvent(Message.SESSION_ANSWER.name()).observe(this, (event) -> {
            try {
                if (event != null) {
                    onAnswerReceived(event.getContent());
                }
            } catch (Throwable e) {
                Preferences.evaluateException(Preferences.EXCEPTION, e);
            }

        });

        eventViewModel.getEvent(Message.SESSION_CANDIDATE.name()).observe(this, (event) -> {
            try {
                if (event != null) {
                    onIceCandidateReceived(event.getContent());
                }
            } catch (Throwable e) {
                Preferences.evaluateException(Preferences.EXCEPTION, e);
            }

        });


        eventViewModel.getEvent(Message.SESSION_CLOSE.name()).observe(this, (event) -> {
            try {
                if (event != null) {
                    runOnUiThread(this::hangup);
                }
            } catch (Throwable e) {
                Preferences.evaluateException(Preferences.EXCEPTION, e);
            }

        });
    }


    private void initViews() {
        hangup = findViewById(R.id.end_call);
        localVideoView = findViewById(R.id.local_gl_surface_view);
        remoteVideoView = findViewById(R.id.remote_gl_surface_view);
        hangup.setOnClickListener(this);
    }

    private void initVideos() {
        rootEglBase = EglBase.create();
        localVideoView.init(rootEglBase.getEglBaseContext(), null);
        remoteVideoView.init(rootEglBase.getEglBaseContext(), null);
        localVideoView.setZOrderMediaOverlay(true);
        remoteVideoView.setZOrderMediaOverlay(true);
    }


    public void start() {
        //Initialize PeerConnectionFactory globals.
        PeerConnectionFactory.InitializationOptions initializationOptions =
                PeerConnectionFactory.InitializationOptions.builder(this)
                        .setEnableVideoHwAcceleration(true)
                        .createInitializationOptions();
        PeerConnectionFactory.initialize(initializationOptions);

        //Create a new PeerConnectionFactory instance - using Hardware encoder and decoder.
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        DefaultVideoEncoderFactory defaultVideoEncoderFactory = new DefaultVideoEncoderFactory(
                rootEglBase.getEglBaseContext(),  /* enableIntelVp8Encoder */true,  /* enableH264HighProfile */true);
        DefaultVideoDecoderFactory defaultVideoDecoderFactory = new DefaultVideoDecoderFactory(rootEglBase.getEglBaseContext());

        PeerConnectionFactory.Builder builder = PeerConnectionFactory.builder();
        builder = builder.setOptions(options);
        builder = builder.setVideoDecoderFactory(defaultVideoDecoderFactory);
        builder = builder.setVideoEncoderFactory(defaultVideoEncoderFactory);


        peerConnectionFactory = builder.createPeerConnectionFactory();


        //Now create a VideoCapturer instance.
        VideoCapturer videoCapturerAndroid;
        videoCapturerAndroid = createCameraCapturer(new Camera1Enumerator(false));


        //Create MediaConstraints - Will be useful for specifying video and audio constraints.
        audioConstraints = new MediaConstraints();
        videoConstraints = new MediaConstraints();

        //Create a VideoSource instance
        if (videoCapturerAndroid != null) {
            videoSource = peerConnectionFactory.createVideoSource(videoCapturerAndroid);
        }
        localVideoTrack = peerConnectionFactory.createVideoTrack("100", videoSource);

        //create an AudioSource instance
        audioSource = peerConnectionFactory.createAudioSource(audioConstraints);
        localAudioTrack = peerConnectionFactory.createAudioTrack("101", audioSource);


        if (videoCapturerAndroid != null) {
            videoCapturerAndroid.startCapture(1024, 720, 30);
        }
        localVideoView.setVisibility(View.VISIBLE);
        remoteVideoView.setVisibility(View.VISIBLE);
        // And finally, with our VideoRenderer ready, we
        // can add our renderer to the VideoTrack.
        localVideoTrack.addSink(localVideoView);

        localVideoView.setMirror(true);
        remoteVideoView.setMirror(true);

        gotUserMedia = true;


    }


    /**
     * This method will be called directly by the app when it is the initiator and has got the local media
     * or when the remote peer sends a message through socket that it is ready to transmit AV data
     */
    //@Override
    public void onTryToStart(boolean doCall) {
        runOnUiThread(() -> {
            //if (!SignallingClient.getInstance().isStarted && localVideoTrack != null && SignallingClient.getInstance().isChannelReady) {
            createPeerConnection();
            //SignallingClient.getInstance().isStarted = true;
            //if (SignallingClient.getInstance().isInitiator) {
            if (doCall) doCall();
            //}
            //}
        });
    }


    /**
     * Creating the local peerconnection instance
     */
    private void createPeerConnection() {
        PeerConnection.RTCConfiguration rtcConfig =
                new PeerConnection.RTCConfiguration(peerIceServers);
        // TCP candidates are only useful when connecting to a server that supports
        // ICE-TCP.
        rtcConfig.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED;
        rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE;
        rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE;
        rtcConfig.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY;
        // Use ECDSA encryption.
        rtcConfig.keyType = PeerConnection.KeyType.ECDSA;
        localPeer = peerConnectionFactory.createPeerConnection(rtcConfig, new CustomPeerConnectionObserver("localPeerCreation") {
            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                super.onIceCandidate(iceCandidate);
                Service.emitIceCandidate(user, iceCandidate);
            }

            @Override
            public void onAddStream(MediaStream mediaStream) {
                showToast("Received Remote stream");
                super.onAddStream(mediaStream);
                gotRemoteStream(mediaStream);
            }
        });

        addStreamToLocalPeer();
    }

    /**
     * Adding the stream to the localpeer
     */
    private void addStreamToLocalPeer() {
        //creating local mediastream
        MediaStream stream = peerConnectionFactory.createLocalMediaStream("102");
        stream.addTrack(localAudioTrack);
        stream.addTrack(localVideoTrack);
        localPeer.addStream(stream);
    }

    /**
     * This method is called when the app is initiator - We generate the offer and send it over through socket
     * to remote peer
     */
    private void doCall() {
        sdpConstraints = new MediaConstraints();
        sdpConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        sdpConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
                "OfferToReceiveVideo", "true"));
        localPeer.createOffer(new CustomSdpObserver("localCreateOffer") {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                super.onCreateSuccess(sessionDescription);
                localPeer.setLocalDescription(new CustomSdpObserver("localSetLocalDesc"), sessionDescription);
                Service.emitSessionOffer(user, sessionDescription);
            }
        }, sdpConstraints);
    }

    /**
     * Received remote peer's media stream. we will get the first video track and render it
     */
    private void gotRemoteStream(MediaStream stream) {
        //we have remote video stream. add to the renderer.
        final VideoTrack videoTrack = stream.videoTracks.get(0);
        runOnUiThread(() -> {
            try {
                remoteVideoView.setVisibility(View.VISIBLE);
                videoTrack.addSink(remoteVideoView);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

    }


    /**
     * SignallingCallback - called when the room is created - i.e. you are the initiator
     */
    //@Override
    public void onCreatedRoom() {
        showToast("You created the room " + gotUserMedia);
        if (gotUserMedia) {
            //SignallingClient.getInstance().emitMessage("got user media");
        }
    }

    /**
     * SignallingCallback - called when you join the room - you are a participant
     */
    //@Override
    public void onJoinedRoom() {
        showToast("You joined the room " + gotUserMedia);
        if (gotUserMedia) {
            //SignallingClient.getInstance().emitMessage("got user media");
        }
    }

    //@Override
    public void onNewPeerJoined() {
        showToast("Remote Peer Joined");
    }

    //
    public void onRemoteHangUp(String msg) {
        showToast("Remote Peer hungup");
        runOnUiThread(this::hangup);
    }

    /**
     * SignallingCallback - Called when remote peer sends offer
     */
    //@Override
    public void onOfferReceived(final String sdp) {
        showToast("Received Offer");
        runOnUiThread(() -> {
            try {
                localPeer.setRemoteDescription(new CustomSdpObserver("localSetRemote"), new SessionDescription(SessionDescription.Type.OFFER, sdp));
                doAnswer();
                updateVideoViews(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void doAnswer() {
        localPeer.createAnswer(new CustomSdpObserver("localCreateAns") {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                super.onCreateSuccess(sessionDescription);
                localPeer.setLocalDescription(new CustomSdpObserver("localSetLocal"), sessionDescription);
                Service.emitSessionAnswer(user, sessionDescription);
            }
        }, new MediaConstraints());
    }

    /**
     * SignallingCallback - Called when remote peer sends answer to your offer
     */

    //@Override
    public void onAnswerReceived(String sdp) {
        showToast("Received Answer");
        try {
            localPeer.setRemoteDescription(new CustomSdpObserver("localSetRemote"),
                    new SessionDescription(SessionDescription.Type.ANSWER, sdp));
            updateVideoViews(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Remote IceCandidate received
     */
    //@Override
    public void onIceCandidateReceived(String data) {
        try {
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, String>>() {
            }.getType();
            Map<String, String> map = gson.fromJson(data, type);
            localPeer.addIceCandidate(new IceCandidate(
                    map.get(Content.MID),
                    Integer.valueOf(map.get(Content.INDEX)),
                    map.get(Content.SDP)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void updateVideoViews(final boolean remoteVisible) {
        runOnUiThread(() -> {
            ViewGroup.LayoutParams params = localVideoView.getLayoutParams();
            if (remoteVisible) {
                params.height = dpToPx(100);
                params.width = dpToPx(100);
            } else {
                params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            }
            localVideoView.setLayoutParams(params);
        });

    }


    /**
     * Closing up - normal hangup and app destroye
     */

    @Override
    public void onClick(View v) {
        hangup();
    }

    private void hangup() {
        try {
            localPeer.close();
            localPeer = null;
            Service.emitSessionClose(user);
            //updateVideoViews(false);
            finish();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onDestroy() {
        Service.emitSessionClose(user);
        super.onDestroy();
    }

    /**
     * Util Methods
     */
    public int dpToPx(int dp) {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    public void showToast(final String msg) {
        runOnUiThread(() -> Toast.makeText(CallActivity.this, msg, Toast.LENGTH_SHORT).show());
    }

    private VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
        final String[] deviceNames = enumerator.getDeviceNames();

        // First, try to find front facing camera
        Logging.d(TAG, "Looking for front facing cameras.");
        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                Logging.d(TAG, "Creating front facing camera capturer.");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        // Front facing camera not found, try something else
        Logging.d(TAG, "Looking for other cameras.");
        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                Logging.d(TAG, "Creating other camera capturer.");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        return null;
    }
}
