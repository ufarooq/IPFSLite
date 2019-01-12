package threads.server;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Hashtable;

import static com.google.common.base.Preconditions.checkNotNull;

public class InfoDialog extends DialogFragment implements DialogInterface.OnClickListener {
    private static final int QR_CODE_SIZE = 600;
    private static final String TAG = InfoDialog.class.getSimpleName();
    @SuppressWarnings("SpellCheckingInspection")
    private static final String QRCODE = "QRCODE";
    private static final String MESSAGE = "MESSAGE";
    private static final String TITLE = "TITLE";
    @NonNull
    private final static Hashtable<String, Bitmap> bitmaps = new Hashtable<>();
    private String code;
    private String message;


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


    public static File getStorageFile(@NonNull Context context, @NonNull String name) {
        checkNotNull(context);
        checkNotNull(name);
        File dir = context.getExternalCacheDir();
        File file = new File(dir, name);
        if (!file.exists()) {
            try {
                if (!file.createNewFile()) {
                    throw new RuntimeException("File couldn't be created.");
                }
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
        return file;
    }

    private static void shareQRCode(@NonNull Context context,
                                    @NonNull String code,
                                    @NonNull String message) {
        checkNotNull(context);
        checkNotNull(code);
        checkNotNull(message);
        try {
            Bitmap bitmap = bitmaps.get(code);

            File file = getStorageFile(context, code + ".png");

            FileOutputStream fileOutputStream = new FileOutputStream(file);

            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();
            if (file.exists()) {
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, code);
                shareIntent.putExtra(Intent.EXTRA_TEXT, message);
                shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
                shareIntent.setType("image/png");
                context.startActivity(Intent.createChooser(shareIntent,
                        context.getResources().getText(R.string.share)));
            }

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View view = inflater.inflate(R.layout.dialog_server_info, null, false);
        ImageView imageView = view.findViewById(R.id.dialog_server_info);
        Bundle bundle = getArguments();
        String title = bundle.getString(TITLE);
        message = bundle.getString(MESSAGE);
        code = bundle.getString(QRCODE);
        Bitmap bitmap = bitmaps.get(code);
        imageView.setImageBitmap(bitmap);

        return new AlertDialog.Builder(getActivity())
                .setTitle(title)
                .setMessage(message)
                .setView(view)
                .setCancelable(false)
                .setNeutralButton(android.R.string.ok, this)
                .setPositiveButton(R.string.share, this)
                .create();
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int which) {
        switch (which) {
            case AlertDialog.BUTTON_POSITIVE:

                shareQRCode(getActivity(), code, message);
                getDialog().dismiss();
                break;
            case AlertDialog.BUTTON_NEUTRAL:
                getDialog().dismiss();
                break;
        }
    }
}
