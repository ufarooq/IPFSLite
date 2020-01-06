package threads.server;

import android.app.ActionBar;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import threads.core.Preferences;
import threads.core.Singleton;
import threads.core.events.EVENTS;
import threads.core.threads.THREADS;
import threads.share.IPFSMediaDataSource;

import static androidx.core.util.Preconditions.checkNotNull;

public class VideoActivity extends AppCompatActivity implements MediaController.MediaPlayerControl,
        MediaPlayer.OnBufferingUpdateListener, SurfaceHolder.Callback, MediaPlayer.OnCompletionListener {
    public static final String CID_ID = "CID_ID";
    private static final String TAG = VideoActivity.class.getSimpleName();

    private MediaPlayer mediaPlayer;
    private MediaController mediaController;
    private int mCurrentBufferPercentage = 0;

    private ProgressBar progress_bar;
    private String cid;
    private THREADS threads;

    private IPFSMediaDataSource dataSource;
    private SurfaceView surfaceView;

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        int orientation = newConfig.orientation;

        switch (orientation) {

            case Configuration.ORIENTATION_LANDSCAPE:
                hideSystemUI();
                handleFullRatio();
                break;

            case Configuration.ORIENTATION_PORTRAIT:
                hideStatusBar();
                handleFullRatio();
                break;

        }


    }

    private void hideStatusBar() {
        View decorView = getWindow().getDecorView();

        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
    }

    public void handleFullRatio() {

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        int screenHeight = metrics.heightPixels;
        int screenWidth = metrics.widthPixels;

        int videoWidth = mediaPlayer.getVideoWidth();
        int videoHeight = mediaPlayer.getVideoHeight();
        float videoProportion = (float) videoWidth / (float) videoHeight;
        float screenProportion = (float) screenWidth / (float) screenHeight;
        ViewGroup.LayoutParams lp = surfaceView.getLayoutParams();

        if (videoProportion > screenProportion) {
            lp.width = screenWidth;
            lp.height = (int) ((float) screenWidth / videoProportion);
        } else {
            lp.width = (int) (videoProportion * (float) screenHeight);
            lp.height = screenHeight;
        }
        surfaceView.setLayoutParams(lp);
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        Configuration config = getResources().getConfiguration();
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            hideSystemUI();
        } else {
            hideStatusBar();
        }

        setContentView(R.layout.activity_video);
        mediaController = new MediaController(this);
        RelativeLayout layoutView = findViewById(R.id.video_preview_layout);

        surfaceView = findViewById(R.id.video_preview);
        layoutView.setOnClickListener((view) -> {

            if (mediaController != null) {
                if (!mediaController.isShowing()) {
                    mediaController.show();
                } else {
                    mediaController.hide();
                    hideSystemUI();
                }
            }

        });

        progress_bar = findViewById(R.id.progress_bar);
        progress_bar.setVisibility(View.INVISIBLE);

        Intent intent = getIntent();

        cid = intent.getStringExtra(CID_ID);
        checkNotNull(cid);

        mediaPlayer = new MediaPlayer();
        final EVENTS events = Singleton.getInstance(this).getEvents();
        threads = Singleton.getInstance(this).getThreads();
        try {
            dataSource = new IPFSMediaDataSource(this, cid);
        } catch (Throwable e) {
            Preferences.error(events, getString(R.string.video_failure,
                    e.getLocalizedMessage()));
            finish();
        }

        mediaController.setMediaPlayer(VideoActivity.this);
        mediaController.setAnchorView(layoutView);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        SurfaceHolder holder = surfaceView.getHolder();
        holder.addCallback(this);

    }

    @Override
    public void start() {
        if (mediaPlayer != null) {
            mediaPlayer.start();
        }
    }

    @Override
    public void pause() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
        }
    }

    @Override
    public int getDuration() {
        if (mediaPlayer != null) {
            return mediaPlayer.getDuration();
        }
        return 0;
    }

    @Override
    public int getCurrentPosition() {
        if (mediaPlayer != null) {
            return mediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    @Override
    public void seekTo(int i) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(i);
        }
    }

    @Override
    public boolean isPlaying() {
        if (mediaPlayer != null) {
            return mediaPlayer.isPlaying();
        }
        return false;
    }

    @Override
    public int getBufferPercentage() {
        return mCurrentBufferPercentage;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return cid.hashCode();
    }


    public void teardown() {
        if (mediaController != null) {
            mediaController.hide();
        }
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        teardown();
    }


    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {

        mediaPlayer.setDisplay(surfaceHolder);

        prepare();
    }


    private void prepare() {

        try {

            progress_bar.setVisibility(View.VISIBLE);
            mediaPlayer.setScreenOnWhilePlaying(true);
            mediaPlayer.setOnCompletionListener(this);
            mediaPlayer.setOnBufferingUpdateListener(this);

            mediaPlayer.setDataSource(dataSource);

            mediaPlayer.setOnPreparedListener((mp) -> {
                try {
                    progress_bar.setVisibility(View.INVISIBLE);
                    handleFullRatio();
                    mediaController.hide();
                    mediaPlayer.start();
                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
                    final EVENTS events = Singleton.getInstance(this).getEvents();
                    Preferences.error(events, getString(R.string.video_failure,
                            e.getLocalizedMessage()));
                    finish();
                }

            });
            mediaPlayer.prepareAsync();

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
            final EVENTS events = Singleton.getInstance(this).getEvents();
            Preferences.error(events, getString(R.string.video_failure,
                    e.getLocalizedMessage()));
            finish();
        }
    }


    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        finish();
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mediaPlayer, int percent) {
        mCurrentBufferPercentage = percent;
    }
}
