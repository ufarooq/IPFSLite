package threads.server;

import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import threads.core.api.User;
import threads.core.mdl.UsersViewModel;
import threads.share.NoteActionDialogFragment;
import threads.share.UserActionDialogFragment;
import threads.share.UsersViewAdapter;

import static com.google.common.base.Preconditions.checkNotNull;

public class PeersFragment extends Fragment implements UsersViewAdapter.UsersViewAdapterListener {
    private static final String TAG = PeersFragment.class.getSimpleName();
    private long mLastClickTime = 0;

    private RecyclerView mRecyclerView;
    private UsersViewAdapter usersViewAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.peers_view, container, false);


        FloatingActionButton fab_action = view.findViewById(R.id.fab_action);
        fab_action.setOnClickListener((v) -> {

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


        mRecyclerView = view.findViewById(R.id.recycler_users);

        LinearLayoutManager linearLayout = new LinearLayoutManager(getContext());
        mRecyclerView.addOnLayoutChangeListener((View v,
                                                 int left, int top, int right, int bottom,
                                                 int oldLeft, int oldTop,
                                                 int oldRight, int oldBottom) -> {

            if (bottom < oldBottom) {
                mRecyclerView.postDelayed(() -> {

                    try {
                        RecyclerView.Adapter adapter = mRecyclerView.getAdapter();
                        if (adapter != null) {
                            mRecyclerView.smoothScrollToPosition(
                                    adapter.getItemCount());
                        }
                    } catch (Throwable e) {
                        Log.e(TAG, "" + e.getLocalizedMessage(), e);
                    }

                }, 50);
            }

        });


        mRecyclerView.setLayoutManager(linearLayout);
        usersViewAdapter = new UsersViewAdapter(this);
        mRecyclerView.setAdapter(usersViewAdapter);

        UsersViewModel messagesViewModel = ViewModelProviders.of(this).get(UsersViewModel.class);
        messagesViewModel.getUsers().observe(this, (users) -> {

            try {
                if (users != null) {
                    updatePeers(users);
                }
            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage());
            }

        });

        return view;
    }

    private void updatePeers(@NonNull List<User> users) {
        try {
            usersViewAdapter.updateData(users);

            mRecyclerView.scrollToPosition(usersViewAdapter.getItemCount() - 1);
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }

    }

    @Override
    public void invokeGeneralAction(@NonNull User user) {
        checkNotNull(user);

        if (getActivity() != null) {
            FragmentManager fm = getActivity().getSupportFragmentManager();

            UserActionDialogFragment.newInstance(
                    user.getPid(), true, true, true, false)
                    .show(fm, NoteActionDialogFragment.TAG);
        }

    }
}
