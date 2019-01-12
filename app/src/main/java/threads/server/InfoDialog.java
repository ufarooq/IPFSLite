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

import static com.google.common.base.Preconditions.checkNotNull;

public class InfoDialog extends DialogFragment implements DialogInterface.OnClickListener {
    private static final int QR_CODE_SIZE = 800;
    private static final String TAG = InfoDialog.class.getSimpleName();
    @SuppressWarnings("SpellCheckingInspection")
    private static final String QRCODE = "QRCODE";
    private static final String MESSAGE = "MESSAGE";
    private static final String TITLE = "TITLE";
    @NonNull
    private final static Hashtable<String, Bitmap> bitmaps = new Hashtable<>();


    public InfoDialog() {
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
    }


    public static void show(@NonNull Activity activity,
                            @NonNull String code,
                            @NonNull String title,
                            @NonNull String message) {
        checkNotNull(activity);
        checkNotNull(code);
        checkNotNull(title);
        checkNotNull(message);
        try {
            String qrCode = InfoDialog.getBitmap(code);

            Bundle bundle = new Bundle();
            bundle.putString(QRCODE, qrCode);
            bundle.putString(MESSAGE, message);
            bundle.putString(TITLE, title);
            InfoDialog fragment = new InfoDialog();
            fragment.setArguments(bundle);
            fragment.show(activity.getFragmentManager(), null);
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }


    }

    private static String getBitmap(@NonNull String qrCode) {
        try {

            if (bitmaps.containsKey(qrCode)) {
                bitmaps.get(qrCode);
                return qrCode;

            }

            Bitmap bitmap = net.glxn.qrgen.android.QRCode.from(qrCode).
                    withSize(InfoDialog.QR_CODE_SIZE, InfoDialog.QR_CODE_SIZE).bitmap();


            bitmaps.put(qrCode, bitmap);
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
        ImageView imageView = view.findViewById(R.id.dialog_server_info);
        Bundle bundle = getArguments();
        String title = bundle.getString(TITLE);
        String message = bundle.getString(MESSAGE);
        String qrCode = bundle.getString(QRCODE);
        Bitmap bitmap = bitmaps.get(qrCode);
        imageView.setImageBitmap(bitmap);

        return new AlertDialog.Builder(getActivity())
                .setTitle(title)
                .setMessage(message)
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
