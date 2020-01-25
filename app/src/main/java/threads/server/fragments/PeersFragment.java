package threads.server.fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import threads.ipfs.IPFS;
import threads.ipfs.PID;
import threads.server.R;
import threads.server.core.peers.User;
import threads.server.mdl.UsersViewModel;
import threads.server.utils.UsersViewAdapter;
import threads.server.work.AutoConnectWorker;

import static androidx.core.util.Preconditions.checkNotNull;

public class PeersFragment extends Fragment implements
        SwipeRefreshLayout.OnRefreshListener, UsersViewAdapter.UsersViewAdapterListener {

    private static final String TAG = PeersFragment.class.getSimpleName();
    private long mLastClickTime = 0;

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private UsersViewAdapter mUsersViewAdapter;
    private Context mContext;


    @Override
    public void onDetach() {
        super.onDetach();
        mContext = null;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.peers_view, container, false);


        RecyclerView mRecyclerView = view.findViewById(R.id.recycler_users);
        mRecyclerView.setItemAnimator(null); // no animation of the item when something changed


        mSwipeRefreshLayout = view.findViewById(R.id.swipe_container);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorAccent,
                android.R.color.holo_green_dark,
                android.R.color.holo_orange_dark,
                android.R.color.holo_blue_dark);


        LinearLayoutManager linearLayout = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(linearLayout);
        mUsersViewAdapter = new UsersViewAdapter(mContext, this);
        mRecyclerView.setAdapter(mUsersViewAdapter);

        final PID host = IPFS.getPID(mContext);
        UsersViewModel messagesViewModel = new ViewModelProvider(this).get(UsersViewModel.class);
        messagesViewModel.getUsers().observe(getViewLifecycleOwner(), (users) -> {

            try {
                if (users != null) {
                    List<User> peers = new ArrayList<>();
                    for (User user : users) {
                        if (!user.getPID().equals(host)) {
                            peers.add(user);
                        }

                    }
                    try {

                        peers.sort(Comparator.comparing(User::getAlias));

                        mUsersViewAdapter.updateData(peers);
                    } catch (Throwable e) {
                        Log.e(TAG, "" + e.getLocalizedMessage(), e);
                    }
                }
            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }

        });

        return view;
    }


    @Override
    public void invokeGeneralAction(@NonNull User user) {
        checkNotNull(user);
        try {

            if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                return;
            }
            mLastClickTime = SystemClock.elapsedRealtime();

            UserActionDialogFragment.newInstance(
                    user.getPID().getPid(), true, user.isConnected(), true,
                    true, true, user.isBlocked(),
                    true)
                    .show(getChildFragmentManager(), UserActionDialogFragment.TAG);

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }

    }

    @Override
    public boolean generalActionSupport(@NonNull User user) {
        return !user.isDialing();
    }


    @Override
    public void onRefresh() {
        mSwipeRefreshLayout.setRefreshing(true);

        try {
            AutoConnectWorker.autoConnect(mContext, true, 0);
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        } finally {
            mSwipeRefreshLayout.setRefreshing(false);
        }

    }
}
