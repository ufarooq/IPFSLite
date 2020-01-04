package threads.share;

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
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import threads.server.R;

import static androidx.core.util.Preconditions.checkNotNull;

public class NoteActionDialogFragment extends DialogFragment {
    public static final String TAG = NoteActionDialogFragment.class.getSimpleName();
    private static final String IDX = "IDX";
    private static final String INFO_ACTIVE = "INFO_ACTIVE";
    private static final String DELETE_ACTIVE = "DELETE_ACTIVE";
    private static final String DOWNLOAD_ACTIVE = "DOWNLOAD_ACTIVE";
    private static final String BLOCKED_ACTIVE = "BLOCKED_ACTIVE";
    private static final String BLOCKED_VALUE = "BLOCKED_VALUE";
    private static final String VIEW_ACTIVE = "VIEW_ACTIVE";
    private static final String TANGLE_ACTIVE = "TANGLE_ACTIVE";

    private ActionListener actionListener;
    private long mLastClickTime = 0;
    private int backgroundColor;


    public static NoteActionDialogFragment newInstance(long idx,
                                                       boolean infoActive,
                                                       boolean viewActive,
                                                       boolean tangleActive,
                                                       boolean downloadActive,
                                                       boolean deleteActive,
                                                       boolean blockedActive,
                                                       boolean blockedValue) {

        Bundle bundle = new Bundle();
        bundle.putLong(IDX, idx);
        bundle.putBoolean(INFO_ACTIVE, infoActive);
        bundle.putBoolean(DELETE_ACTIVE, deleteActive);
        bundle.putBoolean(DOWNLOAD_ACTIVE, downloadActive);
        bundle.putBoolean(VIEW_ACTIVE, viewActive);
        bundle.putBoolean(TANGLE_ACTIVE, tangleActive);
        bundle.putBoolean(BLOCKED_ACTIVE, blockedActive);
        bundle.putBoolean(BLOCKED_VALUE, blockedValue);

        NoteActionDialogFragment fragment = new NoteActionDialogFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    private static int getThemeBackgroundColor(final Context context) {
        final TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.windowBackground, value, true);
        return value.data;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        try {
            actionListener = (ActionListener) getActivity();
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
        long idx = args.getLong(IDX);
        boolean infoActive = args.getBoolean(INFO_ACTIVE);
        boolean deleteActive = args.getBoolean(DELETE_ACTIVE);
        boolean downloadActive = args.getBoolean(DOWNLOAD_ACTIVE);
        boolean viewActive = args.getBoolean(VIEW_ACTIVE);
        boolean tangleActive = args.getBoolean(TANGLE_ACTIVE);
        boolean blockedActive = args.getBoolean(BLOCKED_ACTIVE);
        boolean blockedValue = args.getBoolean(BLOCKED_VALUE);

        @SuppressWarnings("all")
        View view = inflater.inflate(R.layout.action_note_view, null);

        view.setBackgroundColor(backgroundColor);

        TextView menu_info = view.findViewById(R.id.menu_info);
        if (!infoActive) {
            menu_info.setVisibility(View.GONE);
        } else {
            menu_info.setOnClickListener((v) -> {
                try {
                    if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                        return;
                    }
                    mLastClickTime = SystemClock.elapsedRealtime();
                    actionListener.clickInfo(idx);
                } finally {
                    dismiss();
                }
            });
        }
        TextView menu_view = view.findViewById(R.id.menu_view);
        if (!viewActive) {
            menu_view.setVisibility(View.GONE);
        } else {
            menu_view.setOnClickListener((v) -> {

                try {
                    if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                        return;
                    }
                    mLastClickTime = SystemClock.elapsedRealtime();
                    actionListener.clickView(idx);
                } finally {
                    dismiss();
                }

            });
        }

        TextView menu_delete = view.findViewById(R.id.menu_delete);
        if (!deleteActive) {
            menu_delete.setVisibility(View.GONE);
        } else {
            menu_delete.setOnClickListener((v) -> {

                try {
                    if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                        return;
                    }
                    mLastClickTime = SystemClock.elapsedRealtime();
                    actionListener.clickDelete(idx);
                } finally {
                    dismiss();
                }
            });
        }

        TextView menu_tangle = view.findViewById(R.id.menu_tangle);
        if (!tangleActive) {
            menu_tangle.setVisibility(View.GONE);
        } else {
            menu_tangle.setOnClickListener((v) -> {

                try {
                    if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                        return;
                    }
                    mLastClickTime = SystemClock.elapsedRealtime();
                    actionListener.clickTangle(idx);
                } finally {
                    dismiss();
                }

            });
        }


        TextView menu_download = view.findViewById(R.id.menu_download);
        if (!downloadActive) {
            menu_download.setVisibility(View.GONE);
        } else {
            menu_download.setOnClickListener((v) -> {

                try {
                    if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                        return;
                    }
                    mLastClickTime = SystemClock.elapsedRealtime();
                    actionListener.clickDownload(idx);
                } finally {
                    dismiss();
                }

            });
        }


        final CheckBox menu_blocked = view.findViewById(R.id.menu_blocked);
        if (!blockedActive) {
            menu_blocked.setVisibility(View.GONE);
        } else {
            menu_blocked.setChecked(blockedValue);

            menu_blocked.setOnClickListener((v) -> {

                try {
                    if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                        return;
                    }
                    mLastClickTime = SystemClock.elapsedRealtime();

                    actionListener.clickBlocked(idx, menu_blocked.isChecked());
                } finally {
                    dismiss();
                }

            });
        }
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

        void clickDownload(long idx);

        void clickInfo(long idx);

        void clickDelete(long idx);

        void clickChat(long idx);

        void clickView(long idx);

        void clickTangle(long idx);

        void clickBlocked(long idx, boolean checked);
    }
}