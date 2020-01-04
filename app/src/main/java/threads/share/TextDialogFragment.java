package threads.share;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.Editable;
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

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.concurrent.atomic.AtomicBoolean;

import threads.server.R;

import static androidx.core.util.Preconditions.checkNotNull;

public class TextDialogFragment extends DialogFragment {
    public static final String TAG = TextDialogFragment.class.getSimpleName();
    private static final String TITLE = "TITLE";
    private final AtomicBoolean notPrintErrorMessages = new AtomicBoolean(false);
    private TextDialogFragment.ActionListener mListener;
    private long mLastClickTime = 0;
    private TextInputLayout edit_text_layout;
    private TextInputEditText text;
    private Context mContext;

    public static TextDialogFragment newInstance(@NonNull String title) {

        Bundle bundle = new Bundle();
        bundle.putString(TITLE, title);

        TextDialogFragment fragment = new TextDialogFragment();
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
            mListener = (TextDialogFragment.ActionListener) getActivity();
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        Activity activity = getActivity();
        checkNotNull(activity);


        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);


        LayoutInflater inflater = activity.getLayoutInflater();

        Bundle args = getArguments();
        checkNotNull(args);

        String title = args.getString(TITLE);


        @SuppressWarnings("all")
        View view = inflater.inflate(R.layout.text_view, null);
        edit_text_layout = view.findViewById(R.id.edit_text_layout);

        text = view.findViewById(R.id.text);

        text.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                isValid(getDialog());
            }
        });


        builder.setView(view)
                // Add action buttons
                .setPositiveButton(android.R.string.ok, (dialog, id) -> {

                    if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                        return;
                    }

                    mLastClickTime = SystemClock.elapsedRealtime();

                    removeKeyboards();


                    Editable text = this.text.getText();
                    checkNotNull(text);
                    dismiss();
                    mListener.text(text.toString());


                })
                .setTitle(title);


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


    private void isValid(Dialog dialog) {
        if (dialog instanceof AlertDialog) {
            AlertDialog alertDialog = (AlertDialog) dialog;
            Editable text = this.text.getText();
            checkNotNull(text);
            String multi = text.toString();


            boolean result = !multi.isEmpty();


            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(result);


            if (!notPrintErrorMessages.get()) {

                if (multi.isEmpty()) {
                    edit_text_layout.setError(getString(R.string.empty_document));
                } else {
                    edit_text_layout.setError(null);
                }

            } else {
                edit_text_layout.setError(null);
            }
        }
    }


    private void removeKeyboards() {
        try {
            InputMethodManager imm = (InputMethodManager)
                    mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(text.getWindowToken(), 0);
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
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
        isValid(getDialog());
        notPrintErrorMessages.set(false);
    }

    public interface ActionListener {

        void text(@NonNull String text);

    }
}
