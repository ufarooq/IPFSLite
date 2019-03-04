package threads.server;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import threads.core.Preferences;
import threads.core.api.User;
import threads.core.mdl.UsersViewModel;
import threads.ipfs.api.PID;
import threads.share.UserActionDialogFragment;
import threads.share.UsersViewAdapter;

import static com.google.common.base.Preconditions.checkNotNull;

public class PeersFragment extends Fragment implements UsersViewAdapter.UsersViewAdapterListener {

    private long mLastClickTime = 0;

    private RecyclerView mRecyclerView;
    private UsersViewAdapter usersViewAdapter;
    private ActionListener actionListener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        try {
            actionListener = (PeersFragment.ActionListener) getActivity();
        } catch (Throwable e) {
            Preferences.evaluateException(Preferences.EXCEPTION, e);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_peers, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_info: {

                // mis-clicking prevention, using threshold of 1000 ms
                if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                    break;
                }
                mLastClickTime = SystemClock.elapsedRealtime();

                actionListener.clickUserInfo();


                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.peers_view, container, false);

        Activity activity = getActivity();
        checkNotNull(activity);
        FloatingActionButton fab_action = view.findViewById(R.id.fab_action);
        fab_action.setOnClickListener((v) -> {

            if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                return;
            }
            mLastClickTime = SystemClock.elapsedRealtime();

            if (getActivity() != null) {
                ActionDialogFragment.newInstance(true, true)
                        .show(getActivity().getSupportFragmentManager(), ActionDialogFragment.TAG);
            }

        });


        mRecyclerView = view.findViewById(R.id.recycler_users);
        mRecyclerView.setItemAnimator(null); // no animation of the item when something changed

        LinearLayoutManager linearLayout = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(linearLayout);
        usersViewAdapter = new UsersViewAdapter(activity, this);
        mRecyclerView.setAdapter(usersViewAdapter);

        PID host = null;
        if (getActivity() != null) {
            host = Preferences.getPID(getActivity());
        }
        PID hostPid = host;
        UsersViewModel messagesViewModel = ViewModelProviders.of(this).get(UsersViewModel.class);
        messagesViewModel.getUsers().observe(this, (users) -> {

            try {
                if (users != null) {
                    List<User> peers = new ArrayList<>();
                    for (User user : users) {
                        if (!user.getPID().equals(hostPid)) {
                            peers.add(user);
                        }

                    }
                    try {
                        usersViewAdapter.updateData(peers);
                    } catch (Throwable e) {
                        Preferences.evaluateException(Preferences.EXCEPTION, e);
                    }
                }
            } catch (Throwable e) {
                Preferences.evaluateException(Preferences.EXCEPTION, e);
            }

        });

        return view;
    }


    @Override
    public void invokeGeneralAction(@NonNull User user) {
        checkNotNull(user);

        if (getActivity() != null) {
            FragmentManager fm = getActivity().getSupportFragmentManager();

            UserActionDialogFragment.newInstance(
                    user.getPID().getPid(), true, true,
                    true, true, true)
                    .show(fm, UserActionDialogFragment.TAG);
        }

    }

    public interface ActionListener {

        void clickUserInfo();
    }
}
