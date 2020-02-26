package threads.server.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuPopupHelper;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.selection.Selection;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.selection.StorageStrategy;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.work.WorkManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import threads.ipfs.IPFS;
import threads.ipfs.Multihash;
import threads.ipfs.PID;
import threads.server.R;
import threads.server.core.events.EVENTS;
import threads.server.core.peers.PEERS;
import threads.server.core.peers.User;
import threads.server.model.UsersViewModel;
import threads.server.services.LiteService;
import threads.server.utils.Network;
import threads.server.utils.UserItemDetailsLookup;
import threads.server.utils.UsersItemKeyProvider;
import threads.server.utils.UsersViewAdapter;
import threads.server.work.ConnectUserWorker;
import threads.server.work.ConnectionWorker;

public class PeersFragment extends Fragment implements
        SwipeRefreshLayout.OnRefreshListener, UsersViewAdapter.UsersViewAdapterListener {

    private static final String TAG = PeersFragment.class.getSimpleName();
    private static final int CLICK_OFFSET = 500;
    private static final int REQUEST_CAMERA_CAPTURE = 1;
    @NonNull
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean run = new AtomicBoolean(false);
    private long mLastClickTime = 0;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private UsersViewAdapter mUsersViewAdapter;
    private Context mContext;
    private SelectionTracker<String> mSelectionTracker;
    private ActionMode mActionMode;
    private FragmentActivity mActivity;
    private PeersFragment.ActionListener mListener;
    private boolean isTablet;
    private boolean hasCamera;
    private FloatingActionButton mMainFab;

    private static void checkUsers(@NonNull Context context) {

        try {

            PEERS peers = PEERS.getInstance(context);

            IPFS ipfs = IPFS.getInstance(context);

            List<PID> users = peers.getUsersPIDs();

            for (PID user : users) {
                if (!peers.isUserBlocked(user) && !peers.getUserDialing(user)) {

                    try {
                        boolean value = ipfs.isConnected(user);

                        boolean preValue = peers.isUserConnected(user);

                        if (preValue != value) {
                            if (value) {
                                peers.setUserConnected(user);
                            } else {
                                peers.setUserDisconnected(user);
                            }
                        }

                    } catch (Throwable e) {
                        Log.e(TAG, "" + e.getLocalizedMessage(), e);
                    }
                }
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mContext = null;
        mListener = null;
        mActivity = null;
        run.set(false);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
        mActivity = getActivity();
        mListener = (PeersFragment.ActionListener) mActivity;
        isTablet = getResources().getBoolean(R.bool.isTablet);
        PackageManager pm = mContext.getPackageManager();
        hasCamera = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
        run.set(true);
        peersOnlineStatus();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        mSelectionTracker.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);
        menuInflater.inflate(R.menu.menu_peers_fragment, menu);

        MenuItem actionScanPid = menu.findItem(R.id.action_scan_pid);
        actionScanPid.setVisible(isTablet && hasCamera);

        MenuItem actionEditPid = menu.findItem(R.id.action_edit_pid);
        actionEditPid.setVisible(true);


    }

    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (item.getItemId() == R.id.action_scan_pid) {

            if (SystemClock.elapsedRealtime() - mLastClickTime < CLICK_OFFSET) {
                return true;
            }

            mLastClickTime = SystemClock.elapsedRealtime();

            clickScanPeer();
            return true;

        } else if (item.getItemId() == R.id.action_select_all) {

            if (SystemClock.elapsedRealtime() - mLastClickTime < CLICK_OFFSET) {
                return true;
            }

            mLastClickTime = SystemClock.elapsedRealtime();

            mUsersViewAdapter.selectAllUsers();

            return true;

        } else if (item.getItemId() == R.id.action_edit_pid) {

            if (SystemClock.elapsedRealtime() - mLastClickTime < CLICK_OFFSET) {
                return true;
            }

            mLastClickTime = SystemClock.elapsedRealtime();

            clickEditPeer();
            return true;

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.peers_view, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        mMainFab = view.findViewById(R.id.fab_main);
        if (isTablet) {
            mMainFab.setVisibility(View.GONE);
        }

        mMainFab.setOnClickListener((v) -> {

            if (SystemClock.elapsedRealtime() - mLastClickTime < 500) {
                return;
            }
            mLastClickTime = SystemClock.elapsedRealtime();

            clickScanPeer();

        });

        RecyclerView mRecyclerView = view.findViewById(R.id.recycler_users);

        mSwipeRefreshLayout = view.findViewById(R.id.swipe_container);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorAccent,
                android.R.color.holo_green_dark,
                android.R.color.holo_orange_dark,
                android.R.color.holo_blue_dark);


        LinearLayoutManager linearLayout = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(linearLayout);
        mUsersViewAdapter = new UsersViewAdapter(this);
        mRecyclerView.setAdapter(mUsersViewAdapter);

        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                boolean hasSelection = mSelectionTracker.hasSelection();
                if (dy > 0 && !hasSelection) {
                    showMainFab(false);
                } else if (dy < 0 && !hasSelection) {
                    showMainFab(true);
                }

            }
        });

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
                        mActionMode = ((AppCompatActivity) mActivity).startSupportActionMode(
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
                        mActionMode = ((AppCompatActivity) mActivity).startSupportActionMode(
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


        UsersViewModel messagesViewModel = new ViewModelProvider(this).get(UsersViewModel.class);
        messagesViewModel.getUsers().observe(getViewLifecycleOwner(), (peers) -> {

            try {
                if (peers != null) {
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

    }

    private void showMainFab(boolean visible) {
        if (!isTablet) {
            if (visible) {
                mMainFab.show();
            } else {
                mMainFab.hide();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        try {

            IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
            if (result != null) {
                if (result.getContents() != null) {
                    String multihash = result.getContents();
                    clickConnectPeer(multihash);
                } else {
                    EVENTS.getInstance(mContext).postError(getString(R.string.scan_failed));
                }
            }

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult
            (int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_CAPTURE) {
            for (int i = 0, len = permissions.length; i < len; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    EVENTS.getInstance(mContext).postPermission(
                            getString(R.string.permission_camera_denied));
                }
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    clickConnectPeerWork();
                }
            }
        }

    }

    private ActionMode.Callback createActionModeCallback() {
        return new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                mode.getMenuInflater().inflate(R.menu.menu_users_action_mode, menu);

                mListener.showBottomNavigation(false);
                mListener.setPagingEnabled(false);
                showMainFab(false);
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
                    case R.id.action_mode_connect: {
                        if (SystemClock.elapsedRealtime() - mLastClickTime < CLICK_OFFSET) {
                            break;
                        }
                        mLastClickTime = SystemClock.elapsedRealtime();

                        if (!Network.isConnected(mContext)) {
                            EVENTS.getInstance(mContext).postWarning(getString(R.string.offline_mode));
                        }

                        try {
                            Selection<String> entries = mSelectionTracker.getSelection();
                            for (String pid : entries) {
                                connectUser(pid);
                            }
                            mSelectionTracker.clearSelection();

                        } catch (Throwable e) {
                            Log.e(TAG, "" + e.getLocalizedMessage(), e);
                        }
                    }
                    case R.id.action_mode_delete: {

                        if (SystemClock.elapsedRealtime() - mLastClickTime < CLICK_OFFSET) {
                            break;
                        }
                        mLastClickTime = SystemClock.elapsedRealtime();

                        try {
                            Selection<String> entries = mSelectionTracker.getSelection();

                            for (String pid : entries) {
                                deleteUser(pid);
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
                showMainFab(true);

                if (mActionMode != null) {
                    mActionMode = null;
                }
                mHandler.post(() -> mUsersViewAdapter.notifyDataSetChanged());

            }
        };

    }

    @SuppressLint("RestrictedApi")
    @Override
    public void invokeGeneralAction(@NonNull User user, @NonNull View view) {

        try {
            boolean senderBlocked = user.isBlocked();
            boolean isConnected = user.isConnected();
            PopupMenu menu = new PopupMenu(mContext, view);
            menu.inflate(R.menu.popup_peers_menu);
            menu.getMenu().findItem(R.id.popup_block).setVisible(!senderBlocked);
            menu.getMenu().findItem(R.id.popup_unblock).setVisible(senderBlocked);
            menu.getMenu().findItem(R.id.popup_connect).setVisible(!isConnected);
            menu.setOnMenuItemClickListener((item) -> {


                if (SystemClock.elapsedRealtime() - mLastClickTime < CLICK_OFFSET) {
                    return true;
                }

                mLastClickTime = SystemClock.elapsedRealtime();


                if (item.getItemId() == R.id.popup_connect) {
                    if (!Network.isConnected(mContext)) {
                        EVENTS.getInstance(mContext).postWarning(getString(R.string.offline_mode));
                    }
                    connectUser(user.getPid());
                    return true;
                } else if (item.getItemId() == R.id.popup_delete) {
                    clickUserDelete(user.getPid());
                    return true;
                } else if (item.getItemId() == R.id.popup_info) {
                    clickUserInfo(user);
                    return true;
                } else if (item.getItemId() == R.id.popup_edit) {
                    clickUserEdit(user.getPid());
                    return true;
                } else if (item.getItemId() == R.id.popup_block) {
                    clickUserBlock(user.getPid(), true);
                    return true;
                } else if (item.getItemId() == R.id.popup_unblock) {
                    clickUserBlock(user.getPid(), false);
                    return true;
                }
                return false;

            });

            MenuPopupHelper menuHelper = new MenuPopupHelper(
                    mContext, (MenuBuilder) menu.getMenu(), view);
            menuHelper.setForceShowIcon(true);
            menuHelper.show();


        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    @Override
    public void invokeAbortDialing(@NonNull User user) {

        if (SystemClock.elapsedRealtime() - mLastClickTime < 500) {
            return;
        }
        mLastClickTime = SystemClock.elapsedRealtime();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() ->
                PEERS.getInstance(mContext).setUserDialing(user.getPid(), false)
        );
        WorkManager.getInstance(mContext).cancelUniqueWork(
                ConnectUserWorker.getUniqueId(user.getPid()));

    }

    @Override
    public void onRefresh() {
        mSwipeRefreshLayout.setRefreshing(true);


        try {
            ConnectionWorker.connect(mContext, true);
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                PEERS peers = PEERS.getInstance(mContext);
                List<User> users = peers.getUsers();
                for (User user : users) {
                    if (!user.isBlocked()) {
                        peers.setUserDialing(user.getPid(), Network.isConnected(mContext));
                    }
                }

            });


        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        } finally {
            mSwipeRefreshLayout.setRefreshing(false);
        }

    }

    private void clickUserDelete(@NonNull String pid) {
        deleteUser(pid);
    }

    private void clickUserBlock(@NonNull String pid, boolean value) {

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                PEERS peers = PEERS.getInstance(mContext);
                if (!value) {
                    peers.unblockUser(PID.create(pid));
                } else {
                    peers.blockUser(PID.create(pid));
                }

            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }
        });

    }

    private void clickUserInfo(@NonNull User user) {

        try {
            InfoDialogFragment.newInstance(user.getPid(),
                    getString(R.string.peer_id),
                    getString(R.string.peer_access, user.getAlias()))
                    .show(getChildFragmentManager(), InfoDialogFragment.TAG);

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    private void connectUser(@NonNull String pid) {

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {

            if (PEERS.getInstance(mContext).isUserBlocked(pid)) {
                EVENTS.getInstance(mContext).warning(mContext.getString(R.string.peer_is_blocked));

            } else {
                PEERS.getInstance(mContext).setUserDialing(pid, Network.isConnected(mContext));
                ConnectUserWorker.connect(mContext, pid);
            }
        });

    }

    private void clickUserEdit(@NonNull String pid) {

        try {
            NameDialogFragment.newInstance(pid, getString(R.string.peer_name))
                    .show(getChildFragmentManager(), NameDialogFragment.TAG);
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    // todo remove and change to event mechanism
    private void peersOnlineStatus() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                while (run.get()) {
                    checkUsers(mContext);
                    java.lang.Thread.sleep(1000);
                }
            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }
        });
    }

    private void clickScanPeer() {

        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_CAPTURE);
            return;
        }

        clickConnectPeerWork();
    }


    private void clickConnectPeerWork() {
        try {
            PackageManager pm = mActivity.getPackageManager();

            if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
                IntentIntegrator integrator = IntentIntegrator.forSupportFragment(this);
                integrator.setOrientationLocked(false);
                integrator.initiateScan();
            } else {
                EVENTS.getInstance(mContext).postError(getString(R.string.feature_camera_required));
            }

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }


    private void clickConnectPeer(@NonNull String pid) {

        // CHECKED if pid is valid
        try {
            Multihash.fromBase58(pid);
        } catch (Throwable e) {
            EVENTS.getInstance(mContext).postError(getString(R.string.multihash_not_valid));
            return;
        }

        // CHECKED
        PID host = IPFS.getPID(mContext);
        PID user = PID.create(pid);

        if (user.equals(host)) {
            EVENTS.getInstance(mContext).postError(getString(R.string.same_pid_like_host));
            return;
        }

        // CHECKED
        if (!Network.isConnected(mContext)) {
            EVENTS.getInstance(mContext).postWarning(getString(R.string.offline_mode));
        }

        LiteService.connectPeer(mContext, user, false);
    }

    private void clickEditPeer() {
        try {
            EditPeerDialogFragment editPeerDialogFragment = new EditPeerDialogFragment();
            editPeerDialogFragment.show(getChildFragmentManager(), EditPeerDialogFragment.TAG);
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    private void deleteUser(@NonNull String pid) {

        try {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> PEERS.getInstance(mContext).removeUser(pid));
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    public interface ActionListener {

        void showBottomNavigation(boolean visible);

        void setPagingEnabled(boolean enabled);

    }
}
