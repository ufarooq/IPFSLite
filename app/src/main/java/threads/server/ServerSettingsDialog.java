package threads.server;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import static com.google.common.base.Preconditions.checkNotNull;

public class ServerSettingsDialog extends DialogFragment implements DialogInterface.OnClickListener {
    private static final String TAG = "ServerInfoDialog";


    public ServerSettingsDialog() {
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
    }


    public static void show(@NonNull Activity activity) {
        checkNotNull(activity);

        try {

            Bundle bundle = new Bundle();

            ServerInfoDialog fragment = new ServerInfoDialog();
            fragment.setArguments(bundle);
            fragment.show(activity.getFragmentManager(), null);
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }


    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View view = inflater.inflate(R.layout.dialog_server_settings, null, false);

        Bundle bundle = getArguments();

        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.tangle_server_settings)
                .setView(view)
                .setCancelable(true)
                .setNeutralButton(android.R.string.ok, this)
                .create();
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int which) {
        switch (which) {
            case AlertDialog.BUTTON_NEUTRAL:
                getDialog().dismiss();
                break;
        }
    }
}
