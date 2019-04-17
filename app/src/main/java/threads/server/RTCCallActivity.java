package threads.server;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.Logging;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RendererCommon.ScalingType;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoFileRenderer;
import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import threads.ipfs.api.PID;
import threads.server.RTCAudioManager.AudioDevice;
import threads.server.RTCPeerConnection.PeerConnectionParameters;
import threads.share.ConnectService;

import static androidx.core.util.Preconditions.checkNotNull;
public class RTCCallActivity extends AppCompatActivity implements RTCClient.SignalingEvents,
        RTCPeerConnection.PeerConnectionEvents {

    public static final String CALL_PID = "CALL_PID";
    public static final String INITIATOR = "INITIATOR";
    public static final String ACTION_INCOMING_CALL = "ACTION_INCOMING_CALL";

    public static final String EXTRA_VIDEO_CALL = "VIDEO_CALL";
    public static final String EXTRA_CAMERA2 = "CAMERA2";
    public static final String EXTRA_VIDEO_WIDTH = "VIDEO_WIDTH";
    public static final String EXTRA_VIDEO_HEIGHT = "VIDEO_HEIGHT";
    public static final String EXTRA_VIDEO_FPS = "VIDEO_FPS";
    public static final String EXTRA_VIDEO_BITRATE = "VIDEO_BITRATE";
    public static final String EXTRA_VIDEOCODEC = "VIDEOCODEC";
    public static final String EXTRA_HWCODEC_ENABLED = "HWCODEC";
    public static final String EXTRA_CAPTURETOTEXTURE_ENABLED = "CAPTURETOTEXTURE";
    public static final String EXTRA_FLEXFEC_ENABLED = "FLEXFEC";
    public static final String EXTRA_AUDIO_BITRATE = "AUDIO_BITRATE";
    public static final String EXTRA_AUDIOCODEC = "AUDIOCODEC";
    public static final String EXTRA_NOAUDIOPROCESSING_ENABLED = "NOAUDIOPROCESSING";
    public static final String EXTRA_AECDUMP_ENABLED = "AECDUMP";
    public static final String EXTRA_OPENSLES_ENABLED = "OPENSLES";
    public static final String EXTRA_DISABLE_BUILT_IN_AEC = "DISABLE_BUILT_IN_AEC";
    public static final String EXTRA_DISABLE_BUILT_IN_AGC = "DISABLE_BUILT_IN_AGC";
    public static final String EXTRA_DISABLE_BUILT_IN_NS = "DISABLE_BUILT_IN_NS";
    public static final String EXTRA_DISABLE_WEBRTC_AGC_AND_HPF = "DISABLE_WEBRTC_GAIN_CONTROL";
    public static final String EXTRA_TRACING = "TRACING";

    private static final String TAG = "CallRTCClient";

    private static final String[] MANDATORY_PERMISSIONS = {"android.permission.MODIFY_AUDIO_SETTINGS",
            "android.permission.RECORD_AUDIO", "android.permission.INTERNET"};

    private final ProxyVideoSink remoteProxyRenderer = new ProxyVideoSink();
    private final ProxyVideoSink localProxyVideoSink = new ProxyVideoSink();
    private final List<VideoSink> remoteSinks = new ArrayList<>();
    private final List<PeerConnection.IceServer> peerIceServers = new ArrayList<>();
    @Nullable
    private RTCPeerConnection peerConnectionClient;
    @Nullable
    private RTCClient appRtcClient;
    //@Nullable
    //private RTCClient.SignalingParameters signalingParameters;
    @Nullable
    private RTCAudioManager audioManager;
    private SurfaceViewRenderer pipRenderer;
    private SurfaceViewRenderer fullscreenRenderer;
    @Nullable
    private VideoFileRenderer videoFileRenderer;
    private Toast logToast;
    private boolean activityRunning;
    private PeerConnectionParameters peerConnectionParameters;
    private boolean connected;
    private boolean isError;
    private boolean callControlFragmentVisible = true;
    private long callStartedTimeMs;
    private boolean micEnabled = true;
    private boolean speakerEnabled = false;
    private boolean initiator;
    private long mLastClickTime = 0;
    // Controls
    private LinearLayout fab_layout;

    public static void call(@NonNull Context context, @NonNull String pid, boolean initiator) {
        checkNotNull(context);
        checkNotNull(pid);

        Intent intent = new Intent(context, RTCCallActivity.class);
        intent.putExtra(RTCCallActivity.CALL_PID, pid);
        intent.putExtra(RTCCallActivity.INITIATOR, initiator);
        intent.putExtra(RTCCallActivity.EXTRA_VIDEO_CALL, true);
        intent.putExtra(RTCCallActivity.EXTRA_CAMERA2, true);
        intent.putExtra(RTCCallActivity.EXTRA_VIDEO_WIDTH, 1024);
        intent.putExtra(RTCCallActivity.EXTRA_VIDEO_HEIGHT, 720);
        intent.putExtra(RTCCallActivity.EXTRA_VIDEO_FPS, 30);
        intent.putExtra(RTCCallActivity.EXTRA_VIDEO_BITRATE, 0);
        intent.putExtra(RTCCallActivity.EXTRA_VIDEOCODEC, RTCPeerConnection.VIDEO_CODEC_H264_HIGH);
        intent.putExtra(RTCCallActivity.EXTRA_HWCODEC_ENABLED, true);
        intent.putExtra(RTCCallActivity.EXTRA_CAPTURETOTEXTURE_ENABLED, false);
        intent.putExtra(RTCCallActivity.EXTRA_FLEXFEC_ENABLED, false);
        intent.putExtra(RTCCallActivity.EXTRA_NOAUDIOPROCESSING_ENABLED, false);
        intent.putExtra(RTCCallActivity.EXTRA_AECDUMP_ENABLED, false);
        intent.putExtra(RTCCallActivity.EXTRA_OPENSLES_ENABLED, false);
        intent.putExtra(RTCCallActivity.EXTRA_DISABLE_BUILT_IN_AEC, false);
        intent.putExtra(RTCCallActivity.EXTRA_DISABLE_BUILT_IN_AGC, false);
        intent.putExtra(RTCCallActivity.EXTRA_DISABLE_BUILT_IN_NS, false);
        intent.putExtra(RTCCallActivity.EXTRA_DISABLE_WEBRTC_AGC_AND_HPF, false);
        intent.putExtra(RTCCallActivity.EXTRA_AUDIO_BITRATE, 0);
        intent.putExtra(RTCCallActivity.EXTRA_AUDIOCODEC, RTCPeerConnection.AUDIO_CODEC_OPUS);
        intent.putExtra(RTCCallActivity.EXTRA_TRACING, false);

        context.startActivity(intent);

    }

    private static int getSystemUiVisibility() {
        int flags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN;
        flags |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        return flags;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(getSystemUiVisibility());

        setContentView(R.layout.activity_call);

        final Intent intent = getIntent();
        String pid = intent.getStringExtra(CALL_PID);
        PID user = PID.create(pid);

        initiator = intent.getBooleanExtra(INITIATOR, true);

        PeerConnection.IceServer peerIceServer =
                PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer();
        peerIceServers.add(peerIceServer);


        connected = false;

        // Create UI controls.
        pipRenderer = findViewById(R.id.pip_video_view);

        fullscreenRenderer = findViewById(R.id.fullscreen_video_view);


        // Swap feeds on pip view click.
        // TODO pipRenderer.setOnClickListener((view) -> setSwappedFeeds(!isSwappedFeeds));
        // Show/hide call control fragment on view click.
        fullscreenRenderer.setOnClickListener((view) -> toggleCallControls());

        remoteSinks.add(remoteProxyRenderer);

        final EglBase eglBase = EglBase.create();

        // Create video renderers.
        pipRenderer.init(eglBase.getEglBaseContext(), null);
        pipRenderer.setScalingType(ScalingType.SCALE_ASPECT_FIT);

        fullscreenRenderer.init(eglBase.getEglBaseContext(), null);
        fullscreenRenderer.setScalingType(ScalingType.SCALE_ASPECT_FILL);

        pipRenderer.setZOrderMediaOverlay(true);
        pipRenderer.setEnableHardwareScaler(true /* enabled */);
        fullscreenRenderer.setEnableHardwareScaler(false /* enabled */);
        // Start with local feed in fullscreen and swap it to the pip when the call is connected.
        setSwappedFeeds(true /* isSwappedFeeds */);

        // Check for mandatory permissions.
        for (String permission : MANDATORY_PERMISSIONS) {
            if (checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                logAndToast("Permission " + permission + " is not granted");
                setResult(RESULT_CANCELED);
                finish();
                return;
            }
        }


        int videoWidth = intent.getIntExtra(EXTRA_VIDEO_WIDTH, 0);
        int videoHeight = intent.getIntExtra(EXTRA_VIDEO_HEIGHT, 0);


        peerConnectionParameters =
                new PeerConnectionParameters(
                        intent.getBooleanExtra(EXTRA_VIDEO_CALL, true),
                        false,
                        intent.getBooleanExtra(EXTRA_TRACING, false),
                        videoWidth,
                        videoHeight,
                        intent.getIntExtra(EXTRA_VIDEO_FPS, 0),
                        intent.getIntExtra(EXTRA_VIDEO_BITRATE, 0),
                        intent.getStringExtra(EXTRA_VIDEOCODEC),
                        intent.getBooleanExtra(EXTRA_HWCODEC_ENABLED, true),
                        intent.getBooleanExtra(EXTRA_FLEXFEC_ENABLED, false),
                        intent.getIntExtra(EXTRA_AUDIO_BITRATE, 0),
                        intent.getStringExtra(EXTRA_AUDIOCODEC),
                        intent.getBooleanExtra(EXTRA_NOAUDIOPROCESSING_ENABLED, false),
                        intent.getBooleanExtra(EXTRA_AECDUMP_ENABLED, false),
                        intent.getBooleanExtra(EXTRA_OPENSLES_ENABLED, false),
                        intent.getBooleanExtra(EXTRA_DISABLE_BUILT_IN_AEC, false),
                        intent.getBooleanExtra(EXTRA_DISABLE_BUILT_IN_AGC, false),
                        intent.getBooleanExtra(EXTRA_DISABLE_BUILT_IN_NS, false),
                        intent.getBooleanExtra(EXTRA_DISABLE_WEBRTC_AGC_AND_HPF, false),
                        false, null);


        int timeout = ConnectService.getConnectionTimeout(this);
        appRtcClient = new RTCClient(user, this, timeout);


        // Create peer connection client.
        peerConnectionClient = new RTCPeerConnection(
                getApplicationContext(), eglBase, peerConnectionParameters, RTCCallActivity.this);
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();

        peerConnectionClient.createPeerConnectionFactory(options);


        startCall();


        fab_layout = findViewById(R.id.fab_layout);
        FloatingActionButton fab_hangup = findViewById(R.id.fab_hangup);
        fab_hangup.setOnClickListener((view) -> {


            // mis-clicking prevention, using threshold of 1000 ms
            if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                return;
            }
            mLastClickTime = SystemClock.elapsedRealtime();

            onCallHangUp();

        });


        FloatingActionButton fab_switch_camera = findViewById(R.id.fab_switch_camera);
        fab_switch_camera.setOnClickListener((view) -> {


            // mis-clicking prevention, using threshold of 1000 ms
            if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                return;
            }
            mLastClickTime = SystemClock.elapsedRealtime();

            onCameraSwitch();

        });

        FloatingActionButton fab_toggle_speaker = findViewById(R.id.fab_toggle_speaker);
        fab_toggle_speaker.setOnClickListener((view) -> {


            // mis-clicking prevention, using threshold of 1000 ms
            if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                return;
            }
            mLastClickTime = SystemClock.elapsedRealtime();

            boolean enabled = onToggleSpeaker();
            if (enabled) {
                fab_toggle_speaker.setImageResource(R.drawable.volume_high);
            } else {
                fab_toggle_speaker.setImageResource(R.drawable.volume_medium);
            }


        });

        FloatingActionButton fab_toggle_mic = findViewById(R.id.fab_toggle_mic);
        fab_toggle_mic.setOnClickListener((view) -> {


            // mis-clicking prevention, using threshold of 1000 ms
            if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                return;
            }
            mLastClickTime = SystemClock.elapsedRealtime();

            boolean enabled = onToggleMic();
            if (enabled) {
                fab_toggle_mic.setImageResource(R.drawable.microphone);
            } else {
                fab_toggle_mic.setImageResource(R.drawable.microphone_off);
            }


        });
    }


    private boolean useCamera2() {
        return Camera2Enumerator.isSupported(this) && getIntent().getBooleanExtra(EXTRA_CAMERA2, true);
    }

    private boolean captureToTexture() {
        return getIntent().getBooleanExtra(EXTRA_CAPTURETOTEXTURE_ENABLED, false);
    }

    private @Nullable
    VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
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


    @Override
    public void onStop() {
        super.onStop();
        activityRunning = false;
        // Don't stop the video when using screencapture to allow user to show other apps to the remote
        // end.
        if (peerConnectionClient != null) {
            peerConnectionClient.stopVideoSource();
        }

    }

    @Override
    public void onStart() {
        super.onStart();
        activityRunning = true;
        // Video is not paused for screencapture. See onPause.
        if (peerConnectionClient != null) {
            peerConnectionClient.startVideoSource();
        }
    }

    @Override
    protected void onDestroy() {
        Thread.setDefaultUncaughtExceptionHandler(null);
        disconnect();
        if (logToast != null) {
            logToast.cancel();
        }
        activityRunning = false;
        super.onDestroy();
    }


    public void onCallHangUp() {
        disconnect();
    }


    public void onCameraSwitch() {
        if (peerConnectionClient != null) {
            peerConnectionClient.switchCamera();
        }
    }


    public boolean onToggleMic() {
        if (peerConnectionClient != null) {
            micEnabled = !micEnabled;
            peerConnectionClient.setAudioEnabled(micEnabled);
        }
        return micEnabled;
    }

    public boolean onToggleSpeaker() {
        if (audioManager != null) {
            speakerEnabled = !speakerEnabled;
            if (speakerEnabled) {
                audioManager.setDefaultAudioDevice(AudioDevice.SPEAKER_PHONE);
            } else {
                audioManager.setDefaultAudioDevice(AudioDevice.EARPIECE);
            }
        }
        return speakerEnabled;
    }

    private void toggleCallControls() {

        // Show/hide call control fragment
        callControlFragmentVisible = !callControlFragmentVisible;

        if (callControlFragmentVisible) {
            fab_layout.setVisibility(View.VISIBLE);
        } else {
            fab_layout.setVisibility(View.GONE);
        }
    }


    public void startCall() {
        if (appRtcClient == null) {
            Log.e(TAG, "AppRTC client is not allocated for a call.");
            return;
        }
        callStartedTimeMs = System.currentTimeMillis();


        // Create and audio manager that will take care of audio routing,
        // audio modes, audio device enumeration etc.
        audioManager = RTCAudioManager.create(getApplicationContext(), AudioDevice.EARPIECE);

        audioManager.start(new RTCAudioManager.AudioManagerEvents() {
            // This method will be called each time the number of available audio
            // devices has changed.
            @Override
            public void onAudioDeviceChanged(
                    AudioDevice audioDevice, Set<AudioDevice> availableAudioDevices) {
                onAudioManagerDevicesChanged(audioDevice, availableAudioDevices);
            }
        });
    }

    // Should be called from UI thread
    private void callConnected() {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;
        Log.i(TAG, "Call connected: delay=" + delta + "ms");
        if (peerConnectionClient == null || isError) {
            Log.w(TAG, "Call is connected in closed or error state");
            return;
        }

        setSwappedFeeds(false /* isSwappedFeeds */);
    }

    // This method is called when the audio manager reports audio device change,
    // e.g. from wired headset to speakerphone.
    private void onAudioManagerDevicesChanged(
            final AudioDevice device, final Set<AudioDevice> availableDevices) {
        Log.d(TAG, "onAudioManagerDevicesChanged: " + availableDevices + ", "
                + "selected: " + device);
        // TODO(henrika): add callback handler.
    }

    // Disconnect from remote resources, dispose of local resources, and exit.
    private void disconnect() {
        activityRunning = false;
        remoteProxyRenderer.setTarget(null);
        localProxyVideoSink.setTarget(null);
        if (appRtcClient != null) {
            appRtcClient = null;
        }
        if (pipRenderer != null) {
            pipRenderer.release();
            pipRenderer = null;
        }
        if (videoFileRenderer != null) {
            videoFileRenderer.release();
            videoFileRenderer = null;
        }
        if (fullscreenRenderer != null) {
            fullscreenRenderer.release();
            fullscreenRenderer = null;
        }
        if (peerConnectionClient != null) {
            peerConnectionClient.close();
            peerConnectionClient = null;
        }
        if (audioManager != null) {
            audioManager.stop();
            audioManager = null;
        }
        if (connected && !isError) {
            setResult(RESULT_OK);
        } else {
            setResult(RESULT_CANCELED);
        }
        finish();
    }


    private void disconnectWithErrorMessage(final String errorMessage) {
        if (!activityRunning) {
            Log.e(TAG, "Critical error: " + errorMessage);
            disconnect();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle(getText(R.string.channel_error_title))
                    .setMessage(errorMessage)
                    .setCancelable(false)
                    .setNeutralButton(R.string.ok,
                            (dialog, id) -> {
                                dialog.cancel();
                                disconnect();
                            })
                    .create()
                    .show();
        }
    }

    // Log |msg| and Toast about it.
    private void logAndToast(String msg) {
        Log.d(TAG, msg);
        if (logToast != null) {
            logToast.cancel();
        }
        logToast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        logToast.show();
    }

    private void reportError(final String description) {
        runOnUiThread(() -> {
            if (!isError) {
                isError = true;
                disconnectWithErrorMessage(description);
            }
        });
    }

    private @Nullable
    VideoCapturer createVideoCapturer() {
        final VideoCapturer videoCapturer;

        if (useCamera2()) {
            if (!captureToTexture()) {
                reportError(getString(R.string.camera2_texture_only_error));
                return null;
            }

            Logging.d(TAG, "Creating capturer using camera2 API.");
            videoCapturer = createCameraCapturer(new Camera2Enumerator(this));
        } else {
            Logging.d(TAG, "Creating capturer using camera1 API.");
            videoCapturer = createCameraCapturer(new Camera1Enumerator(captureToTexture()));
        }
        if (videoCapturer == null) {
            reportError("Failed to open camera");
            return null;
        }
        return videoCapturer;
    }

    private void setSwappedFeeds(boolean isSwappedFeeds) {
        Logging.d(TAG, "setSwappedFeeds: " + isSwappedFeeds);
        localProxyVideoSink.setTarget(isSwappedFeeds ? fullscreenRenderer : pipRenderer);
        remoteProxyRenderer.setTarget(isSwappedFeeds ? pipRenderer : fullscreenRenderer);
        fullscreenRenderer.setMirror(isSwappedFeeds);
        pipRenderer.setMirror(!isSwappedFeeds);

    }


    @Override
    public void onConnectedToRoom(final SessionDescription offerSdp) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final long delta = System.currentTimeMillis() - callStartedTimeMs;

                logAndToast("Creating peer connection, delay=" + delta + "ms");
                VideoCapturer videoCapturer = null;
                if (peerConnectionParameters.videoCallEnabled) {
                    videoCapturer = createVideoCapturer();
                }
                peerConnectionClient.createPeerConnection(
                        localProxyVideoSink, remoteSinks, videoCapturer, peerIceServers);

                peerConnectionClient.setRemoteDescription(offerSdp);
                logAndToast("Creating ANSWER...");
                // Create answer. Answer SDP will be sent to offering client in
                // PeerConnectionEvents.onLocalDescription event.
                peerConnectionClient.createAnswer();

            }
        });
    }

    @Override
    public void onRemoteDescription(final SessionDescription sdp) {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (peerConnectionClient == null) {
                    Log.e(TAG, "Received remote SDP for non-initilized peer connection.");
                    return;
                }
                logAndToast("Received remote " + sdp.type + ", delay=" + delta + "ms");
                peerConnectionClient.setRemoteDescription(sdp);
                if (!initiator) {
                    logAndToast("Creating ANSWER...");
                    // Create answer. Answer SDP will be sent to offering client in
                    // PeerConnectionEvents.onLocalDescription event.
                    peerConnectionClient.createAnswer();
                }
            }
        });
    }

    @Override
    public void onRemoteIceCandidate(final IceCandidate candidate) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (peerConnectionClient == null) {
                    Log.e(TAG, "Received ICE candidate for a non-initialized peer connection.");
                    return;
                }
                peerConnectionClient.addRemoteIceCandidate(candidate);
            }
        });
    }

    @Override
    public void onRemoteIceCandidateRemoved(final IceCandidate candidate) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (peerConnectionClient == null) {
                    Log.e(TAG, "Received ICE candidate removals for a non-initialized peer connection.");
                    return;
                }
                IceCandidate[] candidates = {candidate};
                peerConnectionClient.removeRemoteIceCandidates(candidates);
            }
        });
    }

    @Override
    public void onChannelClose() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                logAndToast("Remote end hung up; dropping PeerConnection");
                disconnect();
            }
        });
    }

    @Override
    public void onAcceptedToRoom() {
        runOnUiThread(() -> {

            final long delta = System.currentTimeMillis() - callStartedTimeMs;

            logAndToast("Creating peer connection, delay=" + delta + "ms");
            VideoCapturer videoCapturer = null;
            if (peerConnectionParameters.videoCallEnabled) {
                videoCapturer = createVideoCapturer();
            }
            peerConnectionClient.createPeerConnection(
                    localProxyVideoSink, remoteSinks, videoCapturer, peerIceServers);

            logAndToast("Creating OFFER...");
            // Create offer. Offer SDP will be sent to answering client in
            // PeerConnectionEvents.onLocalDescription event.
            peerConnectionClient.createOffer();

        });
    }


    // -----Implementation of RTCPeerConnection.PeerConnectionEvents.---------
    // Send local peer connection SDP and ICE candidates to remote party.
    // All callbacks are invoked from peer connection client looper thread and
    // are routed to UI thread.
    @Override
    public void onLocalDescription(final SessionDescription sdp) {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;
        runOnUiThread(() -> {

            if (appRtcClient != null) {
                logAndToast("Sending " + sdp.type + ", delay=" + delta + "ms");
                if (initiator) {
                    appRtcClient.sendOfferSdp(sdp);
                } else {
                    appRtcClient.sendAnswerSdp(sdp);
                }
            }
            if (peerConnectionParameters.videoMaxBitrate > 0) {
                Log.d(TAG, "Set video maximum bitrate: " + peerConnectionParameters.videoMaxBitrate);
                peerConnectionClient.setVideoMaxBitrate(peerConnectionParameters.videoMaxBitrate);
            }

        });
    }

    @Override
    public void onIceCandidate(final IceCandidate candidate) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (appRtcClient != null) {
                    appRtcClient.sendLocalIceCandidate(candidate);
                }
            }
        });
    }

    @Override
    public void onIceCandidatesRemoved(final IceCandidate[] candidates) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (appRtcClient != null) {
                    appRtcClient.sendLocalIceCandidateRemovals(candidates);
                }
            }
        });
    }

    @Override
    public void onIceConnected() {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                logAndToast("ICE connected, delay=" + delta + "ms");
            }
        });
    }

    @Override
    public void onIceDisconnected() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                logAndToast("ICE disconnected");
            }
        });
    }

    @Override
    public void onConnected() {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;
        runOnUiThread(() -> {

            logAndToast("DTLS connected, delay=" + delta + "ms");
            connected = true;
            callConnected();

        });
    }

    @Override
    public void onDisconnected() {
        runOnUiThread(() -> {
            logAndToast("DTLS disconnected");
            connected = false;
            disconnect();
        });
    }

    @Override
    public void onPeerConnectionClosed() {
        // TODO
        // check implementation
        runOnUiThread(() -> {
            logAndToast("Peer connection closed");
            connected = false;
            disconnect();
        });
    }


    @Override
    public void onPeerConnectionError(final String description) {
        reportError(description);
    }

    @Override
    public void onConnectionFailure() {
        runOnUiThread(() -> logAndToast("Connecting to peer failed"));
    }

    private static class ProxyVideoSink implements VideoSink {
        private VideoSink target;

        @Override
        synchronized public void onFrame(VideoFrame frame) {
            if (target == null) {
                Logging.d(TAG, "Dropping frame in proxy because target is null.");
                return;
            }
            target.onFrame(frame);
        }

        synchronized void setTarget(VideoSink target) {
            this.target = target;
        }
    }
}
