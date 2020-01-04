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
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import threads.server.R;

import static androidx.core.util.Preconditions.checkNotNull;

public class PeerActionDialogFragment extends DialogFragment {
    public static final String TAG = PeerActionDialogFragment.class.getSimpleName();
    private static final String PID = "PID";
    private static final String INFO_ACTIVE = "INFO_ACTIVE";
    private static final String DETAILS_ACTIVE = "DETAILS_ACTIVE";
    private static final String ADD_ACTIVE = "ADD_ACTIVE";


    private ActionListener mListener;
    private long mLastClickTime = 0;
    private Context mContext;
    private int backgroundColor;

    public static PeerActionDialogFragment newInstance(String pid,
                                                       boolean infoActive,
                                                       boolean detailsActive,
                                                       boolean addActive) {

        Bundle bundle = new Bundle();
        bundle.putString(PID, pid);
        bundle.putBoolean(INFO_ACTIVE, infoActive);
        bundle.putBoolean(DETAILS_ACTIVE, detailsActive);
        bundle.putBoolean(ADD_ACTIVE, addActive);

        PeerActionDialogFragment fragment = new PeerActionDialogFragment();
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
            mListener = (PeerActionDialogFragment.ActionListener) getActivity();
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
        boolean addActive = args.getBoolean(ADD_ACTIVE);
        boolean detailsActive = args.getBoolean(DETAILS_ACTIVE);


        @SuppressWarnings("all")
        View view = inflater.inflate(R.layout.action_peer_view, null);

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
                    mListener.clickPeerInfo(pid);
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
                    mListener.clickPeerDetails(pid);
                } finally {
                    dismiss();
                }
            });
        }
        TextView menu_add = view.findViewById(R.id.menu_add);
        if (!addActive) {
            menu_add.setVisibility(View.GONE);
        } else {
            menu_add.setOnClickListener((v) -> {

                try {
                    if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                        return;
                    }
                    mLastClickTime = SystemClock.elapsedRealtime();
                    mListener.clickPeerAdd(pid);
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

        void clickPeerInfo(@NonNull String pid);

        void clickPeerDetails(@NonNull String pid);

        void clickPeerAdd(@NonNull String pid);
    }
}
