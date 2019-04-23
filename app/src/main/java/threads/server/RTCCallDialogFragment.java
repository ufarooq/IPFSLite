package threads.server;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import java.util.concurrent.atomic.AtomicBoolean;

import threads.core.Preferences;

import static androidx.core.util.Preconditions.checkNotNull;

public class RTCCallDialogFragment extends DialogFragment {

    public static final String TAG = RTCCallDialogFragment.class.getSimpleName();
    private static final String PID = "PID";
    private static final String NAME = "NAME";
    private final AtomicBoolean triggerTimeoutCall = new AtomicBoolean(true);
    private RTCCallDialogFragment.ActionListener mListener;
    private Context mContext;
    private RTCSoundPool soundPoolManager;

    static RTCCallDialogFragment newInstance(@NonNull String pid, @NonNull String name) {
        checkNotNull(pid);
        checkNotNull(name);
        Bundle bundle = new Bundle();
        bundle.putString(PID, pid);
        bundle.putString(NAME, name);
        RTCCallDialogFragment fragment = new RTCCallDialogFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mContext = null;
        soundPoolManager.release();
        triggerTimeoutCall.set(false);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
        try {
            mListener = (RTCCallDialogFragment.ActionListener) getActivity();
        } catch (Throwable e) {
            Preferences.evaluateException(Preferences.EXCEPTION, e);
        }
        soundPoolManager = RTCSoundPool.create(mContext, R.raw.outgoing, true);
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        Activity activity = getActivity();
        checkNotNull(activity);


        Bundle args = getArguments();
        checkNotNull(args);
        final String pid = args.getString(PID);
        checkNotNull(pid);
        final String name = args.getString(NAME);
        checkNotNull(name);
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);

        builder.setIcon(R.drawable.phone_black);
        builder.setTitle(getString(R.string.outgoing_call));
        builder.setPositiveButton(getString(R.string.abort), (dialog, which) -> {

            soundPoolManager.stop();
            triggerTimeoutCall.set(false);
            mListener.abortCall(pid);
            dialog.dismiss();

        });

        builder.setMessage(getString(R.string.calling, name));

        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);


        // hide the status bar and enter full screen mode
        checkNotNull(dialog.getWindow());
        dialog.getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        // dismiss the keyguard
        dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);


        // Set the content to appear under the system bars so that the content
        // doesn't resize when the system bars hide and show.
        dialog.getWindow().getDecorView().getRootView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        // Set the content to appear under the system bars so that the
                        // content doesn't resize when the system bars hide and show.
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        // Hide the nav bar and status bar
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);

        // immersive hack 1: set the dialog to not focusable (makes navigation ignore us adding the window)
        dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);


        dialog.setOnShowListener((dialogInterface) -> {

            Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

            positive.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    R.drawable.phone_reject, 0, 0, 0);
            positive.setCompoundDrawablePadding(16);


        });

        new Handler().postDelayed(() -> {
            try {
                if (triggerTimeoutCall.get()) {
                    mListener.timeoutCall(pid);
                }
                dialog.dismiss();
            } catch (Throwable e) {
                Preferences.evaluateException(Preferences.EXCEPTION, e);
            }

        }, 30000);
        soundPoolManager.play();

        return dialog;
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
    }


    public interface ActionListener {

        void abortCall(@NonNull String pid);

        void timeoutCall(@NonNull String pid);

    }
}
