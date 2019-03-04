package threads.server;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import threads.core.Preferences;

import static com.google.common.base.Preconditions.checkNotNull;

public class ActionDialogFragment extends DialogFragment {
    static final String TAG = ActionDialogFragment.class.getSimpleName();
    private static final String EDIT_PEER_ACTIVE = "EDIT_PEER_ACTIVE";
    private static final String SCAN_PEER_ACTIVE = "SCAN_PEER_ACTIVE";

    private ActionListener actionListener;
    private long mLastClickTime = 0;

    static ActionDialogFragment newInstance(boolean editPeerActive,
                                            boolean scanPeerActive) {

        Bundle bundle = new Bundle();

        bundle.putBoolean(EDIT_PEER_ACTIVE, editPeerActive);
        bundle.putBoolean(SCAN_PEER_ACTIVE, scanPeerActive);
        ActionDialogFragment fragment = new ActionDialogFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        try {
            actionListener = (ActionListener) getActivity();
        } catch (Throwable e) {
            Preferences.evaluateException(Preferences.EXCEPTION, e);
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
        boolean editPeerActive = args.getBoolean(EDIT_PEER_ACTIVE);
        boolean scanPeerActive = args.getBoolean(SCAN_PEER_ACTIVE);


        @SuppressWarnings("all")
        View view = inflater.inflate(R.layout.action_view, null);

        TextView menu_scan_peer = view.findViewById(R.id.menu_scan_peer);
        if (!scanPeerActive) {
            menu_scan_peer.setVisibility(View.GONE);
        } else {
            menu_scan_peer.setOnClickListener((v) -> {

                try {
                    if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                        return;
                    }

                    mLastClickTime = SystemClock.elapsedRealtime();
                    actionListener.clickConnectPeer();
                } finally {
                    dismiss();
                }


            });
        }


        TextView menu_edit_peer = view.findViewById(R.id.menu_edit_peer);
        if (!editPeerActive) {
            menu_edit_peer.setVisibility(View.GONE);
        } else {
            menu_edit_peer.setOnClickListener((v) -> {

                try {
                    if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                        return;
                    }

                    mLastClickTime = SystemClock.elapsedRealtime();

                    actionListener.clickEditPeer();

                } finally {
                    dismiss();
                }

            });
        }

        if (!editPeerActive && !scanPeerActive) {
            view.findViewById(R.id.row_first).setVisibility(View.GONE);
        }


        builder.setView(view);
        AlertDialog dialog = builder.create();

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        Window window = dialog.getWindow();
        if (window != null) {
            window.getAttributes().windowAnimations = R.style.ActionDialogAnimation;
            window.getAttributes().gravity = Gravity.BOTTOM | Gravity.CENTER;
        }
        return dialog;
    }

    public interface ActionListener {

        void clickConnectPeer();

        void clickEditPeer();
    }
}
