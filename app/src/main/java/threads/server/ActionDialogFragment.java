package threads.server;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import static com.google.common.base.Preconditions.checkNotNull;

public class ActionDialogFragment extends DialogFragment {
    public static final String TAG = ActionDialogFragment.class.getSimpleName();
    private ActionListener actionListener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        try {
            actionListener = (ActionListener) getActivity();
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage());
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

        ImageView menu_scan_peer = view.findViewById(R.id.menu_scan_peer);
        menu_scan_peer.setOnClickListener((v) -> {

            try {
                actionListener.clickConnectPeer();
            } finally {
                dismiss();
            }


        });

        ImageView menu_upload = view.findViewById(R.id.menu_upload);
        menu_upload.setOnClickListener((v) -> {
            try {
                actionListener.clickUploadFile();
            } finally {
                dismiss();
            }

        });

        ImageView menu_download = view.findViewById(R.id.menu_download);
        menu_download.setOnClickListener((v) -> {

            try {
                if (!DaemonService.DAEMON_RUNNING.get()) {
                    Toast.makeText(getContext(),
                            R.string.daemon_server_not_running, Toast.LENGTH_LONG).show();
                } else {
                    actionListener.clickDownloadFile();
                }
            } finally {
                dismiss();
            }

        });

        ImageView menu_edit_peer = view.findViewById(R.id.menu_edit_peer);
        menu_edit_peer.setOnClickListener((v) -> {

            try {
                if (!DaemonService.DAEMON_RUNNING.get()) {
                    Toast.makeText(getContext(),
                            R.string.daemon_server_not_running, Toast.LENGTH_LONG).show();
                } else {
                    actionListener.clickEditPeer();
                }
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

        void clickUploadFile();

        void clickDownloadFile();

        void clickEditPeer();
    }
}
