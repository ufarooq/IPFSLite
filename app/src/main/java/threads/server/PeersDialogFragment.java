package threads.server;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
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

public class PeersDialogFragment extends DialogFragment {
    static final String TAG = PeersDialogFragment.class.getSimpleName();
    private static final String EDIT_PEER_ACTIVE = "EDIT_PEER_ACTIVE";
    private static final String SCAN_PEER_ACTIVE = "SCAN_PEER_ACTIVE";
    private static final String YOUR_PEER_ACTIVE = "YOUR_PEER_ACTIVE";

    private ActionListener actionListener;
    private long mLastClickTime = 0;

    static PeersDialogFragment newInstance(boolean editPeerActive,
                                           boolean scanPeerActive,
                                           boolean yourPeerActive) {

        Bundle bundle = new Bundle();

        bundle.putBoolean(EDIT_PEER_ACTIVE, editPeerActive);
        bundle.putBoolean(SCAN_PEER_ACTIVE, scanPeerActive);
        bundle.putBoolean(YOUR_PEER_ACTIVE, yourPeerActive);
        PeersDialogFragment fragment = new PeersDialogFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        try {
            actionListener = (ActionListener) getActivity();
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
        boolean editPeerActive = args.getBoolean(EDIT_PEER_ACTIVE);
        boolean scanPeerActive = args.getBoolean(SCAN_PEER_ACTIVE);
        boolean yourPeerActive = args.getBoolean(YOUR_PEER_ACTIVE);


        @SuppressWarnings("all")
        View view = inflater.inflate(R.layout.peers_action_view, null);

        TextView menu_scan_peer = view.findViewById(R.id.menu_scan_pid);
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


        TextView menu_edit_peer = view.findViewById(R.id.menu_edit_pid);
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

        TextView menu_your_peer = view.findViewById(R.id.menu_your_pid);
        if (!yourPeerActive) {
            menu_your_peer.setVisibility(View.GONE);
        } else {
            menu_your_peer.setOnClickListener((v) -> {

                try {
                    if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                        return;
                    }

                    mLastClickTime = SystemClock.elapsedRealtime();

                    actionListener.clickInfoPeer();

                } finally {
                    dismiss();
                }

            });
        }
        if (!editPeerActive && !scanPeerActive && !yourPeerActive) {
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

        void clickConnectPeer();

        void clickEditPeer();

        void clickInfoPeer();
    }
}
