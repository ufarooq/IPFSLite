package threads.server.fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.gson.Gson;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import threads.ipfs.CID;
import threads.ipfs.IPFS;
import threads.ipfs.PID;
import threads.server.R;
import threads.server.core.contents.CDS;
import threads.server.core.contents.Contents;
import threads.server.core.events.EVENTS;
import threads.server.core.peers.User;
import threads.server.core.threads.THREADS;
import threads.server.core.threads.Thread;
import threads.server.model.LiteUsersViewModel;
import threads.server.utils.ContactsViewAdapter;
import threads.server.work.SendNotificationWorker;

import static androidx.core.util.Preconditions.checkNotNull;

public class SendDialogFragment extends BottomSheetDialogFragment implements ContactsViewAdapter.ValidateListener {
    public static final String TAG = SendDialogFragment.class.getSimpleName();
    static final String IDXS = "IDXS";
    private static final int CLICK_OFFSET = 500;
    private long mLastClickTime = 0;
    private ContactsViewAdapter mContactsViewAdapter;
    private Context mContext;
    private TextView mSendTo;
    private TextView mNoPeers;

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
        View view = inflater.inflate(R.layout.send_view, container, false);


        Bundle args = getArguments();
        checkNotNull(args);
        final long[] indices = args.getLongArray(IDXS);

        RecyclerView recycler_view_contact_list = view.findViewById(R.id.send_contact_list);

        mNoPeers = view.findViewById(R.id.no_peers);
        mNoPeers.setVisibility(View.GONE);
        mSendTo = view.findViewById(R.id.send_to);
        recycler_view_contact_list.setLayoutManager(new LinearLayoutManager(getContext()));
        mContactsViewAdapter = new ContactsViewAdapter(this);
        recycler_view_contact_list.setAdapter(mContactsViewAdapter);


        mSendTo.setOnClickListener((v) -> {

            if (SystemClock.elapsedRealtime() - mLastClickTime < CLICK_OFFSET) {
                return;
            }

            mLastClickTime = SystemClock.elapsedRealtime();


            List<User> users = mContactsViewAdapter.getSelectedAccounts();

            sendThreads(users, indices);

            dismiss();

        });

        LiteUsersViewModel messagesViewModel = new ViewModelProvider(this).
                get(LiteUsersViewModel.class);
        messagesViewModel.getLiteUsers().observe(this, (peers) -> {

            try {
                if (peers != null) {
                    try {
                        if (peers.isEmpty()) {
                            mNoPeers.setVisibility(View.VISIBLE);
                        } else {
                            mNoPeers.setVisibility(View.GONE);
                        }
                        peers.sort(Comparator.comparing(User::getAlias));

                        mContactsViewAdapter.setAccounts(peers);
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
    public void validate() {

        boolean result = !mContactsViewAdapter.getSelectedAccounts().isEmpty();

        mSendTo.setEnabled(result);

        if (result) {
            mSendTo.setTextColor(mContext.getColor(R.color.colorAccent));
        } else {
            mSendTo.setTextColor(android.R.attr.textColorSecondary);
        }
    }


    private void sendThreads(@NonNull List<User> users, long[] indices) {
        checkNotNull(users);
        checkNotNull(indices);

        if (users.isEmpty()) {
            EVENTS.getInstance(mContext).postError(
                    mContext.getString(R.string.no_sharing_peers));
        } else {
            EVENTS.getInstance(mContext).postWarning(
                    mContext.getString(R.string.send_notifications));
            Gson gson = new Gson();
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {

                    Contents contents = new Contents();
                    THREADS threads = THREADS.getInstance(mContext);
                    List<Thread> threadList = threads.getThreadsByIdx(indices);

                    contents.add(threadList);

                    String data = gson.toJson(contents);
                    IPFS ipfs = IPFS.getInstance(mContext);
                    CID cid = ipfs.storeText(data);
                    checkNotNull(cid);
                    PID host = IPFS.getPID(mContext);
                    checkNotNull(host);
                    CDS contentService = CDS.getInstance(mContext);
                    contentService.insertContent(host.getPid(), cid.getCid(), true);

                    SendNotificationWorker.sendUsers(mContext, users, cid.getCid());

                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
                }
            });
        }
    }

}
