package threads.server;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import static androidx.core.util.Preconditions.checkNotNull;

public class PeersDialogFragment extends DialogFragment {
    static final String TAG = PeersDialogFragment.class.getSimpleName();

    private int backgroundColor;
    private ActionListener actionListener;
    private long mLastClickTime = 0;

    static PeersDialogFragment newInstance() {

        return new PeersDialogFragment();

    }

    private static int getThemeBackgroundColor(final Context context) {
        final TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.windowBackground, value, true);
        return value.data;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);


        try {
            actionListener = (ActionListener) getActivity();

            backgroundColor = getThemeBackgroundColor(context);
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        Activity activity = getActivity();
        checkNotNull(activity);
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);


        LayoutInflater inflater = activity.getLayoutInflater();

        @SuppressLint("InflateParams") View view = inflater.inflate(R.layout.peers_action_view, null);

        view.setBackgroundColor(backgroundColor);

        TextView menu_scan_peer = view.findViewById(R.id.menu_scan_pid);

        menu_scan_peer.setOnClickListener((v) -> {

            try {
                if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                    return;
                }

                mLastClickTime = SystemClock.elapsedRealtime();
                actionListener.clickConnectPeer();
            } finally {
                dismiss();
            }


        });


        TextView menu_edit_peer = view.findViewById(R.id.menu_edit_pid);

        menu_edit_peer.setOnClickListener((v) -> {

            try {
                if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                    return;
                }

                mLastClickTime = SystemClock.elapsedRealtime();

                actionListener.clickEditPeer();

            } finally {
                dismiss();
            }

        });


        TextView menu_your_peer = view.findViewById(R.id.menu_your_pid);

        menu_your_peer.setOnClickListener((v) -> {

            try {
                if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                    return;
                }

                mLastClickTime = SystemClock.elapsedRealtime();

                actionListener.clickInfoPeer();

            } finally {
                dismiss();
            }

        });


        builder.setView(view);
        AlertDialog dialog = builder.create();

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        Window window = dialog.getWindow();
        if (window != null) {
            window.getAttributes().windowAnimations = R.style.DialogBottomAnimation;
            window.getAttributes().gravity = Gravity.BOTTOM | Gravity.CENTER;
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
        return dialog;
    }

    public interface ActionListener {

        void clickConnectPeer();

        void clickEditPeer();

        void clickInfoPeer();
    }
}
