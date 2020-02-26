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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import threads.server.R;
import threads.server.core.events.EVENTS;
import threads.server.core.peers.PEERS;

public class NameDialogFragment extends DialogFragment {
    public static final String TAG = NameDialogFragment.class.getSimpleName();
    private static final String TITLE = "TITLE";
    private static final String PID = "PID";
    private final AtomicBoolean notPrintErrorMessages = new AtomicBoolean(false);

    private long mLastClickTime = 0;
    private TextInputLayout edit_multi_hash_layout;
    private TextInputEditText multihash;
    private Context mContext;
    private FragmentActivity mActivity;

    public static NameDialogFragment newInstance(@NonNull String pid, @NonNull String title) {

        Bundle bundle = new Bundle();
        bundle.putString(TITLE, title);
        bundle.putString(PID, pid);

        NameDialogFragment fragment = new NameDialogFragment();
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

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {


        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);

        LayoutInflater inflater = mActivity.getLayoutInflater();

        Bundle args = getArguments();
        Objects.requireNonNull(args);
        final String pid = args.getString(PID);
        Objects.requireNonNull(pid);
        String title = args.getString(TITLE);


        @SuppressLint("InflateParams")
        View view = inflater.inflate(R.layout.name_view, null);
        edit_multi_hash_layout = view.findViewById(R.id.edit_text_layout);
        edit_multi_hash_layout.setCounterEnabled(true);
        edit_multi_hash_layout.setCounterMaxLength(30);

        multihash = view.findViewById(R.id.text);
        InputFilter[] filterTitle = new InputFilter[1];
        filterTitle[0] = new InputFilter.LengthFilter(30);
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
                isValidName(getDialog());
            }
        });


        builder.setView(view)
                // Add action buttons
                .setPositiveButton(android.R.string.ok, (dialog, id) -> {

                    if (SystemClock.elapsedRealtime() - mLastClickTime < 500) {
                        return;
                    }

                    mLastClickTime = SystemClock.elapsedRealtime();

                    removeKeyboards();


                    Editable text = multihash.getText();
                    Objects.requireNonNull(text);
                    String name = text.toString();

                    name(pid, name);


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


    private void name(@NonNull String pid, @NonNull String name) {

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                PEERS.getInstance(mContext).setUserAlias(pid, name);
            } catch (Throwable e) {
                EVENTS.getInstance(mContext).exception(e);
            } finally {
                dismiss();
            }
        });


    }

    private void isValidName(Dialog dialog) {
        if (dialog instanceof AlertDialog) {
            AlertDialog alertDialog = (AlertDialog) dialog;
            Editable text = multihash.getText();
            Objects.requireNonNull(text);
            String multi = text.toString();


            boolean result = !multi.isEmpty();


            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(result);


            if (!notPrintErrorMessages.get()) {

                if (multi.isEmpty()) {
                    edit_multi_hash_layout.setError(getString(R.string.name_not_valid));
                } else {
                    edit_multi_hash_layout.setError(null);
                }

            } else {
                edit_multi_hash_layout.setError(null);
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


    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        removeKeyboards();
    }

    @Override
    public void onResume() {
        super.onResume();
        notPrintErrorMessages.set(true);
        isValidName(getDialog());
        notPrintErrorMessages.set(false);
    }

}
