package threads.server;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import java.util.concurrent.atomic.AtomicBoolean;

import threads.core.Preferences;

import static androidx.core.util.Preconditions.checkNotNull;

public class RTCCallingDialogFragment extends DialogFragment {

    public static final String TAG = RTCCallingDialogFragment.class.getSimpleName();
    private static final String PID = "PID";
    private static final String NAME = "NAME";
    private final AtomicBoolean triggerTimeoutCall = new AtomicBoolean(true);
    private RTCCallingDialogFragment.ActionListener mListener;
    private Context mContext;
    private RTCSoundPool soundPoolManager;

    static RTCCallingDialogFragment newInstance(@NonNull String pid, @NonNull String name) {
        checkNotNull(pid);
        checkNotNull(name);
        Bundle bundle = new Bundle();
        bundle.putString(PID, pid);
        bundle.putString(NAME, name);
        RTCCallingDialogFragment fragment = new RTCCallingDialogFragment();
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
            mListener = (RTCCallingDialogFragment.ActionListener) getActivity();
        } catch (Throwable e) {
            Preferences.evaluateException(Preferences.EXCEPTION, e);
        }
        soundPoolManager = RTCSoundPool.create(mContext, R.raw.incoming, true);
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
        builder.setTitle(getString(R.string.incoming_call));
        builder.setPositiveButton(getString(R.string.accept), (dialog, which) -> {

            soundPoolManager.stop();
            triggerTimeoutCall.set(false);
            mListener.acceptCall(pid);
            dialog.dismiss();

        });
        builder.setNegativeButton(getString(R.string.reject), (dialog, which) -> {

            soundPoolManager.stop();
            triggerTimeoutCall.set(false);
            mListener.rejectCall(pid);
            dialog.dismiss();

        });
        builder.setMessage(getString(R.string.is_calling, name));

        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setOnShowListener((dialogInterface) -> {

            Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button negative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

            positive.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    R.drawable.phone_in_talk, 0, 0, 0);
            positive.setCompoundDrawablePadding(16);

            negative.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    R.drawable.phone_reject, 0, 0, 0);
            negative.setCompoundDrawablePadding(16);


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

        void acceptCall(@NonNull String pid);

        void rejectCall(@NonNull String pid);

        void timeoutCall(@NonNull String pid);

    }
}
