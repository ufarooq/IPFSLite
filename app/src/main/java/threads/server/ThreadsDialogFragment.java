package threads.server;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
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
    private static final String EDIT_CID_ACTIVE = "EDIT_CID_ACTIVE";
    private static final String SCAN_CID_ACTIVE = "SCAN_CID_ACTIVE";
    private static final String FILE_CID_ACTIVE = "FILE_CID_ACTIVE";

    private ThreadsDialogFragment.ActionListener actionListener;
    private long mLastClickTime = 0;
    private int backgroundColor;
    static ThreadsDialogFragment newInstance(boolean editActive,
                                             boolean scanActive,
                                             boolean fileActive) {

        Bundle bundle = new Bundle();

        bundle.putBoolean(EDIT_CID_ACTIVE, editActive);
        bundle.putBoolean(SCAN_CID_ACTIVE, scanActive);
        bundle.putBoolean(FILE_CID_ACTIVE, fileActive);
        ThreadsDialogFragment fragment = new ThreadsDialogFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    public static int getThemeBackgroundColor(final Context context) {
        final TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.windowBackground, value, true);
        return value.data;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);


        try {
            actionListener = (ThreadsDialogFragment.ActionListener) getActivity();

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
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);


        LayoutInflater inflater = activity.getLayoutInflater();

        Bundle args = getArguments();
        checkNotNull(args);
        boolean editActive = args.getBoolean(EDIT_CID_ACTIVE);
        boolean scanActive = args.getBoolean(SCAN_CID_ACTIVE);
        boolean fileActive = args.getBoolean(FILE_CID_ACTIVE);


        @SuppressWarnings("all")
        View view = inflater.inflate(R.layout.threads_action_view, null);

        view.setBackgroundColor(backgroundColor);


        TextView menu_file_cid = view.findViewById(R.id.menu_file_cid);
        if (!fileActive) {
            menu_file_cid.setVisibility(View.GONE);
        } else {
            menu_file_cid.setOnClickListener((v) -> {

                try {
                    if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                        return;
                    }

                    mLastClickTime = SystemClock.elapsedRealtime();
                    actionListener.clickUpload();
                } finally {
                    dismiss();
                }


            });
        }

        TextView menu_scan_cid = view.findViewById(R.id.menu_scan_cid);
        if (!scanActive) {
            menu_scan_cid.setVisibility(View.GONE);
        } else {
            menu_scan_cid.setOnClickListener((v) -> {

                try {
                    if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                        return;
                    }

                    mLastClickTime = SystemClock.elapsedRealtime();
                    actionListener.clickMultihash();
                } finally {
                    dismiss();
                }


            });
        }


        TextView menu_edit_cid = view.findViewById(R.id.menu_edit_cid);
        if (!editActive) {
            menu_edit_cid.setVisibility(View.GONE);
        } else {
            menu_edit_cid.setOnClickListener((v) -> {

                try {
                    if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                        return;
                    }

                    mLastClickTime = SystemClock.elapsedRealtime();

                    actionListener.clickEditMultihash();

                } finally {
                    dismiss();
                }

            });
        }

        if (!editActive && !scanActive) {
            view.findViewById(R.id.row_first).setVisibility(View.GONE);
        }


        builder.setView(view);
        AlertDialog dialog = builder.create();

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        Window window = dialog.getWindow();
        if (window != null) {
            window.getAttributes().windowAnimations = threads.share.R.style.DialogBottomAnimation;
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