package threads.server;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import static androidx.core.util.Preconditions.checkNotNull;


public class ThreadsDialogFragment extends DialogFragment {
    static final String TAG = ThreadsDialogFragment.class.getSimpleName();

    private ThreadsDialogFragment.ActionListener mActionListener;
    private long mLastClickTime = 0;
    private int mBackgroundColor;
    private Context mContext;

    static ThreadsDialogFragment newInstance() {

        return new ThreadsDialogFragment();
    }

    private static int getThemeBackgroundColor(final Context context) {
        final TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.windowBackground, value, true);
        return value.data;
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

        try {
            mActionListener = (ThreadsDialogFragment.ActionListener) getActivity();

            mBackgroundColor = getThemeBackgroundColor(context);
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        Activity activity = getActivity();
        checkNotNull(activity);
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);


        LayoutInflater inflater = activity.getLayoutInflater();


        @SuppressLint("InflateParams") View view = inflater.inflate(R.layout.threads_action_view, null);

        view.setBackgroundColor(mBackgroundColor);


        TextView menu_file_cid = view.findViewById(R.id.menu_file_cid);

        menu_file_cid.setOnClickListener((v) -> {

            try {
                if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                    return;
                }

                mLastClickTime = SystemClock.elapsedRealtime();
                mActionListener.clickUpload();
            } finally {
                dismiss();
            }


        });


        TextView menu_scan_cid = view.findViewById(R.id.menu_scan_cid);

        PackageManager pm = mContext.getPackageManager();
        if (!pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            menu_scan_cid.setVisibility(View.GONE);
        }
        menu_scan_cid.setOnClickListener((v) -> {

            try {
                if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                    return;
                }

                mLastClickTime = SystemClock.elapsedRealtime();
                mActionListener.clickMultihash();
            } finally {
                dismiss();
            }


        });


        TextView menu_edit_cid = view.findViewById(R.id.menu_edit_cid);

        menu_edit_cid.setOnClickListener((v) -> {

            try {
                if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                    return;
                }

                mLastClickTime = SystemClock.elapsedRealtime();

                mActionListener.clickEditMultihash();

            } finally {
                dismiss();
            }

        });


        builder.setView(view);
        AlertDialog dialog = builder.create();

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        Window window = dialog.getWindow();
        if (window != null) {
            window.getAttributes().windowAnimations = R.style.DialogBottomAnimation;
            window.getAttributes().gravity = Gravity.BOTTOM | Gravity.CENTER;
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
        return dialog;
    }

    public interface ActionListener {

        void clickMultihash();

        void clickEditMultihash();

        void clickUpload();
    }
}