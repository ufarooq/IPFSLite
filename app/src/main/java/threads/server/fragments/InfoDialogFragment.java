package threads.server.fragments;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ShareCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

import java.util.Objects;

import threads.server.MainActivity;
import threads.server.R;
import threads.server.core.events.EVENTS;
import threads.server.provider.FileDocumentsProvider;

public class InfoDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {

    public static final String TAG = InfoDialogFragment.class.getSimpleName();

    private static final String QR_CODE = "QR_CODE";
    private static final String MESSAGE = "MESSAGE";
    private static final String TITLE = "TITLE";
    private String code;
    private String message;
    private Context mContext;
    private FragmentActivity mActivity;

    public static InfoDialogFragment newInstance(@NonNull String code,
                                                 @NonNull String title,
                                                 @NonNull String message) {


        Bundle bundle = new Bundle();
        bundle.putString(QR_CODE, code);
        bundle.putString(MESSAGE, message);
        bundle.putString(TITLE, title);
        InfoDialogFragment fragment = new InfoDialogFragment();
        fragment.setArguments(bundle);
        return fragment;


    }


    @Override
    public void onDetach() {
        super.onDetach();
        mContext = null;
        mActivity = null;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
        mActivity = getActivity();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        LayoutInflater inflater = LayoutInflater.from(getActivity());
        @SuppressLint("InflateParams")
        View view = inflater.inflate(R.layout.dialog_info, null);

        ImageView imageView = view.findViewById(R.id.dialog_server_info);
        Bundle bundle = getArguments();
        Objects.requireNonNull(bundle);
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

        try {
            Bitmap imageBitmap = FileDocumentsProvider.getBitmap(code);
            imageView.setImageBitmap(imageBitmap);
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage());
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);

        builder.setTitle(title)
                .setMessage(message)
                .setView(view)
                .setPositiveButton(R.string.share, this)
                .create();

        Dialog dialog = builder.create();
        Window window = dialog.getWindow();
        if (window != null) {
            window.getAttributes().windowAnimations = R.style.DialogTopAnimation;
            window.getAttributes().gravity = Gravity.TOP | Gravity.CENTER;
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        return dialog;
    }


    @Override
    public void onClick(DialogInterface dialogInterface, int which) {
        if (which == AlertDialog.BUTTON_POSITIVE) {
            shareQRCode(code, message);
        }

    }


    private void shareQRCode(@NonNull String code, @NonNull String message) {

        try {

            Uri uri = FileDocumentsProvider.getUriForBitmap(code);
            ComponentName[] names = {new ComponentName(mContext, MainActivity.class)};

            String mimeType = "image/png";
            Intent intent = ShareCompat.IntentBuilder.from(mActivity)
                    .setStream(uri)
                    .setType(mimeType)
                    .getIntent();
            intent.setAction(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_SUBJECT, code);
            intent.putExtra(Intent.EXTRA_TEXT, message);
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.setType(mimeType);
            intent.putExtra(DocumentsContract.EXTRA_EXCLUDE_SELF, true);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            if (intent.resolveActivity(mContext.getPackageManager()) != null) {
                Intent chooser = Intent.createChooser(intent, getText(R.string.share));
                chooser.putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, names);
                startActivity(chooser);
            } else {
                EVENTS.getInstance(mContext).postError(
                        getString(R.string.no_activity_found_to_handle_uri));
            }

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        } finally {
            dismiss();
        }
    }

}
