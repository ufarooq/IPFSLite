package threads.server.fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.selection.Selection;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.selection.StorageStrategy;
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
import threads.server.services.Service;
import threads.server.utils.UserItemDetailsLookup;
import threads.server.utils.UsersItemKeyProvider;
import threads.server.utils.UsersViewAdapter;
import threads.server.work.AutoConnectWorker;

import static androidx.core.util.Preconditions.checkNotNull;

public class PeersFragment extends Fragment implements
        SwipeRefreshLayout.OnRefreshListener, UsersViewAdapter.UsersViewAdapterListener {

    private static final String TAG = PeersFragment.class.getSimpleName();
    private static final int CLICK_OFFSET = 500;
    @NonNull
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private long mLastClickTime = 0;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private UsersViewAdapter mUsersViewAdapter;
    private Context mContext;
    private SelectionTracker<String> mSelectionTracker;
    private ActionMode mActionMode;
    private PeersFragment.ActionListener mListener;


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
            mListener = (PeersFragment.ActionListener) getActivity();
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        mSelectionTracker.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
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

        mSelectionTracker = new SelectionTracker.Builder<>(
                "user-selection",//unique id
                mRecyclerView,
                new UsersItemKeyProvider(mUsersViewAdapter),
                new UserItemDetailsLookup(mRecyclerView),
                StorageStrategy.createStringStorage())
                .build();


        mSelectionTracker.addObserver(new SelectionTracker.SelectionObserver<String>() {
            @Override
            public void onSelectionChanged() {
                if (!mSelectionTracker.hasSelection()) {
                    if (mActionMode != null) {
                        mActionMode.finish();
                    }
                } else {
                    if (mActionMode == null) {
                        mActionMode = ((AppCompatActivity)
                                getActivity()).startSupportActionMode(
                                createActionModeCallback());
                    }
                }
                if (mActionMode != null) {
                    mActionMode.setTitle("" + mSelectionTracker.getSelection().size());
                }
                super.onSelectionChanged();
            }

            @Override
            public void onSelectionRestored() {
                if (!mSelectionTracker.hasSelection()) {
                    if (mActionMode != null) {
                        mActionMode.finish();
                    }
                } else {
                    if (mActionMode == null) {
                        mActionMode = ((AppCompatActivity)
                                getActivity()).startSupportActionMode(
                                createActionModeCallback());
                    }
                }
                if (mActionMode != null) {
                    mActionMode.setTitle("" + mSelectionTracker.getSelection().size());
                }
                super.onSelectionRestored();
            }
        });

        mUsersViewAdapter.setSelectionTracker(mSelectionTracker);


        if (savedInstanceState != null) {
            mSelectionTracker.onRestoreInstanceState(savedInstanceState);
        }


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

    private ActionMode.Callback createActionModeCallback() {
        return new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                mode.getMenuInflater().inflate(R.menu.menu_users_action_mode, menu);

                mListener.showBottomNavigation(false);
                mListener.setPagingEnabled(false);
                mListener.showMainFab(false);
                mHandler.post(() -> mUsersViewAdapter.notifyDataSetChanged());

                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {

                return true;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_mode_mark_all: {

                        if (SystemClock.elapsedRealtime() - mLastClickTime < CLICK_OFFSET) {
                            break;
                        }
                        mLastClickTime = SystemClock.elapsedRealtime();

                        mUsersViewAdapter.selectAllUsers();

                        return true;
                    }
                    case R.id.action_mode_delete: {

                        if (SystemClock.elapsedRealtime() - mLastClickTime < CLICK_OFFSET) {
                            break;
                        }
                        mLastClickTime = SystemClock.elapsedRealtime();

                        try {
                            Selection<String> entries = mSelectionTracker.getSelection();

                            for (String pid : entries) {
                                Service.getInstance(mContext).deleteUser(mContext, pid);
                            }

                            mSelectionTracker.clearSelection();

                        } catch (Throwable e) {
                            Log.e(TAG, "" + e.getLocalizedMessage(), e);
                        }

                        return true;
                    }

                }
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {

                mSelectionTracker.clearSelection();

                mListener.showBottomNavigation(true);
                mListener.setPagingEnabled(true);
                mListener.showMainFab(true);

                if (mActionMode != null) {
                    mActionMode = null;
                }
                mHandler.post(() -> mUsersViewAdapter.notifyDataSetChanged());

            }
        };

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


    public interface ActionListener {

        void showBottomNavigation(boolean visible);

        void showMainFab(boolean visible);

        void setPagingEnabled(boolean enabled);
    }
}
