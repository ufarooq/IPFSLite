package threads.server;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProviders;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RendererCommon.ScalingType;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;

import java.util.ArrayList;
import java.util.List;

import threads.core.Preferences;
import threads.core.mdl.EventViewModel;
import threads.ipfs.api.PID;
import threads.server.RTCAudioManager.AudioDevice;
import threads.server.RTCPeerConnection.PeerConnectionParameters;
import threads.share.ConnectService;

import static androidx.core.util.Preconditions.checkNotNull;

public class RTCCallActivity extends AppCompatActivity implements
        RTCClient.SignalingEvents,
        RTCPeerConnection.PeerConnectionEvents,
        RTCCallingDialogFragment.ActionListener,
        RTCCallDialogFragment.ActionListener {
    public static final String CALL_NAME = "CALL_NAME";
    public static final String CALL_PID = "CALL_PID";
    public static final String CALL_ICES = "CALL_ICES";
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
    private static final int REQUEST_VIDEO_CAPTURE = 1;
    private static final int REQUEST_AUDIO_CAPTURE = 2;
    private static final int REQUEST_MODIFY_AUDIO_SETTINGS = 3;
    private static final String TAG = "CallRTCClient";


    private final ProxyVideoSink remoteProxyRenderer = new ProxyVideoSink();
    private final ProxyVideoSink localProxyVideoSink = new ProxyVideoSink();
    private final List<VideoSink> remoteSinks = new ArrayList<>();
    boolean isReceiverRegistered = false;
    private CallBroadcastReceiver callBroadcastReceiver;
    @Nullable
    private RTCPeerConnection peerConnectionClient;
    @Nullable
    private RTCClient appRtcClient;
    @Nullable
    private RTCAudioManager audioManager;
    private SurfaceViewRenderer pipRenderer;
    private SurfaceViewRenderer fullscreenRenderer;

    private PeerConnectionParameters peerConnectionParameters;

    private boolean callControlFragmentVisible = true;
    private long callStartedTimeMs;
    private boolean micEnabled = true;
    private boolean speakerEnabled = true;
    private boolean initiator;
    private long mLastClickTime = 0;
    private String callee;
    private String calleeName;
    private List<PeerConnection.IceServer> peerIceServers = new ArrayList<>();
    // Controls
    private LinearLayout fab_layout;
    private RTCCallDialogFragment callDialog;
    private RTCSoundPool soundPoolManager;


    public static Intent createIntent(@NonNull Context context,
                                      @NonNull String pid,
                                      @NonNull String name,
                                      @Nullable String[] ices,
                                      boolean initiator) {
        checkNotNull(context);
        checkNotNull(pid);
        Intent intent = new Intent(context, RTCCallActivity.class);
        intent.putExtra(RTCCallActivity.CALL_PID, pid);
        intent.putExtra(RTCCallActivity.CALL_NAME, name);
        intent.putExtra(RTCCallActivity.INITIATOR, initiator);
        intent.putExtra(RTCCallActivity.CALL_ICES, ices);
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
        return intent;
    }


    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI();
        registerReceiver();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        hideSystemUI();


        setContentView(R.layout.activity_call);


        final Intent intent = getIntent();
        callee = intent.getStringExtra(CALL_PID);
        calleeName = intent.getStringExtra(CALL_NAME);
        PID user = PID.create(callee);
        String[] ices = intent.getStringArrayExtra(CALL_ICES);

        // TODO maybe remove in the future
        PeerConnection.IceServer peerIceServer =
                PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer();
        peerIceServers.add(peerIceServer);

        if (ices != null) {
            for (String turn : ices) {
                Log.e(TAG, "Turn address : " + turn); // TODO remove
                peerIceServers.add(
                        PeerConnection.IceServer.builder(turn).createIceServer());
            }
        }

        initiator = intent.getBooleanExtra(INITIATOR, true);

        // Create UI controls.
        pipRenderer = findViewById(R.id.pip_video_view);

        fullscreenRenderer = findViewById(R.id.fullscreen_video_view);


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


        callStartedTimeMs = System.currentTimeMillis();

        soundPoolManager = RTCSoundPool.create(this, R.raw.disconnect, false);

        // Create and audio manager that will take care of audio routing,
        // audio modes, audio device enumeration etc.
        AudioDevice defaultAudioDevice = AudioDevice.EARPIECE;
        if (speakerEnabled) {
            defaultAudioDevice = AudioDevice.SPEAKER_PHONE;
        }

        audioManager = RTCAudioManager.create(getApplicationContext(), defaultAudioDevice);

        audioManager.start((audioDevice, availableAudioDevices) -> {
            // This method will be called each time the number of available audio
            // devices has changed.

            Log.d(TAG, "onAudioManagerDevicesChanged: " + availableAudioDevices + ", "
                    + "selected: " + audioDevice);


        });


        fab_layout = findViewById(R.id.fab_layout);
        FloatingActionButton fab_hangup = findViewById(R.id.fab_hangup);
        fab_hangup.setOnClickListener((view) -> {


            // mis-clicking prevention, using threshold of 1000 ms
            if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                return;
            }
            mLastClickTime = SystemClock.elapsedRealtime();

            if (soundPoolManager != null) {
                soundPoolManager.play();
            }

            disconnect(2500);

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

        if (speakerEnabled) {
            fab_toggle_speaker.setImageResource(R.drawable.volume_high);
        } else {
            fab_toggle_speaker.setImageResource(R.drawable.volume_medium);
        }

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


        EventViewModel eventViewModel = ViewModelProviders.of(this).get(EventViewModel.class);
        eventViewModel.getException().observe(this, (event) -> {
            try {
                if (event != null) {
                    Snackbar snackbar = Snackbar.make(fullscreenRenderer, event.getContent(),
                            Snackbar.LENGTH_INDEFINITE);
                    snackbar.setAction(android.R.string.ok, (view) -> {
                        eventViewModel.removeEvent(event);
                        snackbar.dismiss();

                    });
                    snackbar.show();
                }
            } catch (Throwable e) {
                Preferences.evaluateException(Preferences.EXCEPTION, e);
            }

        });

        eventViewModel.getWarning().observe(this, (event) -> {
            try {
                if (event != null) {
                    Toast.makeText(
                            getApplicationContext(), event.getContent(), Toast.LENGTH_LONG).show();
                    eventViewModel.removeEvent(event);
                }
            } catch (Throwable e) {
                Preferences.evaluateException(Preferences.EXCEPTION, e);
            }

        });


        callBroadcastReceiver = new CallBroadcastReceiver();
        registerReceiver();

        handleIncomingCallIntent(getIntent());


        if (initiator) {
            callDialog = RTCCallDialogFragment.newInstance(callee, calleeName);
            callDialog.show(getSupportFragmentManager(), RTCCallDialogFragment.TAG);
        }
    }

    private void handleIncomingCallIntent(Intent intent) {
        if (intent != null && intent.getAction() != null) {
            if (intent.getAction().equals(RTCCallActivity.ACTION_INCOMING_CALL)) {
                String pid = intent.getStringExtra(RTCCallActivity.CALL_PID);
                if (pid != null && !pid.isEmpty()) {
                    receiveUserCall();
                    intent.removeExtra(RTCCallActivity.CALL_PID);
                }
            }
        }
    }


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        hideSystemUI();
    }

    private void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        // Set the content to appear under the system bars so that the
                        // content doesn't resize when the system bars hide and show.
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        // Hide the nav bar and status bar
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    @Override
    public void onRequestPermissionsResult
            (int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {

            case REQUEST_AUDIO_CAPTURE: {
                for (int i = 0, len = permissions.length; i < len; i++) {
                    if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        Snackbar.make(fullscreenRenderer,
                                getString(R.string.permission_audio_denied),
                                Snackbar.LENGTH_LONG)
                                .setAction(R.string.app_settings, new PermissionAction()).show();
                    }
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        receiveUserCall();
                    }
                }

                break;
            }
            case REQUEST_MODIFY_AUDIO_SETTINGS: {
                for (int i = 0, len = permissions.length; i < len; i++) {
                    if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        Snackbar.make(fullscreenRenderer,
                                getString(R.string.permission_audio_settings_denied),
                                Snackbar.LENGTH_LONG)
                                .setAction(R.string.app_settings, new PermissionAction()).show();
                    }
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        receiveUserCall();
                    }
                }

                break;
            }
            case REQUEST_VIDEO_CAPTURE: {
                for (int i = 0, len = permissions.length; i < len; i++) {
                    if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        Snackbar.make(fullscreenRenderer,
                                getString(R.string.permission_camera_denied),
                                Snackbar.LENGTH_LONG)
                                .setAction(R.string.app_settings, new PermissionAction()).show();
                    }
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        receiveUserCall();
                    }
                }

                break;
            }
        }
    }

    public void receiveUserCall() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_AUDIO_CAPTURE);
            return;
        }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_VIDEO_CAPTURE);
            return;
        }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.MODIFY_AUDIO_SETTINGS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.MODIFY_AUDIO_SETTINGS},
                    REQUEST_MODIFY_AUDIO_SETTINGS);
            return;
        }


        RTCCallingDialogFragment.newInstance(callee, calleeName)
                .show(getSupportFragmentManager(), RTCCallingDialogFragment.TAG);

    }

    private void registerReceiver() {
        if (!isReceiverRegistered) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(RTCCallActivity.ACTION_INCOMING_CALL);

            LocalBroadcastManager.getInstance(this).registerReceiver(
                    callBroadcastReceiver, intentFilter);
            isReceiverRegistered = true;
        }
    }

    private void unregisterReceiver() {
        if (isReceiverRegistered) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(callBroadcastReceiver);
            isReceiverRegistered = false;
        }
    }


    private boolean useCamera2() {
        return Camera2Enumerator.isSupported(this) && getIntent().getBooleanExtra(EXTRA_CAMERA2, true);
    }

    private boolean captureToTexture() {
        return getIntent().getBooleanExtra(EXTRA_CAPTURETOTEXTURE_ENABLED, false);
    }

    @Nullable
    private VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
        final String[] deviceNames = enumerator.getDeviceNames();


        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                VideoCapturer videoCapturer = enumerator.createCapturer(
                        deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {

                VideoCapturer videoCapturer = enumerator.createCapturer(
                        deviceName, null);

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

        // Don't stop the video when using screencapture to allow user to show other apps to the remote
        // end.
        if (peerConnectionClient != null) {
            peerConnectionClient.stopVideoSource();
        }

    }

    @Override
    public void onStart() {
        super.onStart();

        // Video is not paused for screencapture. See onPause.
        if (peerConnectionClient != null) {
            peerConnectionClient.startVideoSource();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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


    // Disconnect from remote resources, dispose of local resources, and exit.
    private void disconnect(long timeout) {

        closeCallDialog();


        remoteProxyRenderer.setTarget(null);
        localProxyVideoSink.setTarget(null);

        if (appRtcClient != null) {
            appRtcClient = null;
        }
        if (pipRenderer != null) {
            pipRenderer.release();
            pipRenderer = null;
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


        RTCSession.getInstance().setBusy(false);

        new Handler().postDelayed(() -> {
            try {
                closeSoundPool();
            } catch (Throwable e) {
                Preferences.evaluateException(Preferences.EXCEPTION, e);
            } finally {
                finish();
            }

        }, timeout);


    }

    private void closeSoundPool() {
        try {
            if (soundPoolManager != null) {
                soundPoolManager.release();
                soundPoolManager = null;
            }
        } catch (Throwable e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
    }

    private void closeCallDialog() {
        try {
            if (callDialog != null) {
                callDialog.dismiss();
                callDialog = null;
            }
        } catch (Throwable e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
    }


    @Nullable
    private VideoCapturer createVideoCapturer() {
        final VideoCapturer videoCapturer;

        if (useCamera2()) {
            if (!captureToTexture()) {
                Preferences.error(getString(R.string.camera2_texture_only_error));
                disconnect(2500);
                return null;
            }

            videoCapturer = createCameraCapturer(new Camera2Enumerator(this));
        } else {
            videoCapturer = createCameraCapturer(new Camera1Enumerator(captureToTexture()));
        }
        if (videoCapturer == null) {
            Preferences.error("Failed to open camera");
            disconnect(2500);
            return null;
        }
        return videoCapturer;
    }

    private void setSwappedFeeds(boolean isSwappedFeeds) {
        localProxyVideoSink.setTarget(isSwappedFeeds ? fullscreenRenderer : pipRenderer);
        remoteProxyRenderer.setTarget(isSwappedFeeds ? pipRenderer : fullscreenRenderer);
        fullscreenRenderer.setMirror(isSwappedFeeds);
        pipRenderer.setMirror(!isSwappedFeeds);

    }


    @Override
    public void onConnectedToRoom(final SessionDescription offerSdp) {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;

        Preferences.warning("Creating peer connection, delay=" + delta + "ms");

        runOnUiThread(() -> {
            try {

                VideoCapturer videoCapturer = null;
                if (peerConnectionParameters.videoCallEnabled) {
                    videoCapturer = createVideoCapturer();
                }
                checkNotNull(peerConnectionClient);

                peerConnectionClient.createPeerConnection(
                        localProxyVideoSink, remoteSinks, videoCapturer, peerIceServers);

                peerConnectionClient.setRemoteDescription(offerSdp);

                // Create answer. Answer SDP will be sent to offering client in
                // PeerConnectionEvents.onLocalDescription event.
                peerConnectionClient.createAnswer();
            } catch (Throwable e) {
                Preferences.evaluateException(Preferences.EXCEPTION, e);
            }

        });
    }

    @Override
    public void onRemoteDescription(final SessionDescription sdp) {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;
        runOnUiThread(() -> {

            if (peerConnectionClient == null) {
                return;
            }
            Preferences.warning("Received remote " + sdp.type + ", delay=" + delta + "ms");
            peerConnectionClient.setRemoteDescription(sdp);
            if (!initiator) {
                // Create answer. Answer SDP will be sent to offering client in
                // PeerConnectionEvents.onLocalDescription event.
                peerConnectionClient.createAnswer();
            }

        });
    }

    @Override
    public void onRemoteIceCandidate(final IceCandidate candidate) {
        runOnUiThread(() -> {
            if (peerConnectionClient != null) {
                peerConnectionClient.addRemoteIceCandidate(candidate);
            }
        });
    }

    @Override
    public void onRemoteIceCandidateRemoved(final IceCandidate candidate) {
        runOnUiThread(() -> {
            if (peerConnectionClient != null) {
                IceCandidate[] candidates = {candidate};
                peerConnectionClient.removeRemoteIceCandidates(candidates);
            }
        });
    }

    @Override
    public void onChannelClose() {
        Preferences.warning("Remote end hung up; dropping PeerConnection");
        runOnUiThread(() -> disconnect(1000));
    }

    @Override
    public void onAcceptedToRoom(@Nullable String[] ices) {
        runOnUiThread(() -> {
            try {

                if (ices != null) {
                    for (String turn : ices) {
                        Log.e(TAG, "Turn address : " + turn);
                        peerIceServers.add(
                                PeerConnection.IceServer.builder(turn).createIceServer());
                    }
                }


                final long delta = System.currentTimeMillis() - callStartedTimeMs;

                Preferences.warning("Creating peer connection, delay=" + delta + "ms");
                VideoCapturer videoCapturer = null;
                if (peerConnectionParameters.videoCallEnabled) {
                    videoCapturer = createVideoCapturer();
                }
                checkNotNull(peerConnectionClient); // must be defined here


                peerConnectionClient.createPeerConnection(
                        localProxyVideoSink, remoteSinks, videoCapturer, peerIceServers);

                Preferences.warning("Creating OFFER...");
                // Create offer. Offer SDP will be sent to answering client in
                // PeerConnectionEvents.onLocalDescription event.
                peerConnectionClient.createOffer();
            } catch (Throwable e) {
                Preferences.evaluateException(Preferences.EXCEPTION, e);
            }
        });
    }

    // -----Implementation of RTCPeerConnection.PeerConnectionEvents.---------
    // Send local peer connection SDP and ICE candidates to remote party.
    // All callbacks are invoked from peer connection client looper thread and
    // are routed to UI thread.
    @Override
    public void onLocalDescription(final SessionDescription sdp) {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;

        if (appRtcClient != null) {
            Preferences.warning("Sending " + sdp.type + ", delay=" + delta + "ms");
            if (initiator) {
                appRtcClient.sendOfferSdp(sdp);
            } else {
                appRtcClient.sendAnswerSdp(sdp);
            }
        }

        runOnUiThread(() -> {

            if (peerConnectionParameters.videoMaxBitrate > 0) {

                if (peerConnectionClient != null) {
                    peerConnectionClient.setVideoMaxBitrate(
                            peerConnectionParameters.videoMaxBitrate);
                }
            }

        });
    }

    @Override
    public void onIceCandidate(final IceCandidate candidate) {
        if (appRtcClient != null) {
            appRtcClient.sendLocalIceCandidate(candidate);
        }

    }

    @Override
    public void onIceCandidatesRemoved(final IceCandidate[] candidates) {
        if (appRtcClient != null) {
            appRtcClient.sendLocalIceCandidateRemovals(candidates);
        }
    }

    @Override
    public void onIceConnected() {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;
        Preferences.warning("ICE connected, delay=" + delta + "ms");
    }

    @Override
    public void onIceDisconnected() {
        Preferences.warning("ICE disconnected");
    }

    @Override
    public void onConnected() {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;
        Preferences.warning("DTLS connected, delay=" + delta + "ms");
        closeCallDialog();
        runOnUiThread(() -> setSwappedFeeds(false));
    }

    @Override
    public void onDisconnected() {
        Preferences.warning("DTLS disconnected");
        runOnUiThread(() -> disconnect(1000));
    }

    @Override
    public void onPeerConnectionClosed() {
        Preferences.warning("Peer connection closed");
        runOnUiThread(() -> disconnect(1000));
    }

    @Override
    public void onPeerConnectionError(final String description) {
        Preferences.error(description);
    }

    @Override
    public void onConnectionFailure() {
        Preferences.warning("Connecting to peer failed");
    }

    @Override
    public void acceptCall(@NonNull String pid) {

        try {
            PID host = Preferences.getPID(getApplicationContext());
            checkNotNull(host);
            final long timeout = ConnectService.getConnectionTimeout(getApplicationContext());
            RTCSession.getInstance().emitSessionAccept(host, PID.create(pid), () ->
                            Preferences.warning(getString(R.string.connection_failed))
                    , timeout);

            if (!initiator) {
                onToggleSpeaker();
            }

        } catch (Throwable e) {
            Preferences.evaluateException(Preferences.EXCEPTION, e);
        }
    }

    @Override
    public void rejectCall(@NonNull String pid) {
        checkNotNull(pid);
        try {
            final long timeout = ConnectService.getConnectionTimeout(getApplicationContext());
            RTCSession.getInstance().emitSessionReject(PID.create(pid), () ->
                            Preferences.warning(getString(R.string.connection_failed))
                    , timeout);

            if (soundPoolManager != null) {
                soundPoolManager.play();
            }

            disconnect(2500);
        } catch (Throwable e) {
            Preferences.evaluateException(Preferences.EXCEPTION, e);
        } finally {
            RTCSession.getInstance().setBusy(false);
        }
    }

    @Override
    public void abortCall(@NonNull String pid) {
        checkNotNull(pid);
        try {

            final long timeout = ConnectService.getConnectionTimeout(getApplicationContext());
            RTCSession.getInstance().emitSessionClose(PID.create(pid), () ->
                            Preferences.warning(getString(R.string.connection_failed))
                    , timeout);

            if (soundPoolManager != null) {
                soundPoolManager.play();
            }

            disconnect(2500);
        } catch (Throwable e) {
            Preferences.evaluateException(Preferences.EXCEPTION, e);
        } finally {
            RTCSession.getInstance().setBusy(false);
        }
    }

    @Override
    public void timeoutCall(@NonNull String pid) {
        try {
            final long timeout = ConnectService.getConnectionTimeout(getApplicationContext());
            RTCSession.getInstance().emitSessionTimeout(PID.create(pid), () ->
                            Preferences.warning(getString(R.string.connection_failed))
                    , timeout);

            if (soundPoolManager != null) {
                soundPoolManager.play();
            }

            disconnect(2500);
        } catch (Throwable e) {
            Preferences.evaluateException(Preferences.EXCEPTION, e);
        }

    }

    private static class ProxyVideoSink implements VideoSink {
        private VideoSink target;

        @Override
        synchronized public void onFrame(VideoFrame frame) {
            if (target == null) {
                return;
            }
            target.onFrame(frame);
        }

        synchronized void setTarget(VideoSink target) {
            this.target = target;
        }
    }

    private class CallBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null && action.equals(RTCCallActivity.ACTION_INCOMING_CALL)) {
                handleIncomingCallIntent(intent);
            }
        }
    }

}
