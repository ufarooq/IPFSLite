package threads.server;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
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

import java.util.Hashtable;

import threads.iri.tangle.Encryption;

import static com.google.common.base.Preconditions.checkNotNull;

public class ServerInfoDialog extends DialogFragment implements DialogInterface.OnClickListener {
    public static final int QR_CODE_SIZE = 800;
    private static final String TAG = "ServerInfoDialog";
    private static final String QRCODE = "QRCODE";
    @NonNull
    private final static Hashtable<String, Bitmap> generalHashtable = new Hashtable<>();

    public ImageView imageView;

    public ServerInfoDialog() {
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
    }


    public static void show(@NonNull Activity activity,
                            @NonNull String address,
                            @NonNull String aesKey) {
        checkNotNull(activity);
        checkNotNull(address);
        try {
            String qrCode = ServerInfoDialog.getBitmap(address, aesKey);

            Bundle bundle = new Bundle();
            bundle.putString(QRCODE, qrCode);
            ServerInfoDialog fragment = new ServerInfoDialog();
            fragment.setArguments(bundle);
            fragment.show(activity.getFragmentManager(), null);
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }


    }

    private static String getBitmap(@NonNull String address, @NonNull String aesKey) {
        String qrCode = "";

        try {
            qrCode = Encryption.encrypt(address, aesKey);

            if (generalHashtable.containsKey(qrCode)) {
                generalHashtable.get(qrCode);
                return qrCode;

            }

            Bitmap bitmap = net.glxn.qrgen.android.QRCode.from(qrCode).
                    withSize(ServerInfoDialog.QR_CODE_SIZE, ServerInfoDialog.QR_CODE_SIZE).bitmap();


            generalHashtable.put(qrCode, bitmap);
            return qrCode;
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
        return qrCode;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View view = inflater.inflate(R.layout.dialog_server_info, null, false);
        imageView = view.findViewById(R.id.dialog_server_info);
        Bundle bundle = getArguments();
        String qrCode = bundle.getString(QRCODE);
        Bitmap bitmap = generalHashtable.get(qrCode);
        imageView.setImageBitmap(bitmap);

        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.daemon_server)
                .setMessage(R.string.daemon_server_access)
                .setView(view)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, this)
                .create();
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int which) {
        switch (which) {
            case AlertDialog.BUTTON_POSITIVE:
                getDialog().dismiss();
                break;
        }
    }
}
