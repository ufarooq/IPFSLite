package threads.server.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
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

import java.util.Comparator;

import threads.ipfs.PID;
import threads.server.R;
import threads.server.core.peers.Peer;
import threads.server.model.PeersViewModel;
import threads.server.services.LiteService;
import threads.server.utils.PeersViewAdapter;
import threads.server.work.ConnectionWorker;
import threads.server.work.LoadPeersWorker;

public class SwarmFragment extends Fragment implements
        SwipeRefreshLayout.OnRefreshListener, PeersViewAdapter.PeersViewAdapterListener {

    private static final String TAG = SwarmFragment.class.getSimpleName();
    private static final int CLICK_OFFSET = 500;
    private PeersViewAdapter peersViewAdapter;
    private Context mContext;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private long mLastClickTime = 0;

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
    public void onRefresh() {
        loadPeers(true);
    }

    private void loadPeers(boolean animation) {

        if (animation) {
            mSwipeRefreshLayout.setRefreshing(true);
        }
        ConnectionWorker.connect(mContext, true);
        LoadPeersWorker.loadPeers(mContext);

        if (animation) {
            mSwipeRefreshLayout.setRefreshing(false);
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
        peersViewAdapter = new PeersViewAdapter(this);
        mRecyclerView.setAdapter(peersViewAdapter);


        PeersViewModel messagesViewModel = new ViewModelProvider(this).get(PeersViewModel.class);
        messagesViewModel.getPeers().observe(getViewLifecycleOwner(), (peers) -> {

            try {
                if (peers != null) {
                    try {
                        peers.sort(Comparator.comparing(Peer::getPid));
                        peersViewAdapter.updateData(peers);
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

    @SuppressLint("RestrictedApi")
    @Override
    public void invokeAction(@NonNull Peer peer, @NonNull View view) {

        try {

            PopupMenu menu = new PopupMenu(mContext, view);
            menu.inflate(R.menu.popup_swarm_menu);

            menu.setOnMenuItemClickListener((item) -> {


                if (SystemClock.elapsedRealtime() - mLastClickTime < CLICK_OFFSET) {
                    return true;
                }

                mLastClickTime = SystemClock.elapsedRealtime();
                if (item.getItemId() == R.id.popup_info) {
                    clickPeerInfo(peer.getPID().getPid());
                    return true;
                } else if (item.getItemId() == R.id.popup_add) {
                    clickPeerAdd(peer.getPID().getPid());
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


    private void clickPeerInfo(@NonNull String pid) {

        try {
            InfoDialogFragment.newInstance(pid,
                    getString(R.string.peer_id),
                    getString(R.string.peer_access, pid))
                    .show(getChildFragmentManager(), InfoDialogFragment.TAG);

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }


    private void clickPeerAdd(@NonNull String pid) {

        LiteService.connectPeer(mContext, PID.create(pid), true);
    }

}
