package threads.share;

import android.app.Activity;
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
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.concurrent.atomic.AtomicBoolean;

import threads.server.R;

import static androidx.core.util.Preconditions.checkNotNull;

public class NameLoginDialogFragment extends DialogFragment {
    public static final String TAG = NameLoginDialogFragment.class.getSimpleName();
    private static final String NUM_LETTERS = "NUM_LETTERS";

    private final AtomicBoolean notPrintErrorMessages = new AtomicBoolean(false);
    private long mLastClickTime = 0;
    private TextInputLayout text_name_layout;
    private TextInputEditText name;
    private LoginListener mListener;
    private Context mContext;

    public static NameLoginDialogFragment newInstance(int numLetters) {

        Bundle bundle = new Bundle();
        bundle.putInt(NUM_LETTERS, numLetters);

        NameLoginDialogFragment fragment = new NameLoginDialogFragment();
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
            mListener = (LoginListener) getActivity();
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Activity activity = getActivity();
        checkNotNull(activity);

        Bundle args = getArguments();
        checkNotNull(args);
        int numLetters = args.getInt(NUM_LETTERS);

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);

        LayoutInflater inflater = activity.getLayoutInflater();

        @SuppressWarnings("all")
        View view = inflater.inflate(R.layout.name_login_view, null);
        text_name_layout = view.findViewById(R.id.text_name_layout);
        text_name_layout.setCounterEnabled(true);
        text_name_layout.setCounterMaxLength(numLetters);

        name = view.findViewById(R.id.text);
        InputFilter[] filterTitle = new InputFilter[1];
        filterTitle[0] = new InputFilter.LengthFilter(numLetters);
        name.setFilters(filterTitle);

        name.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                isValidLogin(getDialog());
            }
        });


        builder.setView(view)
                // Add action buttons
                .setPositiveButton(android.R.string.ok, (dialog, id) -> {

                    if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                        return;
                    }

                    mLastClickTime = SystemClock.elapsedRealtime();
                    // This removes all input keyboards from user_alias edit views
                    removeKeyboards();


                    Editable text = name.getText();
                    checkNotNull(text);
                    String alias = text.toString();
                    dismiss();
                    mListener.login(alias);


                })
                .setNegativeButton(android.R.string.cancel, (dialog, id) -> {

                    if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                        return;
                    }

                    mLastClickTime = SystemClock.elapsedRealtime();

                    removeKeyboards();
                    dismiss();
                    mListener.dismissLogin();


                })
                .setTitle(getString(R.string.login));


        Dialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        Window window = dialog.getWindow();
        if (window != null) {
            window.getAttributes().windowAnimations = R.style.DialogTopAnimation;
            window.getAttributes().gravity = Gravity.TOP | Gravity.CENTER;
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        return dialog;
    }

    private void isValidLogin(Dialog dialog) {
        if (dialog instanceof AlertDialog) {
            AlertDialog alertDialog = (AlertDialog) dialog;
            Editable text = name.getText();
            checkNotNull(text);
            String displayName = text.toString();

            boolean result = !displayName.isEmpty();

            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(result);


            if (!notPrintErrorMessages.get()) {

                if (displayName.isEmpty()) {
                    text_name_layout.setError(getString(R.string.login_name_error));
                } else {
                    text_name_layout.setError(null);
                }

            } else {
                text_name_layout.setError(null);
            }
        }
    }


    private void removeKeyboards() {
        try {

            InputMethodManager imm = (InputMethodManager)
                    mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(name.getWindowToken(), 0);
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
        isValidLogin(getDialog());
        notPrintErrorMessages.set(false);
    }

    public interface LoginListener {
        void login(@NonNull String alias);

        void dismissLogin();
    }
}
