package threads.server.fragments;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import threads.ipfs.CID;
import threads.ipfs.Multihash;
import threads.server.R;
import threads.server.core.events.EVENTS;
import threads.server.services.DownloaderService;
import threads.server.utils.CodecDecider;
import threads.server.work.ConnectionWorker;

public class EditMultihashDialogFragment extends DialogFragment {
    public static final String TAG = EditMultihashDialogFragment.class.getSimpleName();
    private static final int MULTIHASH_SIZE = 128;
    private static final int CLICK_OFFSET = 500;
    private final AtomicBoolean notPrintErrorMessages = new AtomicBoolean(false);

    private long mLastClickTime = 0;
    private TextInputLayout edit_multihash_layout;
    private TextInputEditText multihash;
    private Context mContext;
    private FragmentActivity mActivity;

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


    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {


        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);


        LayoutInflater inflater = mActivity.getLayoutInflater();


        @SuppressLint("InflateParams") View view = inflater.inflate(R.layout.edit_view, null);
        edit_multihash_layout = view.findViewById(R.id.edit_multihash_layout);
        edit_multihash_layout.setCounterEnabled(true);
        edit_multihash_layout.setCounterMaxLength(MULTIHASH_SIZE);

        multihash = view.findViewById(R.id.multihash);
        InputFilter[] filterTitle = new InputFilter[1];
        filterTitle[0] = new InputFilter.LengthFilter(MULTIHASH_SIZE);
        multihash.setFilters(filterTitle);

        multihash.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                isValidMultihash(getDialog());
            }
        });


        builder.setView(view)
                // Add action buttons
                .setPositiveButton(android.R.string.ok, (dialog, id) -> {

                    if (SystemClock.elapsedRealtime() - mLastClickTime < CLICK_OFFSET) {
                        return;
                    }

                    mLastClickTime = SystemClock.elapsedRealtime();

                    removeKeyboards();


                    Editable text = multihash.getText();
                    Objects.requireNonNull(text);
                    String hash = text.toString();

                    downloadMultihash(hash);


                })

                .setTitle(getString(R.string.content_id));


        Dialog dialog = builder.create();


        Window window = dialog.getWindow();
        if (window != null) {
            window.getAttributes().windowAnimations = R.style.DialogTopAnimation;
            window.getAttributes().gravity = Gravity.TOP | Gravity.CENTER;
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }


        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        return dialog;
    }


    private void isValidMultihash(Dialog dialog) {
        if (dialog instanceof AlertDialog) {
            AlertDialog alertDialog = (AlertDialog) dialog;
            Editable text = multihash.getText();
            Objects.requireNonNull(text);
            String multi = text.toString();


            boolean result = !multi.isEmpty();

            if (result) {
                try {
                    Multihash.fromBase58(multi);
                    result = true;
                } catch (Throwable e) {
                    result = false;
                }
            }


            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(result);


            if (!notPrintErrorMessages.get()) {

                if (multi.isEmpty()) {
                    edit_multihash_layout.setError(getString(R.string.multihash_not_valid));
                } else {
                    edit_multihash_layout.setError(null);
                }

            } else {
                edit_multihash_layout.setError(null);
            }
        }
    }


    private void removeKeyboards() {

        try {
            InputMethodManager imm = (InputMethodManager)
                    mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(multihash.getWindowToken(), 0);
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }

    }

    private void downloadMultihash(@NonNull String codec) {

        try {
            CodecDecider codecDecider = CodecDecider.evaluate(codec);
            if (codecDecider.getCodex() == CodecDecider.Codec.MULTIHASH ||
                    codecDecider.getCodex() == CodecDecider.Codec.URI) {

                String multihash = codecDecider.getMultihash();
                ConnectionWorker.connect(mContext, true);
                DownloaderService.download(mContext, CID.create(multihash));
            } else {
                EVENTS.getInstance(mContext).postError(getString(R.string.codec_not_supported));
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        } finally {
            dismiss();
        }
    }


    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        removeKeyboards();
    }

    @Override
    public void onResume() {
        super.onResume();
        notPrintErrorMessages.set(true);
        isValidMultihash(getDialog());
        notPrintErrorMessages.set(false);
    }

}
