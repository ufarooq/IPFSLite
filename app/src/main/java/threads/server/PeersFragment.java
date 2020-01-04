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
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import threads.core.Preferences;
import threads.core.Singleton;
import threads.core.THREADS;
import threads.core.api.User;
import threads.core.api.UserType;
import threads.core.mdl.UsersViewModel;
import threads.ipfs.IPFS;
import threads.ipfs.api.PID;
import threads.ipfs.api.PeerInfo;
import threads.share.DetailsDialogFragment;
import threads.share.UserActionDialogFragment;
import threads.share.UsersViewAdapter;

import static androidx.core.util.Preconditions.checkNotNull;

public class PeersFragment extends Fragment implements UsersViewAdapter.UsersViewAdapterListener {

    private static final String TAG = PeersFragment.class.getSimpleName();
    private long mLastClickTime = 0;


    private UsersViewAdapter usersViewAdapter;
    private ActionListener mListener;
    private Context mContext;


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

                mListener.clickInfoPeer();

                return true;
            }

            case R.id.action_id: {

                // mis-clicking prevention, using threshold of 1000 ms
                if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                    break;
                }
                mLastClickTime = SystemClock.elapsedRealtime();


                final THREADS threads = Singleton.getInstance(mContext).getThreads();
                final IPFS ipfs = Singleton.getInstance(mContext).getIpfs();
                if (ipfs != null) {
                    ExecutorService executor = Executors.newSingleThreadExecutor();
                    executor.submit(() -> {
                        try {
                            PeerInfo info = ipfs.id();
                            checkNotNull(info);
                            String html = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?><html><body style=\"background-color:snow;\"><h3 style=\"text-align:center; color:teal;\">Addresses</h3><ul>";
                            List<String> addresses = info.getMultiAddresses();
                            for (String address : addresses) {
                                html = html.concat("<li><div style=\"width: 80%;" +
                                        "  word-wrap:break-word;\">").concat(address).concat("</div></li>");
                            }
                            String agentVersion = info.getAgentVersion();
                            html = html.concat("</ul><br/></body><footer>Agent : <strong style=\"color:teal;\">" + agentVersion + "</strong></footer></html>");


                            DetailsDialogFragment.newInstance(
                                    DetailsDialogFragment.Type.HTML, html).show(
                                    getChildFragmentManager(),
                                    DetailsDialogFragment.TAG);

                        } catch (Throwable e) {
                            // ignore exception for now
                            Preferences.evaluateException(threads, Preferences.EXCEPTION, e);
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


        RecyclerView mRecyclerView = view.findViewById(R.id.recycler_users);
        mRecyclerView.setItemAnimator(null); // no animation of the item when something changed

        LinearLayoutManager linearLayout = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(linearLayout);
        usersViewAdapter = new UsersViewAdapter(mContext, this);
        mRecyclerView.setAdapter(usersViewAdapter);

        final PID host = Preferences.getPID(mContext);
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

                        usersViewAdapter.updateData(peers);
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
            // mis-clicking prevention, using threshold of 1000 ms
            if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                return;
            }
            mLastClickTime = SystemClock.elapsedRealtime();

            boolean valid = user.isValid();
            boolean verified = user.getType() == UserType.VERIFIED;

            UserActionDialogFragment.newInstance(
                    user.getPID().getPid(), true, true, true,
                    valid, user.isAutoConnect(), true, true, user.isBlocked(),
                    true, false, false)
                    .show(getChildFragmentManager(), UserActionDialogFragment.TAG);

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }

    }

    @Override
    public boolean generalActionSupport(@NonNull User user) {
        return !user.isDialing();
    }

    public interface ActionListener {

        void clickInfoPeer();
    }
}
