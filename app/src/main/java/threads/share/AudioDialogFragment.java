package threads.share;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.amulyakhare.textdrawable.util.ColorGenerator;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import threads.server.R;

import static androidx.core.util.Preconditions.checkNotNull;

public class AudioDialogFragment extends DialogFragment {
    public static final String TAG = AudioDialogFragment.class.getSimpleName();

    private static final String CID_ID = "CID_ID";
    private static final String TITLE = "ARG_TITLE";
    private static final String AUTHOR = "AUTHOR";
    private static final AtomicBoolean valid = new AtomicBoolean(true);

    private MediaPlayer mediaPlayer;
    private Context mContext;
    private int backgroundColor;
    private IPFSMediaDataSource dataSource;


    public static AudioDialogFragment newInstance(@NonNull String cid,
                                                  @NonNull String title,
                                                  @NonNull String author) {
        checkNotNull(cid);
        checkNotNull(title);
        checkNotNull(author);

        Bundle bundle = new Bundle();
        bundle.putString(CID_ID, cid);
        bundle.putString(TITLE, title);
        bundle.putString(AUTHOR, author);

        AudioDialogFragment fragment = new AudioDialogFragment();
        fragment.setArguments(bundle);
        return fragment;


    }

    private static int getThemeBackgroundColor(final Context context) {
        final TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.windowBackground, value, true);
        return value.data;
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
        try {
            backgroundColor = getThemeBackgroundColor(context);
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Activity activity = getActivity();
        checkNotNull(activity);

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        LayoutInflater inflater = activity.getLayoutInflater();

        @SuppressWarnings("all")
        View view = inflater.inflate(R.layout.audio_view, null);

        view.setBackgroundColor(backgroundColor);

        TextView audio_title = view.findViewById(R.id.audio_title);
        TextView email_user = view.findViewById(R.id.user);
        ImageView image_action = view.findViewById(R.id.image_action);
        image_action.setVisibility(View.VISIBLE);
        image_action.setClickable(true);
        image_action.setImageResource(R.drawable.pause_circle);


        SeekBar mSeekBar = view.findViewById(R.id.seek_bar);
        mSeekBar.setVisibility(View.VISIBLE);


        Bundle args = getArguments();
        checkNotNull(args);

        String title = args.getString(TITLE);
        checkNotNull(title);

        String author = args.getString(AUTHOR);
        checkNotNull(author);
        String cid = args.getString(CID_ID);
        checkNotNull(cid);

        email_user.setText(author);
        int color = ColorGenerator.MATERIAL.getColor(author);
        email_user.setTextColor(color);
        audio_title.setText(title);


        mediaPlayer = new MediaPlayer();

        try {
            dataSource = new IPFSMediaDataSource(mContext, cid);
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }


        mediaPlayer.setScreenOnWhilePlaying(true);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);


        try {

            mediaPlayer.setDataSource(dataSource);


            mediaPlayer.setOnCompletionListener((mediaPlayer) -> {
                teardown();
                try {
                    dismissAllowingStateLoss();
                } catch (IllegalStateException e) {
                    // ignore exception
                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
                }
            });

            mediaPlayer.setOnPreparedListener((mp) -> {
                mp.start();


                final int duration = mp.getDuration();
                mSeekBar.setMax(duration);


                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.submit(() -> {
                    try {
                        do {
                            Thread.sleep(50);
                            activity.runOnUiThread(() -> {
                                try {
                                    if (valid.get()) {
                                        if (mp.isPlaying()) {
                                            mSeekBar.setProgress(mp.getCurrentPosition());
                                        }
                                    }
                                } catch (Throwable e) {
                                    // ignore exception
                                }
                            });
                        } while (valid.get() && mp.isPlaying());


                    } catch (Throwable e) {
                        // ignore exception
                    }
                });

            });

            mediaPlayer.prepareAsync();

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }

        builder.setView(view);


        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (mediaPlayer != null && fromUser) {
                    mediaPlayer.seekTo(progress);
                }
            }
        });


        image_action.setOnClickListener((v) -> {

            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                image_action.setImageResource(R.drawable.play_circle);
            } else {
                mediaPlayer.start();
                image_action.setImageResource(R.drawable.pause_circle);
            }

        });


        Dialog dialog = builder.create();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        return dialog;
    }

    @Override
    public void onResume() {
        super.onResume();
        valid.set(true);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        teardown();
    }

    private void teardown() {
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
            valid.set(false);
        }
    }


}

