package threads.server;

import android.content.Context;
import android.os.Bundle;
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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import threads.core.GatewayService;
import threads.core.api.IPeer;
import threads.core.mdl.EventViewModel;
import threads.core.mdl.PeersViewModel;
import threads.share.PeersViewAdapter;
import threads.share.UserActionDialogFragment;
import threads.share.WebViewDialogFragment;

import static androidx.core.util.Preconditions.checkNotNull;

public class SwarmFragment extends Fragment implements PeersViewAdapter.PeersViewAdapterListener {

    public static final String TAG = SwarmFragment.class.getSimpleName();
    public static final String LOW = "LOW";
    public static final String HIGH = "HIGH";
    public static final String MEDIUM = "MEDIUM";
    public static final String NONE = "NONE";

    private long mLastClickTime = 0;


    private PeersViewAdapter peersViewAdapter;
    private ActionListener actionListener;
    private Context mContext;
    private FloatingActionButton fab_traffic;

    @Override
    public void onDetach() {
        super.onDetach();
        Service.getInstance(mContext).swarmCheckEnable(false);
        mContext = null;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
        try {
            actionListener = (SwarmFragment.ActionListener) getActivity();
            Service.getInstance(context).swarmCheckEnable(true);
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    private void swarmOnlineStatus() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                Service.getInstance(mContext).checkSwarmOnlineStatus(mContext);
            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }
        });
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_swarm, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        return super.onOptionsItemSelected(item);
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


                    String html = "<html><h2 align=\"center\">Quality</h2><ul>";


                    String numPeers = "Number Peers : " + info.getNumPeers();
                    html = html.concat("<li><div style=\"width: 80%;" +
                            "  word-wrap:break-word;\">").concat(numPeers).concat("</div></li>");


                    String latency = "Average Latency : n.a.";
                    if (info.getLatency() < Long.MAX_VALUE) {
                        latency = "Average Latency : " + info.getLatency() + " [ms]";
                    }


                    html = html.concat("<li><div style=\"width: 80%;" +
                            "  word-wrap:break-word;\">").concat(latency).concat("</div></li>");
                    html = html.concat("</ul></html>");


                    WebViewDialogFragment.newInstance(
                            WebViewDialogFragment.Type.HTML, html).show(
                            getChildFragmentManager(),
                            WebViewDialogFragment.TAG);


                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
                }
            });


        });
        RecyclerView mRecyclerView = view.findViewById(R.id.recycler_users);
        mRecyclerView.setItemAnimator(null); // no animation of the item when something changed

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
                    if (content.equals(HIGH)) {
                        fab_traffic.setBackgroundTintList(
                                ContextCompat.getColorStateList(mContext, android.R.color.holo_green_light));
                    } else if (content.equals(MEDIUM)) {
                        fab_traffic.setBackgroundTintList(
                                ContextCompat.getColorStateList(mContext, android.R.color.holo_orange_light));
                    } else if (content.equals(LOW)) {
                        fab_traffic.setBackgroundTintList(
                                ContextCompat.getColorStateList(mContext, android.R.color.holo_red_light));
                    } else {
                        fab_traffic.setBackgroundTintList(
                                ContextCompat.getColorStateList(mContext, android.R.color.holo_red_dark));
                    }
                }
            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }

        });
        swarmOnlineStatus();

        return view;
    }


    @Override
    public void invokeGeneralAction(@NonNull IPeer peer) {
        checkNotNull(peer);
        try {

            FragmentManager fm = getChildFragmentManager();

            UserActionDialogFragment.newInstance(
                    peer.getPID().getPid(), true, false,
                    false, false, false, false)
                    .show(fm, UserActionDialogFragment.TAG);

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }

    }

    @Override
    public boolean generalActionSupport(@NonNull IPeer peer) {
        return !peer.isDialing();
    }

    public interface ActionListener {

        void clickInfoPeer();
    }
}
