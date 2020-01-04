package threads.share;


import android.app.Dialog;
import android.content.Context;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import threads.server.R;

import static androidx.core.util.Preconditions.checkNotNull;

public class VoiceDialogFragment extends DialogFragment implements View.OnClickListener {
    public static final String TAG = VoiceDialogFragment.class.getSimpleName();
    private final AtomicBoolean record = new AtomicBoolean(false);
    private final int[] amplitudes = new int[100];
    private final Handler mHandler = new Handler();
    private TextView mTimerTextView;
    private ImageView image_action;
    private MediaRecorder mRecorder;
    private long mStartTime = 0;
    private int i = 0;
    private final Runnable mTickExecutor = new Runnable() {
        @Override
        public void run() {
            tick();
            mHandler.postDelayed(mTickExecutor, 100);
        }
    };
    private File mOutputFile;
    private long mLastClickTime = 0;

    private ActionListener mListener;
    private Context mContext;

    @NonNull
    private static File getCacheFile(@NonNull Context context, @NonNull String name) {
        checkNotNull(name);
        File dir = context.getCacheDir();
        File file = new File(dir, name);
        if (!file.exists()) {
            try {
                if (!file.createNewFile()) {
                    throw new RuntimeException("File couldn't be created.");
                }
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
        return file;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mContext = null;
        mListener = null;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
        try {
            mListener = (ActionListener) getActivity();
        } catch (Throwable e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        FragmentActivity activity = getActivity();
        checkNotNull(activity);
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);

        LayoutInflater inflater = activity.getLayoutInflater();

        @SuppressWarnings("all")
        View view = inflater.inflate(R.layout.audio_recording, null);


        String name = mContext.getString(R.string.voice_recording) + "_" + (int) (Math.random() * 10000 + 1);
        final String extension = "m4a";
        String filename = name + "." + extension;
        mOutputFile = getCacheFile(activity, filename);

        this.mTimerTextView = view.findViewById(R.id.timer);
        this.image_action = view.findViewById(R.id.image_action);
        this.image_action.setOnClickListener(this);
        this.image_action.setClickable(true);

        builder.setView(view)
                // Add action buttons
                .setPositiveButton(android.R.string.ok, (dialog, id) -> {

                    // mis-clicking prevention, using threshold of 1000 ms
                    if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                        return;
                    }

                    mLastClickTime = SystemClock.elapsedRealtime();


                    if (mRecorder != null) {
                        stopRecording();
                    }
                    if (mOutputFile.length() > 0) {
                        mListener.storeAudio(mOutputFile);
                    }
                    dismiss();

                })
                .setNegativeButton(android.R.string.cancel, (dialog, id) -> {

                    // mis-clicking prevention, using threshold of 1000 ms
                    if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                        return;
                    }

                    mLastClickTime = SystemClock.elapsedRealtime();
                    if (mRecorder != null) {
                        stopRecording();
                    }


                    dismiss();

                });


        Dialog dialog = builder.create();

        dialog.setCanceledOnTouchOutside(false);

        return dialog;
    }

    private boolean startRecording() {
        mRecorder = new MediaRecorder();
        mRecorder.setAudioChannels(2);
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.HE_AAC);
        mRecorder.setAudioEncodingBitRate(48000);
        mRecorder.setAudioSamplingRate(16000);
        mRecorder.setOutputFile(mOutputFile.getAbsolutePath());

        try {
            mRecorder.prepare();
            mRecorder.start();
            mStartTime = SystemClock.elapsedRealtime();
            mHandler.postDelayed(mTickExecutor, 100);
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    private void stopRecording() {
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
        mStartTime = 0;
        mHandler.removeCallbacks(mTickExecutor);
    }

    private void tick() {
        long time = (mStartTime < 0) ? 0 : (SystemClock.elapsedRealtime() - mStartTime);
        int minutes = (int) (time / 60000);
        int seconds = (int) (time / 1000) % 60;
        int milliseconds = (int) (time / 100) % 10;
        String text = "" + minutes + ":" + (seconds < 10 ? "0" + seconds : seconds) + "." + milliseconds;
        mTimerTextView.setText(text);
        if (mRecorder != null) {
            amplitudes[i] = mRecorder.getMaxAmplitude();


            if (i >= amplitudes.length - 1) {
                i = 0;
            } else {
                ++i;
            }
        }
    }

    @Override
    public void onClick(View view) {

        if (!record.get()) {
            if (!startRecording()) {
                image_action.setEnabled(false);
            } else {
                image_action.setImageResource(R.drawable.accent_stop_circle);
                record.set(true);
            }
        } else {
            stopRecording();
            image_action.setImageResource(R.drawable.record);
            record.set(false);
        }

    }

    public interface ActionListener {
        void storeAudio(@NonNull File file);
    }
}