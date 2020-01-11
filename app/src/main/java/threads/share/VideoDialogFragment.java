package threads.share;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

import java.util.concurrent.atomic.AtomicBoolean;

import threads.core.Preferences;
import threads.core.events.EVENTS;
import threads.core.threads.THREADS;
import threads.server.R;

import static androidx.core.util.Preconditions.checkNotNull;

public class VideoDialogFragment extends DialogFragment implements MediaController.MediaPlayerControl,
        MediaPlayer.OnBufferingUpdateListener, SurfaceHolder.Callback, MediaPlayer.OnCompletionListener {
    public static final String TAG = VideoDialogFragment.class.getSimpleName();
    private static final String URI = "URI";

    private final AtomicBoolean stopped = new AtomicBoolean(false);
    private MediaPlayer mediaPlayer;
    private MediaController mediaController;
    private int mCurrentBufferPercentage = 0;
    private ProgressBar progress_bar;
    private Uri uri;
    private SurfaceView surfaceView;
    private Context mContext;
    //private IPFSMediaDataSource dataSource;
    private THREADS threads;

    public static VideoDialogFragment newInstance(@NonNull Uri uri) {
        VideoDialogFragment dialogFragment = new VideoDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable(VideoDialogFragment.URI, uri);
        dialogFragment.setArguments(bundle);
        return dialogFragment;
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        handleFullRatio();
    }

    private void handleFullRatio() {

        DisplayMetrics metrics = new DisplayMetrics();
        FragmentActivity activity = getActivity();

        if (activity != null) {
            activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);


            Configuration config = getResources().getConfiguration();
            int screenHeight = metrics.heightPixels;
            if (config.orientation == Configuration.ORIENTATION_PORTRAIT) {
                screenHeight = (screenHeight * 6) / 10;
            }

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
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mContext = null;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        Activity activity = getActivity();
        checkNotNull(activity);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        LayoutInflater inflater = activity.getLayoutInflater();


        @SuppressWarnings("all")
        View view = inflater.inflate(R.layout.video_dialog, null);

        threads = THREADS.getInstance(mContext);

        mediaController = new MediaController(mContext);
        RelativeLayout layoutView = view.findViewById(R.id.video_preview_layout);

        surfaceView = view.findViewById(R.id.video_preview);
        progress_bar = view.findViewById(R.id.progress_bar);
        progress_bar.setVisibility(View.INVISIBLE);

        surfaceView.setOnClickListener((v) -> {

            if (mediaController != null) {
                if (mediaController.isShowing()) {
                    mediaController.setVisibility(View.INVISIBLE);
                    mediaController.hide();
                } else {
                    mediaController.setVisibility(View.VISIBLE);
                    mediaController.show();
                }
            }

        });


        Bundle args = getArguments();
        checkNotNull(args);

        uri = args.getParcelable(URI);
        checkNotNull(uri);

        mediaPlayer = new MediaPlayer();

        mediaController.setMediaPlayer(VideoDialogFragment.this);
        mediaController.setAnchorView(layoutView);


        ((ViewGroup) mediaController.getParent()).removeView(mediaController);

        ((FrameLayout) view.findViewById(R.id.videoViewWrapper))
                .addView(mediaController);
        mediaController.setVisibility(View.INVISIBLE);

        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);


        SurfaceHolder holder = surfaceView.getHolder();
        holder.addCallback(this);


        builder.setView(view);


        AlertDialog dialog = builder.create();

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        Window window = dialog.getWindow();
        if (window != null) {
            window.getAttributes().windowAnimations = R.style.DialogTopAnimation;
            window.getAttributes().gravity = Gravity.TOP | Gravity.CENTER;
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
        return dialog;

    }

    @Override
    public void onStop() {
        super.onStop();
        teardown();
        try {
            stopped.set(true);
            dismissAllowingStateLoss();
        } catch (IllegalStateException e) {
            // ignore exception
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }


    @Override
    public void start() {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.start();
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    @Override
    public void pause() {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.pause();
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    @Override
    public int getDuration() {
        try {
            if (mediaPlayer != null) {
                return mediaPlayer.getDuration();
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
        return 0;
    }

    @Override
    public int getCurrentPosition() {
        try {
            if (mediaPlayer != null) {
                return mediaPlayer.getCurrentPosition();
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
        return 0;
    }

    @Override
    public void seekTo(int i) {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.seekTo(i);
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }

    }

    @Override
    public boolean isPlaying() {
        try {
            if (mediaPlayer != null) {
                return mediaPlayer.isPlaying();
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
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
        return uri.hashCode();
    }


    private void teardown() {
        try {
            mediaController.hide();
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
        try {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
            }

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
        try {
            if (mediaPlayer != null) {
                mediaPlayer.release();
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        } finally {
            mediaPlayer = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {

        if (mediaPlayer != null) {
            mediaPlayer.setDisplay(surfaceHolder);

            prepare();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mediaPlayer == null) {
            dismissAllowingStateLoss();
        }

    }


    private void prepare() {

        try {

            progress_bar.setVisibility(View.VISIBLE);
            mediaPlayer.setScreenOnWhilePlaying(true);
            mediaPlayer.setOnCompletionListener(this);
            mediaPlayer.setOnBufferingUpdateListener(this);
            mediaPlayer.setDataSource(mContext, uri);
            //mediaPlayer.setDataSource(dataSource);

            mediaPlayer.setOnPreparedListener((mp) -> {
                try {
                    progress_bar.setVisibility(View.INVISIBLE);
                    handleFullRatio();
                    mediaController.hide();
                    mediaPlayer.start();
                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
                    final EVENTS events = EVENTS.getInstance(mContext);
                    Preferences.error(events, mContext.getString(R.string.video_failure,
                            e.getLocalizedMessage()));
                    dismiss();
                }

            });
            mediaPlayer.prepare();

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
            final EVENTS events = EVENTS.getInstance(mContext);
            Preferences.error(events, mContext.getString(R.string.video_failure,
                    e.getLocalizedMessage()));
            dismiss();
        }
    }


    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        surfaceHolder.removeCallback(this);
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        teardown();
        try {
            if (!stopped.get()) {
                dismissAllowingStateLoss();
            }
        } catch (IllegalStateException e) {
            // ignore exception
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mediaPlayer, int percent) {
        mCurrentBufferPercentage = percent;
    }
}
