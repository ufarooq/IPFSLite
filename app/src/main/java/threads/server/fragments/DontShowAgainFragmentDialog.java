package threads.server.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import threads.server.R;

import static androidx.core.util.Preconditions.checkNotNull;

public class DontShowAgainFragmentDialog extends DialogFragment {
    public static final String TAG = DontShowAgainFragmentDialog.class.getSimpleName();
    private static final String TEXT = "TEXT";
    private static final String KEY = "KEY";

    private ActionListener listener;
    private Context mContext;
    private int backgroundColor;
    private CheckBox dontShowAgain;
    private String key;

    public static DontShowAgainFragmentDialog newInstance(@NonNull String text,
                                                          @NonNull String key) {
        checkNotNull(text);
        Bundle bundle = new Bundle();
        bundle.putString(TEXT, text);
        bundle.putString(KEY, key);
        DontShowAgainFragmentDialog fragment = new DontShowAgainFragmentDialog();
        fragment.setArguments(bundle);
        return fragment;
    }

    private static int getThemeBackgroundColor(final Context context) {
        final TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.windowBackground, value, true);
        return value.data;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
        mContext = null;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
        try {
            listener = (DontShowAgainFragmentDialog.ActionListener) getActivity();
            backgroundColor = getThemeBackgroundColor(context);
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

        Bundle bundle = getArguments();
        checkNotNull(bundle);
        String text = bundle.getString(TEXT);
        checkNotNull(text);
        key = bundle.getString(KEY);
        checkNotNull(key);

        @SuppressLint("InflateParams")
        View view = inflater.inflate(R.layout.checkbox, null);


        view.setBackgroundColor(backgroundColor);


        dontShowAgain = view.findViewById(R.id.skip);
        TextView textView = view.findViewById(R.id.text);
        textView.setText(Html.fromHtml(text, Html.FROM_HTML_MODE_COMPACT));

        builder.setView(view);

        Dialog dialog = builder.create();

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        Window window = dialog.getWindow();
        if (window != null) {
            window.getAttributes().windowAnimations = R.style.DialogTopAnimation;
            window.getAttributes().gravity = Gravity.TOP | Gravity.CENTER;
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
        return dialog;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        listener.dontShowAgain(key, dontShowAgain.isChecked());
    }

    public interface ActionListener {

        void dontShowAgain(@NonNull String key, boolean dontShowAgain);

    }
}