package threads.share;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import threads.server.R;
import threads.server.provider.FileDocumentsProvider;

import static androidx.core.util.Preconditions.checkNotNull;

public class InfoDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {

    public static final String TAG = InfoDialogFragment.class.getSimpleName();

    private static final String QR_CODE = "QR_CODE";
    private static final String MESSAGE = "MESSAGE";
    private static final String TITLE = "TITLE";
    private String code;
    private String message;
    private Context mContext;
    private InfoDialogFragment.ActionListener mListener;

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


    @Override
    public void onDetach() {
        super.onDetach();
        mContext = null;
        mListener = null;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
        try {
            mListener = (InfoDialogFragment.ActionListener) getActivity();
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        LayoutInflater inflater = LayoutInflater.from(getActivity());
        @SuppressWarnings("all")
        View view = inflater.inflate(R.layout.dialog_info, null);

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

        try {
            Bitmap imageBitmap = FileDocumentsProvider.getBitmap(code);
            imageView.setImageBitmap(imageBitmap);
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage());
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
                    mListener.shareQRCode(code, message);
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

    public interface ActionListener {

        void shareQRCode(@NonNull String code, @NonNull String message);

    }
}
