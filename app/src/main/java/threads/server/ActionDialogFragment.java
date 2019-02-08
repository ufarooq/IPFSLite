package threads.server;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.Toast;

import static com.google.common.base.Preconditions.checkNotNull;

public class ActionDialogFragment extends DialogFragment {
    private static final String TAG = ActionDialogFragment.class.getSimpleName();
    private ActionListener actionListener;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            actionListener = (ActionListener) getActivity();
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage());
        }
    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        Activity activity = getActivity();
        checkNotNull(activity);
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);


        LayoutInflater inflater = activity.getLayoutInflater();


        @SuppressWarnings("all")
        View view = inflater.inflate(R.layout.action_view, null);

        ImageView menu_galary = view.findViewById(R.id.menu_connect_peer);
        menu_galary.setOnClickListener((v) -> {

            try {
                if (!DaemonService.isIpfsRunning()) {
                    Toast.makeText(getContext(),
                            R.string.daemon_server_not_running, Toast.LENGTH_LONG).show();
                } else {
                    actionListener.clickConnectPeer();
                }
            } finally {
                dismiss();
            }


        });

        ImageView menu_camera = view.findViewById(R.id.menu_upload_file);
        menu_camera.setOnClickListener((v) -> {
            try {
                if (!DaemonService.isIpfsRunning()) {
                    Toast.makeText(getContext(),
                            R.string.daemon_server_not_running, Toast.LENGTH_LONG).show();
                } else {
                    actionListener.clickUploadFile();
                }
            } finally {
                dismiss();
            }

        });

        ImageView menu_video = view.findViewById(R.id.menu_download_file);
        menu_video.setOnClickListener((v) -> {

            try {
                if (!DaemonService.isIpfsRunning()) {
                    Toast.makeText(getContext(),
                            R.string.daemon_server_not_running, Toast.LENGTH_LONG).show();
                } else {
                    actionListener.clickDownloadFile();
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
    }
}
