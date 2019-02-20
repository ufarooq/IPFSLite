package threads.server;

import android.os.Bundle;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

public class PeersFragment extends Fragment {

    private long mLastClickTime = 0;
    private FloatingActionButton fab_action;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.peers_view, container, false);


        fab_action = view.findViewById(R.id.fab_action);
        fab_action.setOnClickListener((v) -> {


            // mis-clicking prevention, using threshold of 1000 ms
            if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                return;
            }
            mLastClickTime = SystemClock.elapsedRealtime();

            if (getActivity() != null) {
                FragmentManager fm = getActivity().getSupportFragmentManager();

                ActionDialogFragment messageActionDialogFragment = new ActionDialogFragment();
                messageActionDialogFragment.show(fm, ActionDialogFragment.TAG);
            }

        });
        return view;
    }
}
