package threads.server.fragments;

import android.annotation.SuppressLint;
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


public class UserActionDialogFragment extends DialogFragment {
    public static final String TAG = UserActionDialogFragment.class.getSimpleName();
    private static final String PID = "PID";
    private static final String INFO_ACTIVE = "INFO_ACTIVE";
    private static final String DETAILS_ACTIVE = "DETAILS_ACTIVE";
    private static final String DELETE_ACTIVE = "DELETE_ACTIVE";
    private static final String CONNECT_ACTIVE = "CONNECT_ACTIVE";
    private static final String AUTO_CONNECT_ACTIVE = "AUTO_CONNECT_ACTIVE";
    private static final String AUTO_CONNECT_VALUE = "AUTO_CONNECT_VALUE";
    private static final String BLOCKED_VALUE = "BLOCKED_VALUE";
    private static final String BLOCKED_ACTIVE = "BLOCKED_ACTIVE";
    private static final String EDIT_ACTIVE = "EDIT_ACTIVE";


    private ActionListener mListener;
    private long mLastClickTime = 0;
    private Context mContext;
    private int backgroundColor;

    public static UserActionDialogFragment newInstance(String pid,
                                                       boolean infoActive,
                                                       boolean detailsActive,
                                                       boolean connectActive,
                                                       boolean autoConnectActive,
                                                       boolean autoConnectValue,
                                                       boolean deleteActive,
                                                       boolean blockedActive,
                                                       boolean blockedValue,
                                                       boolean editActive) {

        Bundle bundle = new Bundle();
        bundle.putString(PID, pid);
        bundle.putBoolean(INFO_ACTIVE, infoActive);
        bundle.putBoolean(DETAILS_ACTIVE, detailsActive);
        bundle.putBoolean(DELETE_ACTIVE, deleteActive);
        bundle.putBoolean(CONNECT_ACTIVE, connectActive);
        bundle.putBoolean(AUTO_CONNECT_ACTIVE, autoConnectActive);
        bundle.putBoolean(AUTO_CONNECT_VALUE, autoConnectValue);
        bundle.putBoolean(BLOCKED_ACTIVE, blockedActive);
        bundle.putBoolean(BLOCKED_VALUE, blockedValue);
        bundle.putBoolean(EDIT_ACTIVE, editActive);


        UserActionDialogFragment fragment = new UserActionDialogFragment();
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
            mListener = (UserActionDialogFragment.ActionListener) getActivity();
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
        String pid = args.getString(PID);
        checkNotNull(pid);
        boolean infoActive = args.getBoolean(INFO_ACTIVE);
        boolean deleteActive = args.getBoolean(DELETE_ACTIVE);
        boolean blockedValue = args.getBoolean(BLOCKED_VALUE);
        boolean blockedActive = args.getBoolean(BLOCKED_ACTIVE);
        boolean connectActive = args.getBoolean(CONNECT_ACTIVE);
        boolean editActive = args.getBoolean(EDIT_ACTIVE);
        boolean detailsActive = args.getBoolean(DETAILS_ACTIVE);
        boolean autoConnectValue = args.getBoolean(AUTO_CONNECT_VALUE);
        boolean autoConnectActive = args.getBoolean(AUTO_CONNECT_ACTIVE);


        @SuppressLint("InflateParams")
        View view = inflater.inflate(R.layout.action_user_view, null);

        view.setBackgroundColor(backgroundColor);


        final CheckBox menu_auto_connect = view.findViewById(R.id.menu_auto_connect);

        if (!autoConnectActive) {
            menu_auto_connect.setVisibility(View.GONE);
        } else {
            menu_auto_connect.setChecked(autoConnectValue);

            menu_auto_connect.setOnClickListener((v) -> {

                try {
                    if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                        return;
                    }
                    mLastClickTime = SystemClock.elapsedRealtime();

                    mListener.clickUserAutoConnect(pid, menu_auto_connect.isChecked());
                } finally {
                    dismiss();
                }

            });
        }

        TextView menu_connect = view.findViewById(R.id.menu_connect);
        if (!connectActive) {
            menu_connect.setVisibility(View.GONE);
        } else {
            menu_connect.setOnClickListener((v) -> {

                try {
                    if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                        return;
                    }
                    mLastClickTime = SystemClock.elapsedRealtime();
                    mListener.clickUserConnect(pid);
                } finally {
                    dismiss();
                }

            });
        }


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
                    mListener.clickUserInfo(pid);
                } finally {
                    dismiss();
                }
            });
        }

        TextView menu_details = view.findViewById(R.id.menu_details);
        if (!detailsActive) {
            menu_details.setVisibility(View.GONE);
        } else {
            menu_details.setOnClickListener((v) -> {
                try {
                    if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                        return;
                    }
                    mLastClickTime = SystemClock.elapsedRealtime();
                    mListener.clickUserDetails(pid);
                } finally {
                    dismiss();
                }
            });
        }
        TextView menu_edit = view.findViewById(R.id.menu_edit);
        if (!editActive) {
            menu_edit.setVisibility(View.GONE);
        } else {
            menu_edit.setOnClickListener((v) -> {

                try {
                    if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                        return;
                    }
                    mLastClickTime = SystemClock.elapsedRealtime();
                    mListener.clickUserEdit(pid);
                } finally {
                    dismiss();
                }
            });
        }

        final CheckBox menu_block = view.findViewById(R.id.menu_block);
        if (!blockedActive) {
            menu_block.setVisibility(View.GONE);
        } else {
            menu_block.setChecked(blockedValue);

            menu_block.setOnClickListener((v) -> {

                try {
                    if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                        return;
                    }
                    mLastClickTime = SystemClock.elapsedRealtime();


                    mListener.clickUserBlock(pid, menu_block.isChecked());
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
                    mListener.clickUserDelete(pid);
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

        void clickUserBlock(@NonNull String pid, boolean value);

        void clickUserInfo(@NonNull String pid);

        void clickUserDelete(@NonNull String pid);

        void clickUserConnect(@NonNull String pid);

        void clickUserEdit(@NonNull String pid);

        void clickUserDetails(@NonNull String pid);

        void clickUserAutoConnect(@NonNull String pid, boolean value);

    }
}
