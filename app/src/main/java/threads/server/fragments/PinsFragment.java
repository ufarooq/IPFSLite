package threads.server.fragments;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
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
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.selection.Selection;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.selection.StorageStrategy;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import threads.ipfs.CID;
import threads.ipfs.IPFS;
import threads.server.R;
import threads.server.core.events.EVENTS;
import threads.server.core.threads.Status;
import threads.server.core.threads.THREADS;
import threads.server.core.threads.Thread;
import threads.server.model.PinsViewModel;
import threads.server.services.DeleteThreadsService;
import threads.server.services.LiteService;
import threads.server.utils.Network;
import threads.server.utils.PinsItemDetailsLookup;
import threads.server.utils.PinsItemKeyProvider;
import threads.server.utils.PinsViewAdapter;
import threads.server.work.PublishContentWorker;
import threads.server.work.PublisherChain;

import static androidx.core.util.Preconditions.checkNotNull;

public class PinsFragment extends Fragment implements PinsViewAdapter.PinsViewAdapterListener {

    private static final String TAG = PinsFragment.class.getSimpleName();

    private static final int CLICK_OFFSET = 500;

    @NonNull
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private PinsViewAdapter mPinsViewAdapter;
    private long mLastClickTime = 0;
    private Context mContext;
    private FragmentActivity mActivity;
    private PinsFragment.ActionListener mListener;
    private RecyclerView mRecyclerView;
    private ActionMode mActionMode;
    private SelectionTracker<Long> mSelectionTracker;


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
        mActivity = getActivity();
        mListener = (PinsFragment.ActionListener) mActivity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mContext = null;
        mActivity = null;
        mListener = null;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mSelectionTracker != null) {
            mSelectionTracker.onSaveInstanceState(outState);
        }

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);
        menuInflater.inflate(R.menu.menu_pins_fragment, menu);
    }


    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (item.getItemId() == R.id.action_select_all) {

            if (SystemClock.elapsedRealtime() - mLastClickTime < CLICK_OFFSET) {
                return true;
            }

            mLastClickTime = SystemClock.elapsedRealtime();

            mPinsViewAdapter.selectAllThreads();

            return true;

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.pins_view, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        PinsViewModel mPinsViewModel = new ViewModelProvider(this).get(PinsViewModel.class);
        mPinsViewModel.getVisiblePinnedThreads().observe(this, (threads) -> {

            if (threads != null) {

                threads.sort(Comparator.comparing(Thread::getLastModified).reversed());


                int size = mPinsViewAdapter.getItemCount();
                boolean scrollToTop = size < threads.size();


                mPinsViewAdapter.updateData(threads);

                if (scrollToTop) {
                    try {
                        mRecyclerView.scrollToPosition(0);
                    } catch (Throwable e) {
                        Log.e(TAG, "" + e.getLocalizedMessage(), e);
                    }
                }
            }
        });


        mRecyclerView = view.findViewById(R.id.recycler_view_pins);

        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(mContext);
        mRecyclerView.setLayoutManager(linearLayoutManager);

        mPinsViewAdapter = new PinsViewAdapter(mContext, this);
        mRecyclerView.setAdapter(mPinsViewAdapter);


        mSelectionTracker = new SelectionTracker.Builder<>(
                "threads-selection",//unique id
                mRecyclerView,
                new PinsItemKeyProvider(mPinsViewAdapter),
                new PinsItemDetailsLookup(mRecyclerView),
                StorageStrategy.createLongStorage())
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
                                mActivity).startSupportActionMode(
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
                                mActivity).startSupportActionMode(
                                createActionModeCallback());
                    }
                }
                if (mActionMode != null) {
                    mActionMode.setTitle("" + mSelectionTracker.getSelection().size());
                }
                super.onSelectionRestored();
            }
        });

        mPinsViewAdapter.setSelectionTracker(mSelectionTracker);


        if (savedInstanceState != null) {
            mSelectionTracker.onRestoreInstanceState(savedInstanceState);
        }

    }

    private void unpinAction() {

        Selection<Long> selection = mSelectionTracker.getSelection();
        if (selection.size() == 0) {
            EVENTS.getInstance(mContext).postWarning(getString(R.string.no_marked_file_unpin));
            return;
        }

        try {

            long[] entries = convert(selection);

            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    THREADS.getInstance(mContext).setThreadsUnpin(entries);
                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
                }
            });

            mSelectionTracker.clearSelection();
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    private long[] convert(Selection<Long> entries) {
        int i = 0;

        long[] basic = new long[entries.size()];
        for (Long entry : entries) {
            basic[i] = entry;
            i++;
        }

        return basic;
    }

    private void deleteAction() {


        if (!mSelectionTracker.hasSelection()) {
            EVENTS.getInstance(mContext).postWarning(getString(R.string.no_marked_file_delete));
            return;
        }


        try {
            long[] entries = convert(mSelectionTracker.getSelection());

            DeleteThreadsService.removeThreads(mContext, entries);

            mSelectionTracker.clearSelection();

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }

    }

    @Override
    public void invokeAction(@NonNull Thread thread, @NonNull View view) {
        checkNotNull(thread);
        checkNotNull(view);


        try {
            PopupMenu menu = new PopupMenu(mContext, view);
            menu.inflate(R.menu.popup_pins_menu);
            menu.setOnMenuItemClickListener((item) -> {

                if (SystemClock.elapsedRealtime() - mLastClickTime < CLICK_OFFSET) {
                    return true;
                }
                mLastClickTime = SystemClock.elapsedRealtime();

                if (item.getItemId() == R.id.popup_info) {
                    clickThreadInfo(thread.getIdx());
                    return true;
                } else if (item.getItemId() == R.id.popup_view) {
                    clickThreadView(thread);
                    return true;
                } else if (item.getItemId() == R.id.popup_delete) {
                    clickThreadDelete(thread.getIdx());
                    return true;
                } else if (item.getItemId() == R.id.popup_unpin) {
                    clickUnpin(thread.getIdx());
                    return true;
                } else if (item.getItemId() == R.id.popup_publish) {
                    clickPublish(thread);
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


    private void clickPublish(@NonNull Thread thread) {

        if (!Network.isConnected(mContext)) {
            EVENTS.getInstance(mContext).postWarning(getString(R.string.offline_mode));
        }


        CID cid = thread.getContent();
        if (cid != null) {
            THREADS.getInstance(mContext).setThreadStatus(thread.getIdx(), Status.STARTED);
            PublisherChain.publish(mContext, cid, thread.getIdx());
        }

    }

    private void clickUnpin(long idx) {

        final THREADS threads = THREADS.getInstance(mContext);
        final EVENTS events = EVENTS.getInstance(mContext);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                threads.setThreadPinned(idx, false);
                threads.setThreadStatus(idx, Status.UNKNOWN);
            } catch (Throwable e) {
                events.exception(e);
            }
        });

    }

    private ActionMode.Callback createActionModeCallback() {
        return new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                mode.getMenuInflater().inflate(R.menu.menu_pins_action_mode, menu);

                mListener.showBottomNavigation(false);
                mListener.setPagingEnabled(false);

                mHandler.post(() -> mPinsViewAdapter.notifyDataSetChanged());

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

                        mPinsViewAdapter.selectAllThreads();

                        return true;
                    }
                    case R.id.action_mode_delete: {

                        if (SystemClock.elapsedRealtime() - mLastClickTime < CLICK_OFFSET) {
                            break;
                        }
                        mLastClickTime = SystemClock.elapsedRealtime();

                        deleteAction();

                        return true;
                    }

                    case R.id.action_mode_unpin: {

                        if (SystemClock.elapsedRealtime() - mLastClickTime < CLICK_OFFSET) {
                            break;
                        }
                        mLastClickTime = SystemClock.elapsedRealtime();

                        unpinAction();

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

                if (mActionMode != null) {
                    mActionMode = null;
                }
                mHandler.post(() -> mPinsViewAdapter.notifyDataSetChanged());

            }
        };

    }


    private void clickThreadInfo(long idx) {
        try {
            final EVENTS events = EVENTS.getInstance(mContext);
            final THREADS threads = THREADS.getInstance(mContext);

            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    CID cid = threads.getThreadContent(idx);
                    checkNotNull(cid);
                    String multihash = cid.getCid();

                    InfoDialogFragment.newInstance(multihash,
                            getString(R.string.content_id),
                            getString(R.string.multi_hash_access, multihash))
                            .show(getChildFragmentManager(), InfoDialogFragment.TAG);


                } catch (Throwable e) {
                    events.exception(e);
                }
            });

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }

    }


    private void clickThreadView(@NonNull Thread thread) {

        CID cid = thread.getContent();
        checkNotNull(cid);

        PublishContentWorker.publish(mContext, cid);

        try {
            String gateway = LiteService.getGateway(mContext);
            Uri uri = Uri.parse(gateway + "/" + IPFS.Style.ipfs + "/" + cid.getCid());

            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);

        } catch (Throwable e) {
            EVENTS.getInstance(mContext).postError("" + e.getLocalizedMessage());
        }
    }


    private void clickThreadDelete(long idx) {
        DeleteThreadsService.removeThreads(mContext, idx);
    }


    public interface ActionListener {

        void showBottomNavigation(boolean visible);

        void setPagingEnabled(boolean enabled);

    }
}
