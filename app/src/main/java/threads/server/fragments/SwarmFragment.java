package threads.server.fragments;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuPopupHelper;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import threads.ipfs.IPFS;
import threads.ipfs.Multihash;
import threads.ipfs.PID;
import threads.server.R;
import threads.server.core.events.EVENTS;
import threads.server.core.peers.IPeer;
import threads.server.core.peers.Peer;
import threads.server.model.PeersViewModel;
import threads.server.services.Service;
import threads.server.utils.PeersViewAdapter;
import threads.server.work.BootstrapWorker;
import threads.server.work.LoadPeersWorker;

import static androidx.core.util.Preconditions.checkNotNull;

public class SwarmFragment extends Fragment implements
        SwipeRefreshLayout.OnRefreshListener, PeersViewAdapter.PeersViewAdapterListener {

    public static final String TAG = SwarmFragment.class.getSimpleName();
    public static final String LOW = "LOW";
    public static final String HIGH = "HIGH";
    public static final String MEDIUM = "MEDIUM";
    public static final String NONE = "NONE";
    private PeersViewAdapter peersViewAdapter;
    private Context mContext;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private SwarmFragment.ActionListener mListener;

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
        mListener = (SwarmFragment.ActionListener) getActivity();
    }

    @Override
    public void onRefresh() {
        loadPeers(true);
    }

    private void loadPeers(boolean animation) {

        if (animation) {
            mSwipeRefreshLayout.setRefreshing(true);
        }


        try {
            BootstrapWorker.bootstrap(mContext);
            LoadPeersWorker.loadPeers(mContext);
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        } finally {
            if (animation) {
                mSwipeRefreshLayout.setRefreshing(false);
            }
        }

    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.swarm_view, container, false);


        RecyclerView mRecyclerView = view.findViewById(R.id.recycler_peers);
        mRecyclerView.setItemAnimator(null); // no animation of the item when something changed

        mSwipeRefreshLayout = view.findViewById(R.id.swipe_container);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorAccent,
                android.R.color.holo_green_dark,
                android.R.color.holo_orange_dark,
                android.R.color.holo_blue_dark);


        LinearLayoutManager linearLayout = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(linearLayout);
        peersViewAdapter = new PeersViewAdapter(mContext, this);
        mRecyclerView.setAdapter(peersViewAdapter);

        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0) {
                    mListener.showMainFab(false);
                } else if (dy < 0) {
                    mListener.showMainFab(true);
                }

            }
        });

        PeersViewModel messagesViewModel = new ViewModelProvider(this).get(PeersViewModel.class);
        messagesViewModel.getPeers().observe(getViewLifecycleOwner(), (peers) -> {

            try {
                if (peers != null) {
                    try {
                        List<Peer> connected = new ArrayList<>();
                        for (Peer peer : peers) {
                            if (peer.isConnected()) {
                                connected.add(peer);
                            }
                        }
                        connected.sort(Comparator.comparing(IPeer::getAlias));
                        peersViewAdapter.updateData(connected);
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
    public void onResume() {
        super.onResume();
        if (isVisible()) {
            loadPeers(false);
        }
    }

    @Override
    public void invokeGeneralAction(@NonNull IPeer peer, @NonNull View view) {
        checkNotNull(peer);
        checkNotNull(view);

        try {

            PopupMenu menu = new PopupMenu(mContext, view);
            menu.inflate(R.menu.popup_swarm_menu);

            menu.setOnMenuItemClickListener((item) -> {

                if (item.getItemId() == R.id.popup_info) {
                    clickPeerInfo(peer.getPID().getPid());
                    return true;
                } else if (item.getItemId() == R.id.popup_add) {
                    clickPeerAdd(peer.getPID().getPid());
                    return true;
                } else if (item.getItemId() == R.id.popup_details) {
                    clickPeerDetails(peer.getPID().getPid());
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
    public boolean generalActionSupport(@NonNull IPeer peer) {
        return !peer.isDialing();
    }


    private void clickPeerInfo(@NonNull String pid) {
        checkNotNull(pid);
        try {
            InfoDialogFragment.newInstance(pid,
                    getString(R.string.peer_id),
                    getString(R.string.peer_access, pid))
                    .show(getChildFragmentManager(), InfoDialogFragment.TAG);

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }


    private void clickPeerDetails(@NonNull String pid) {
        mListener.clickUserDetails(pid);
    }


    private void clickPeerAdd(@NonNull String pid) {
        checkNotNull(pid);

        // CHECKED if pid is valid
        try {
            Multihash.fromBase58(pid);
        } catch (Throwable e) {
            java.lang.Thread threadError = new java.lang.Thread(()
                    -> EVENTS.getInstance(mContext).error(getString(R.string.multihash_not_valid)));
            threadError.start();
            return;
        }

        // CHECKED
        PID host = IPFS.getPID(mContext);
        PID user = PID.create(pid);

        if (user.equals(host)) {

            java.lang.Thread threadError = new java.lang.Thread(()
                    -> EVENTS.getInstance(mContext).error(
                    getString(R.string.same_pid_like_host)));
            threadError.start();

            return;
        }


        try {

            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    Service.connectPeer(mContext, user, true);
                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
                }
            });

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    public interface ActionListener {

        void showMainFab(boolean visible);

        void clickUserDetails(String pid);

    }
}
