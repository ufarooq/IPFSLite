package threads.server;

import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RendererCommon;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import threads.core.Preferences;
import threads.core.Singleton;
import threads.core.THREADS;
import threads.core.api.Addresses;
import threads.core.api.Content;
import threads.ipfs.api.PID;
import threads.share.ConnectService;

public class VideoActivity extends AppCompatActivity implements Session.Listener {
    public static final int VIDEO_RESOLUTION_WIDTH = 1280;
    public static final int VIDEO_RESOLUTION_HEIGHT = 720;
    public static final int FPS = 30;
    private static final String TAG = VideoActivity.class.getSimpleName();
    private static final int CAPTURE_PERMISSION_REQUEST_CODE = 1;
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
    PeerConnection localPeer;
    EglBase rootEglBase;
    boolean gotUserMedia;
    List<PeerConnection.IceServer> peerIceServers = new ArrayList<>();
    private long mLastClickTime = 0;

    private static int getSystemUiVisibility() {
        int flags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN;
        flags |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        return flags;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        //Thread.setDefaultUncaughtExceptionHandler(new UnhandledExceptionHandler(this));

        // Set window styles for fullscreen-window size. Needs to be done before
        // adding content.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(getSystemUiVisibility());


        setContentView(R.layout.activity_video);


        localVideoView = findViewById(R.id.local_gl_surface_view);
        remoteVideoView = findViewById(R.id.remote_gl_surface_view);
        initVideos();

        Intent intent = getIntent();
        String pid = intent.getStringExtra(Content.USER);
        user = PID.create(pid);


        PeerConnection.IceServer peerIceServer = PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer();
        peerIceServers.add(peerIceServer);

        //peerIceServers.add(PeerConnection.IceServer.builder("turn:turn01.hubl.in?transport=udp").createIceServer());
        //getIceServers();

        Session.getInstance().setListener(this);


        start();

        // Create peer connection client.
        /*
        final EglBase eglBase = EglBase.create();


        RTCPeerConnection peerConnectionClient = new RTCPeerConnection(
                getApplicationContext(), eglBase, peerConnectionParameters, VideoActivity.this);
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        if (loopback) {
            options.networkIgnoreMask = 0;
        }
        peerConnectionClient.createPeerConnectionFactory(options);*/


        createPeerConnection();


        FloatingActionButton fab_hangup = findViewById(R.id.fab_hangup);
        fab_hangup.setOnClickListener((view) -> {


            // mis-clicking prevention, using threshold of 1000 ms
            if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                return;
            }
            mLastClickTime = SystemClock.elapsedRealtime();

            hangup(true);

        });
    }

    private void initVideos() {
        rootEglBase = EglBase.create();
        localVideoView.init(rootEglBase.getEglBaseContext(), null);
        localVideoView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        remoteVideoView.init(rootEglBase.getEglBaseContext(), null);
        remoteVideoView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
        //remoteVideoView.setEnableHardwareScaler(false /* enabled */);
        localVideoView.setZOrderMediaOverlay(true);
        //localVideoView.setEnableHardwareScaler(true /* enabled */);
        remoteVideoView.setZOrderMediaOverlay(true);
    }

    private void startScreenCapture() {
        MediaProjectionManager mediaProjectionManager =
                (MediaProjectionManager) getApplication().getSystemService(
                        Context.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(
                mediaProjectionManager.createScreenCaptureIntent(), CAPTURE_PERMISSION_REQUEST_CODE);
    }


    private VideoCapturer createVideoCapturer() {
        VideoCapturer videoCapturer;
        if (useCamera2()) {
            videoCapturer = createCameraCapturer(new Camera2Enumerator(this));
        } else {
            videoCapturer = createCameraCapturer(new Camera1Enumerator(true));
        }
        return videoCapturer;
    }

    private VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
        final String[] deviceNames = enumerator.getDeviceNames();

        // First, try to find front facing camera
        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        // Front facing camera not found, try something else
        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        return null;
    }

    /*
     * Read more about Camera2 here
     * https://developer.android.com/reference/android/hardware/camera2/package-summary.html
     * */
    private boolean useCamera2() {
        return Camera2Enumerator.isSupported(this);
    }

    public void start() {


        // Create peer connection client.
        /*
        peerConnectionClient = new RTCPeerConnection(
                getApplicationContext(), eglBase, peerConnectionParameters, RTCCallActivity.this);
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        if (loopback) {
            options.networkIgnoreMask = 0;
        }
        peerConnectionClient.createPeerConnectionFactory(options);
*/


        //Initialize PeerConnectionFactory globals.
        PeerConnectionFactory.InitializationOptions initializationOptions =
                PeerConnectionFactory.InitializationOptions.builder(this)
                        //.setEnableVideoHwAcceleration(true)
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
        VideoCapturer videoCapturerAndroid = createVideoCapturer();


        //Create MediaConstraints - Will be useful for specifying video and audio constraints.
        audioConstraints = new MediaConstraints();
        videoConstraints = new MediaConstraints();

        //Create a VideoSource instance
        if (videoCapturerAndroid != null) {

            SurfaceTextureHelper surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", rootEglBase.getEglBaseContext());
            videoSource = peerConnectionFactory.createVideoSource(videoCapturerAndroid.isScreencast());
            videoCapturerAndroid.initialize(surfaceTextureHelper, VideoActivity.this, videoSource.getCapturerObserver());

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

    public String getHost(String address) {
        String[] parts = address.substring(1).split("/");
        return parts[1];
    }

    public String getPort(String address) {
        String[] parts = address.substring(1).split("/");
        return parts[3];
    }

    public String getProtocol(String address) {
        String[] parts = address.substring(1).split("/");
        return parts[2];
    }

    private void getIceServers() {
        final THREADS threads = Singleton.getInstance().getThreads();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {

                threads.core.api.Peer peer = threads.getPeerByPID(user);
                if (peer != null) {
                    Addresses addresses = peer.getAddresses();
                    for (String relay : addresses.values()) {
                        String url = getProtocol(relay) + "://" + getHost(relay) + ":" + getPort(relay);
                        PeerConnection.IceServer peerIceServer = PeerConnection.IceServer.builder(url).createIceServer();
                        peerIceServers.add(peerIceServer);
                    }
                }

            } catch (Throwable e) {
                Preferences.evaluateException(Preferences.EXCEPTION, e);
            }
        });
    }


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
                int timeout = ConnectService.getConnectionTimeout(getApplicationContext());
                Session.getInstance().emitIceCandidate(user, iceCandidate, timeout);
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
    private void doCall(@NonNull PID user) {
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
                long timeout = ConnectService.getConnectionTimeout(getApplicationContext());
                Session.getInstance().emitSessionOffer(user, sessionDescription, timeout);
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
                videoTrack.addSink(remoteVideoView);
            } catch (Throwable e) {
                Preferences.evaluateException(Preferences.EXCEPTION, e);
            }
        });

    }


    public void onOfferReceived(final String sdp) {
        showToast("Received Offer");
        runOnUiThread(() -> {
            try {
                localPeer.setRemoteDescription(new CustomSdpObserver("localSetRemote"), new SessionDescription(SessionDescription.Type.OFFER, sdp));
                doAnswer();
                //updateVideoViews(true);
            } catch (Throwable e) {
                Preferences.evaluateException(Preferences.EXCEPTION, e);
            }
        });
    }

    private void doAnswer() {
        localPeer.createAnswer(new CustomSdpObserver("localCreateAns") {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                super.onCreateSuccess(sessionDescription);
                localPeer.setLocalDescription(new CustomSdpObserver("localSetLocal"), sessionDescription);
                long timeout = ConnectService.getConnectionTimeout(getApplicationContext());
                Session.getInstance().emitSessionAnswer(user, sessionDescription, timeout);
            }
        }, new MediaConstraints());
    }


    public void onAnswerReceived(String sdp, String type) {
        showToast("Received Answer");
        try {
            Log.e(TAG, type + " : " + sdp);
            localPeer.setRemoteDescription(new CustomSdpObserver("localSetRemote"),
                    new SessionDescription(SessionDescription.Type.valueOf(type), sdp));
            //updateVideoViews(true);
        } catch (Throwable e) {
            Preferences.evaluateException(Preferences.EXCEPTION, e);
        }
    }


    public void onIceCandidateReceived(String sdp, String mid, String index) {
        try {
            localPeer.addIceCandidate(new IceCandidate(
                    mid,
                    Integer.valueOf(index),
                    sdp));
        } catch (Throwable e) {
            Preferences.evaluateException(Preferences.EXCEPTION, e);
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


    private void hangup(boolean emitSessionClose) {
        try {
            localPeer.close();
            if (emitSessionClose) {
                long timeout = ConnectService.getConnectionTimeout(getApplicationContext());
                Session.getInstance().emitSessionClose(user, timeout);
            }
            finish();
        } catch (Throwable e) {
            Preferences.evaluateException(Preferences.EXCEPTION, e);
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Session.getInstance().setListener(null);
    }

    /**
     * Util Methods
     */
    public int dpToPx(int dp) {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    public void showToast(final String msg) {
        runOnUiThread(() -> Toast.makeText(VideoActivity.this, msg, Toast.LENGTH_SHORT).show());
    }


    @Override
    public void busy(PID pid) {
        hangup(false);
    }

    @Override
    public void accept(PID pid) {
        doCall(pid);
    }

    @Override
    public void reject(PID pid) {
        hangup(false);
    }

    @Override
    public void offer(PID pid, String sdp) {
        onOfferReceived(sdp);
    }

    @Override
    public void answer(PID pid, String sdp, String type) {
        onAnswerReceived(sdp, type);
    }

    @Override
    public void candidate(PID pid, String sdp, String mid, String index) {
        onIceCandidateReceived(sdp, mid, index);
    }

    @Override
    public void candidate_remove(PID pid, String sdp, String mid, String index) {

    }

    @Override
    public void close(PID pid) {
        hangup(false);
    }

    @Override
    public void timeout(PID pid) {
        hangup(false);
    }
}
