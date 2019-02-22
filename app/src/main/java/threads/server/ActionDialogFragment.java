package threads.server;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import threads.core.Preferences;

import static com.google.common.base.Preconditions.checkNotNull;

public class ActionDialogFragment extends DialogFragment {
    public static final String TAG = ActionDialogFragment.class.getSimpleName();
    private ActionListener actionListener;
    private long mLastClickTime = 0;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        try {
            actionListener = (ActionListener) getActivity();
        } catch (Throwable e) {
            Preferences.evaluateException(Preferences.EXCEPTION, e);
        }
    }


    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        Activity activity = getActivity();
        checkNotNull(activity);
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);


        LayoutInflater inflater = activity.getLayoutInflater();


        @SuppressWarnings("all")
        View view = inflater.inflate(R.layout.action_view, null);

        TextView menu_scan_peer = view.findViewById(R.id.menu_scan_peer);
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

        TextView menu_upload = view.findViewById(R.id.menu_upload);
        menu_upload.setOnClickListener((v) -> {
            try {
                if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                    return;
                }

                mLastClickTime = SystemClock.elapsedRealtime();
                actionListener.clickUploadMultihash();
            } finally {
                dismiss();
            }

        });

        TextView menu_download = view.findViewById(R.id.menu_download);
        menu_download.setOnClickListener((v) -> {

            try {
                if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                    return;
                }

                mLastClickTime = SystemClock.elapsedRealtime();
                actionListener.clickDownloadMultihash();

            } finally {
                dismiss();
            }

        });

        TextView menu_edit_peer = view.findViewById(R.id.menu_edit_peer);
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


        builder.setView(view);
        AlertDialog dialog = builder.create();

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        Window window = dialog.getWindow();
        if (window != null) {
            window.getAttributes().windowAnimations = R.style.ActionDialogAnimation;
            window.getAttributes().gravity = Gravity.BOTTOM | Gravity.CENTER;
        }
        return dialog;
    }

    public interface ActionListener {

        void clickConnectPeer();

        void clickUploadMultihash();

        void clickDownloadMultihash();

        void clickEditPeer();
    }
}
