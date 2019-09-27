package threads.server;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import threads.core.GatewayService;
import threads.core.Singleton;
import threads.core.THREADS;
import threads.core.api.IPeer;
import threads.core.mdl.EventViewModel;
import threads.core.mdl.PeersViewModel;
import threads.ipfs.IPFS;
import threads.share.DetailsDialogFragment;
import threads.share.PeerActionDialogFragment;
import threads.share.PeersViewAdapter;

import static androidx.core.util.Preconditions.checkNotNull;

@SuppressWarnings("ALL")
public class SwarmFragment extends Fragment implements
        SwipeRefreshLayout.OnRefreshListener, PeersViewAdapter.PeersViewAdapterListener {

    public static final String TAG = SwarmFragment.class.getSimpleName();
    static final String LOW = "LOW";
    static final String HIGH = "HIGH";
    static final String MEDIUM = "MEDIUM";
    static final String NONE = "NONE";
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private long mLastClickTime = 0;
    private PeersViewAdapter peersViewAdapter;
    private Context mContext;
    private FloatingActionButton fab_traffic;
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
        loadPeers();
    }


    private void loadPeers() {

        mSwipeRefreshLayout.setRefreshing(true);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {

            try {
                loadPeers(mContext);
            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            } finally {
                mHandler.post(() -> mSwipeRefreshLayout.setRefreshing(false));
            }
        });
    }

    private void loadPeers(@NonNull Context context) {
        checkNotNull(context);
        THREADS threads = Singleton.getInstance(context).getThreads();
        IPFS ipfs = Singleton.getInstance(context).getIpfs();

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
            threads.invokeEvent(SwarmFragment.TAG, content);


            threads.invokeEvent(SwarmFragment.TAG, SwarmFragment.NONE);
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }

    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.swarm_view, container, false);

        fab_traffic = view.findViewById(R.id.fab_traffic);
        fab_traffic.setOnClickListener((v) -> {

            if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                return;
            }
            mLastClickTime = SystemClock.elapsedRealtime();


            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    GatewayService.PeerSummary info = GatewayService.evaluateAllPeers(mContext);


                    String html = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?><html><body style=\"background-color:snow;\"><h3 style=\"text-align:center; color:teal;\">Quality</h3><ul>";


                    String numPeers = "Number Peers : " + info.getNumPeers();
                    html = html.concat("<li><div style=\"width: 80%;" +
                            "  word-wrap:break-word;\">").concat(numPeers).concat("</div></li>");


                    String latency = "Average Latency : n.a.";
                    if (info.getLatency() < Long.MAX_VALUE) {
                        latency = "Average Latency : " + info.getLatency() + " [ms]";
                    }


                    html = html.concat("<li><div style=\"width: 80%;" +
                            "  word-wrap:break-word;\">").concat(latency).concat("</div></li>");
                    html = html.concat("</ul></body><footer style=\"color:tomato;\">"
                            + getString(R.string.quality_measurement) + "</footer></html>");


                    DetailsDialogFragment.newInstance(
                            DetailsDialogFragment.Type.HTML, html).show(
                            getChildFragmentManager(),
                            DetailsDialogFragment.TAG);

                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
                }
            });


        });
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
                        List<threads.core.api.Peer> connected = new ArrayList<>();
                        for (threads.core.api.Peer peer : peers) {
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

        EventViewModel eventViewModel = new ViewModelProvider(this).get(EventViewModel.class);
        eventViewModel.getEvent(TAG).observe(this, (event) -> {
            try {
                if (event != null) {
                    String content = event.getContent();
                    switch (content) {
                        case HIGH:
                            fab_traffic.setBackgroundTintList(
                                    ContextCompat.getColorStateList(mContext, android.R.color.holo_green_light));
                            break;
                        case MEDIUM:
                            fab_traffic.setBackgroundTintList(
                                    ContextCompat.getColorStateList(mContext, android.R.color.holo_orange_light));
                            break;
                        case LOW:
                            fab_traffic.setBackgroundTintList(
                                    ContextCompat.getColorStateList(mContext, android.R.color.holo_red_light));
                            break;
                        default:
                            fab_traffic.setBackgroundTintList(
                                    ContextCompat.getColorStateList(mContext, android.R.color.holo_red_dark));

                    }
                }
            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }

        });
        loadPeers();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isVisible()) {
            Toast.makeText(mContext, R.string.drag_down_refresh, Toast.LENGTH_LONG).show();
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
