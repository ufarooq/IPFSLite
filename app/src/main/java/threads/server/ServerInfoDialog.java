package threads.server;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import java.io.File;

import threads.iri.ServerConfig;

import static com.google.common.base.Preconditions.checkNotNull;

public class ServerInfoDialog extends DialogFragment implements DialogInterface.OnClickListener {
    private static final String TAG = "ServerInfoDialog";

    public ImageView imageView;
    private Bitmap bitmap;

    public ServerInfoDialog() {
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
    }


    private static File getExternalDirectory(Context context) {
        try {
            File cacheDir = new File(context.getExternalCacheDir(), "tangleserver");
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }
            return cacheDir;
        } catch (Exception e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
        return null;
    }


    public static void show(@NonNull Activity activity, @NonNull ServerConfig serverConfig) {
        checkNotNull(activity);
        checkNotNull(serverConfig);
        try {
            Bitmap bitmap = Application.getBitmap(serverConfig);

            Bundle bundle = new Bundle();
            bundle.putParcelable("bitmap", bitmap);
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
        View view = inflater.inflate(R.layout.dialog_server_info, null, false);
        imageView = view.findViewById(R.id.dialog_server_info);
        Bundle bundle = getArguments();
        bitmap = bundle.getParcelable("bitmap");
        imageView.setImageBitmap(bitmap);

        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.tangle_server)
                .setMessage(R.string.tangle_server_access)
                .setView(view)
                .setCancelable(false)
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
