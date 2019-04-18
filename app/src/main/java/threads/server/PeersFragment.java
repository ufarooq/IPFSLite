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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import threads.core.Preferences;
import threads.core.Singleton;
import threads.core.api.User;
import threads.core.api.UserStatus;
import threads.core.mdl.UsersViewModel;
import threads.ipfs.IPFS;
import threads.ipfs.api.PID;
import threads.ipfs.api.PeerInfo;
import threads.share.UserActionDialogFragment;
import threads.share.UsersViewAdapter;
import threads.share.WebViewDialogFragment;

import static androidx.core.util.Preconditions.checkNotNull;

public class PeersFragment extends Fragment implements UsersViewAdapter.UsersViewAdapterListener {

    private long mLastClickTime = 0;


    private UsersViewAdapter usersViewAdapter;
    private ActionListener actionListener;
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

            case R.id.action_id: {

                // mis-clicking prevention, using threshold of 1000 ms
                if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                    break;
                }
                mLastClickTime = SystemClock.elapsedRealtime();


                final IPFS ipfs = Singleton.getInstance().getIpfs();
                if (ipfs != null) {
                    ExecutorService executor = Executors.newSingleThreadExecutor();
                    executor.submit(() -> {
                        try {
                            PeerInfo info = ipfs.id();

                            String html = "<html><h2>Addresses</h2><ul>";
                            List<String> addresses = info.getMultiAddresses();
                            for (String address : addresses) {
                                html = html.concat("<li><div style=\"width: 80%;" +
                                        "  word-wrap:break-word;\">").concat(address).concat("</div></li>");
                            }
                            String agent = info.getAgentVersion();
                            html = html.concat("</ul><br/><footer>Version : " + agent + "</footer></html>");

                            Activity activity = getActivity();
                            if (activity != null) {
                                WebViewDialogFragment.newInstance(
                                        WebViewDialogFragment.Type.HTML, html).show(
                                        getActivity().getSupportFragmentManager(),
                                        WebViewDialogFragment.TAG);
                            }

                        } catch (Throwable e) {
                            // ignore exception for now
                            Preferences.evaluateException(Preferences.EXCEPTION, e);
                        }
                    });

                }


                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

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
                PeersDialogFragment.newInstance(true, true)
                        .show(getActivity().getSupportFragmentManager(), PeersDialogFragment.TAG);
            }

        });


        RecyclerView mRecyclerView = view.findViewById(R.id.recycler_users);
        mRecyclerView.setItemAnimator(null); // no animation of the item when something changed

        LinearLayoutManager linearLayout = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(linearLayout);
        usersViewAdapter = new UsersViewAdapter(mContext, this);
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
        try {
            if (getActivity() != null) {
                FragmentManager fm = getActivity().getSupportFragmentManager();
                boolean relayActive = Preferences.isAutoRelayEnabled(mContext);
                UserActionDialogFragment.newInstance(
                        user.getPID().getPid(), true, true,
                        true, true, true, relayActive)
                        .show(fm, UserActionDialogFragment.TAG);
            }
        } catch (Throwable e) {
            Preferences.evaluateException(Preferences.EXCEPTION, e);
        }

    }

    @Override
    public boolean generalActionSupport(@NonNull User user) {
        return user.getStatus() != UserStatus.DIALING;
    }

    public interface ActionListener {

        void clickUserInfo();
    }
}
