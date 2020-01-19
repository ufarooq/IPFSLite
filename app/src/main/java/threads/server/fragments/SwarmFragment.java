package threads.server.fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
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
import threads.server.R;
import threads.server.core.events.EVENTS;
import threads.server.core.peers.IPeer;
import threads.server.core.peers.Peer;
import threads.server.core.threads.THREADS;
import threads.server.mdl.PeersViewModel;
import threads.server.services.GatewayService;
import threads.server.utils.PeersViewAdapter;

import static androidx.core.util.Preconditions.checkNotNull;

@SuppressWarnings("ALL")
public class SwarmFragment extends Fragment implements
        SwipeRefreshLayout.OnRefreshListener, PeersViewAdapter.PeersViewAdapterListener {

    public static final String TAG = SwarmFragment.class.getSimpleName();
    public static final String LOW = "LOW";
    public static final String HIGH = "HIGH";
    public static final String MEDIUM = "MEDIUM";
    public static final String NONE = "NONE";
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private long mLastClickTime = 0;
    private PeersViewAdapter peersViewAdapter;
    private Context mContext;
    private SwipeRefreshLayout mSwipeRefreshLayout;


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
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {

            try {
                loadPeers(mContext);
            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            } finally {
                if (animation) {
                    mHandler.post(() -> mSwipeRefreshLayout.setRefreshing(false));
                }
            }
        });
    }

    private void loadPeers(@NonNull Context context) {
        checkNotNull(context);
        THREADS threads = THREADS.getInstance(context);
        IPFS ipfs = IPFS.getInstance(context);
        EVENTS events = EVENTS.getInstance(context);
        try {
            checkNotNull(ipfs, "IPFS not valid");
            GatewayService.PeerSummary info = GatewayService.evaluateAllPeers(context);

            String content = SwarmFragment.NONE;
            if (info.getLatency() < 150) {
                content = SwarmFragment.HIGH;
            } else if (info.getLatency() < 500) {
                content = SwarmFragment.MEDIUM;
            } else if (info.getNumPeers() > 0) {
                content = SwarmFragment.LOW;
            }
            events.invokeEvent(SwarmFragment.TAG, content);


            events.invokeEvent(SwarmFragment.TAG, SwarmFragment.NONE);
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
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

        PeersViewModel messagesViewModel = new ViewModelProvider(this).get(PeersViewModel.class);
        messagesViewModel.getPeers().observe(this, (peers) -> {

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
    public void invokeGeneralAction(@NonNull IPeer peer) {
        checkNotNull(peer);
        try {
            // mis-clicking prevention, using threshold of 1000 ms
            if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                return;
            }
            mLastClickTime = SystemClock.elapsedRealtime();

            FragmentManager fm = getChildFragmentManager();

            PeerActionDialogFragment.newInstance(
                    peer.getPID().getPid(), true, true, true)
                    .show(fm, PeerActionDialogFragment.TAG);

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }

    }

    @Override
    public boolean generalActionSupport(@NonNull IPeer peer) {
        return !peer.isDialing();
    }

}