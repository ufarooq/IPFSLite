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

public class ThreadActionDialogFragment extends DialogFragment {
    public static final String TAG = ThreadActionDialogFragment.class.getSimpleName();
    private static final String IDX = "IDX";
    private static final String INFO_ACTIVE = "INFO_ACTIVE";
    private static final String DELETE_ACTIVE = "DELETE_ACTIVE";
    private static final String VIEW_ACTIVE = "VIEW_ACTIVE";
    private static final String SHARE_ACTIVE = "SHARE_ACTIVE";
    private static final String SEND_ACTIVE = "SEND_ACTIVE";
    private static final String COPY_ACTIVE = "COPY_ACTIVE";
    private static final String PUBLISH_ACTIVE = "PUBLISH_ACTIVE";
    private static final String PUBLISH_VALUE = "PUBLISH_VALUE";

    private ActionListener mListener;
    private long mLastClickTime = 0;
    private int backgroundColor;
    private Context mContext;

    public static ThreadActionDialogFragment newInstance(long idx,
                                                         boolean infoActive,
                                                         boolean viewActive,
                                                         boolean deleteActive,
                                                         boolean shareActive,
                                                         boolean sendActive,
                                                         boolean copyActive,
                                                         boolean publishActive,
                                                         boolean publishValue) {

        Bundle bundle = new Bundle();
        bundle.putLong(IDX, idx);
        bundle.putBoolean(INFO_ACTIVE, infoActive);
        bundle.putBoolean(DELETE_ACTIVE, deleteActive);
        bundle.putBoolean(VIEW_ACTIVE, viewActive);
        bundle.putBoolean(SHARE_ACTIVE, shareActive);
        bundle.putBoolean(SEND_ACTIVE, sendActive);
        bundle.putBoolean(COPY_ACTIVE, copyActive);
        bundle.putBoolean(PUBLISH_ACTIVE, publishActive);
        bundle.putBoolean(PUBLISH_VALUE, publishValue);
        ThreadActionDialogFragment fragment = new ThreadActionDialogFragment();
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
        mContext = null;
        mListener = null;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
        try {
            mListener = (ThreadActionDialogFragment.ActionListener) getActivity();
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
        Bundle args = getArguments();
        checkNotNull(args);
        long idx = args.getLong(IDX);
        boolean infoActive = args.getBoolean(INFO_ACTIVE);
        boolean deleteActive = args.getBoolean(DELETE_ACTIVE);
        boolean viewActive = args.getBoolean(VIEW_ACTIVE);
        boolean shareActive = args.getBoolean(SHARE_ACTIVE);
        boolean sendActive = args.getBoolean(SEND_ACTIVE);
        boolean copyActive = args.getBoolean(COPY_ACTIVE);
        boolean publishActive = args.getBoolean(PUBLISH_ACTIVE);
        boolean publishValue = args.getBoolean(PUBLISH_VALUE);

        @SuppressWarnings("all")
        View view = inflater.inflate(R.layout.action_thread_view, null);

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
                    mListener.clickThreadInfo(idx);
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
                    mListener.clickThreadDelete(idx);
                } finally {
                    dismiss();
                }
            });
        }


        TextView menu_share = view.findViewById(R.id.menu_share);
        if (!shareActive) {
            menu_share.setVisibility(View.GONE);
        } else {
            menu_share.setOnClickListener((v) -> {

                try {
                    if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                        return;
                    }
                    mLastClickTime = SystemClock.elapsedRealtime();
                    mListener.clickThreadShare(idx);
                } finally {
                    dismiss();
                }

            });
        }

        TextView menu_send = view.findViewById(R.id.menu_send);
        if (!sendActive) {
            menu_send.setVisibility(View.GONE);
        } else {
            menu_send.setOnClickListener((v) -> {

                try {
                    if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                        return;
                    }
                    mLastClickTime = SystemClock.elapsedRealtime();
                    mListener.clickThreadSend(idx);
                } finally {
                    dismiss();
                }

            });
        }

        final CheckBox menu_publish = view.findViewById(R.id.menu_publish);
        if (!publishActive) {
            menu_publish.setVisibility(View.GONE);
        } else {
            menu_publish.setChecked(publishValue);
            menu_publish.setOnClickListener((v) -> {

                try {
                    if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                        return;
                    }
                    mLastClickTime = SystemClock.elapsedRealtime();
                    mListener.clickThreadPublish(idx, menu_publish.isChecked());
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
                    mListener.clickThreadView(idx);
                } finally {
                    dismiss();
                }

            });
        }

        TextView menu_copy = view.findViewById(R.id.menu_copy_to);
        if (!copyActive) {
            menu_copy.setVisibility(View.GONE);
        } else {
            menu_copy.setOnClickListener((v) -> {

                try {
                    if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                        return;
                    }
                    mLastClickTime = SystemClock.elapsedRealtime();
                    mListener.clickThreadCopy(idx);
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

        void clickThreadInfo(long idx);

        void clickThreadDelete(long idx);

        void clickThreadView(long idx);

        void clickThreadShare(long idx);

        void clickThreadSend(long idx);

        void clickThreadCopy(long idx);

        void clickThreadPublish(long idx, boolean value);
    }
}
