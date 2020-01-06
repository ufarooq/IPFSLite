package threads.share;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.fragment.app.DialogFragment;

import java.io.File;

import threads.core.Preferences;
import threads.core.Singleton;
import threads.core.events.EVENTS;
import threads.ipfs.IPFS;
import threads.ipfs.api.CID;
import threads.server.R;

import static androidx.core.util.Preconditions.checkNotNull;

public class InfoDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {

    public static final String TAG = InfoDialogFragment.class.getSimpleName();

    private static final String QR_CODE = "QR_CODE";
    private static final String MESSAGE = "MESSAGE";
    private static final String TITLE = "TITLE";
    private String code;
    private String message;
    private Context mContext;

    public static InfoDialogFragment newInstance(@NonNull String code,
                                                 @NonNull String title,
                                                 @NonNull String message) {

        checkNotNull(code);
        checkNotNull(title);
        checkNotNull(message);

        Bundle bundle = new Bundle();
        bundle.putString(QR_CODE, code);
        bundle.putString(MESSAGE, message);
        bundle.putString(TITLE, title);
        InfoDialogFragment fragment = new InfoDialogFragment();
        fragment.setArguments(bundle);
        return fragment;


    }

    private static void shareQRCode(@NonNull Context context,
                                    @NonNull String code,
                                    @NonNull String message) {
        checkNotNull(context);
        checkNotNull(code);
        checkNotNull(message);
        try {
            final int timeout = Preferences.getConnectionTimeout(context);
            CID bitmap = Preferences.getBitmap(context, code);
            checkNotNull(bitmap);

            IPFS ipfs = Singleton.getInstance(context).getIpfs();

            if (ipfs != null) {

                File file = new File(ipfs.getCacheDir(), bitmap + ".png");

                if (!file.exists()) {
                    ipfs.storeToFile(file, bitmap, true, timeout, -1);
                }


                Uri uri = FileProvider.getUriForFile(
                        context, context.getApplicationContext()
                                .getPackageName() + ".provider", file);

                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, code);
                shareIntent.putExtra(Intent.EXTRA_TEXT, message);
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                shareIntent.setType("image/png");
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                context.startActivity(Intent.createChooser(shareIntent,
                        context.getResources().getText(R.string.share)));

            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mContext = null;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        LayoutInflater inflater = LayoutInflater.from(getActivity());
        @SuppressWarnings("all")
        View view = inflater.inflate(R.layout.dialog_info, null, false);

        ImageView imageView = view.findViewById(R.id.dialog_server_info);
        Bundle bundle = getArguments();
        checkNotNull(bundle);
        String title = bundle.getString(TITLE);
        message = bundle.getString(MESSAGE);
        code = bundle.getString(QR_CODE);

        TextView copy_to_clipboard = view.findViewById(R.id.copy_to_clipboard);
        copy_to_clipboard.setPaintFlags(copy_to_clipboard.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);


        copy_to_clipboard.setOnClickListener((v) -> {

            try {
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager)
                        mContext.getSystemService(Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText(
                        getString(R.string.qr_code),
                        code);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(mContext,
                            "Copied " + code + " to clipboard",
                            Toast.LENGTH_LONG).show();
                }
            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }
        });


        CID bitmap = Preferences.getBitmap(mContext, code);

        IPFS ipfs = Singleton.getInstance(mContext).getIpfs();
        int timeout = Preferences.getConnectionTimeout(mContext);

        final EVENTS events = Singleton.getInstance(mContext).getEvents();
        if (ipfs != null) {
            try {
                Bitmap imageBitmap = ThumbnailService.getImage(
                        ipfs, bitmap, timeout, true);
                imageView.setImageBitmap(imageBitmap);
            } catch (Throwable e) {
                Preferences.evaluateException(events, Preferences.EXCEPTION, e);
            }
        }

        return new AlertDialog.Builder(mContext)
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
            case AlertDialog.BUTTON_POSITIVE: {
                try {
                    shareQRCode(mContext, code, message);
                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
                }
                Dialog dialog = getDialog();
                if (dialog != null) {
                    dialog.dismiss();
                }
                break;
            }
            case AlertDialog.BUTTON_NEUTRAL: {
                Dialog dialog = getDialog();
                if (dialog != null) {
                    dialog.dismiss();
                }
                break;
            }
        }
    }
}
