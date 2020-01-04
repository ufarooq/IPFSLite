package threads.server;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Base64;
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
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.selection.Selection;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.selection.StorageStrategy;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import threads.core.MimeType;
import threads.core.Network;
import threads.core.Preferences;
import threads.core.Singleton;
import threads.core.THREADS;
import threads.core.api.Content;
import threads.core.api.Status;
import threads.core.api.Thread;
import threads.core.mdl.ThreadViewModel;
import threads.ipfs.IPFS;
import threads.ipfs.api.CID;
import threads.share.AudioDialogFragment;
import threads.share.ImageDialogFragment;
import threads.share.PDFView;
import threads.share.ThreadActionDialogFragment;
import threads.share.VideoDialogFragment;
import threads.share.WebViewDialogFragment;

import static androidx.core.util.Preconditions.checkNotNull;

public class ThreadsFragment extends Fragment implements
        SwipeRefreshLayout.OnRefreshListener, ThreadsViewAdapter.ThreadsViewAdapterListener {

    private static final String TAG = ThreadsFragment.class.getSimpleName();

    private static final int CLICK_OFFSET = 500;
    @NonNull
    private final AtomicReference<LiveData<List<Thread>>> observer = new AtomicReference<>(null);
    @NonNull
    private final AtomicBoolean topLevel = new AtomicBoolean(true);
    @NonNull
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private SelectionViewModel mSelectionViewModel;

    private ThreadsViewAdapter mThreadsViewAdapter;
    private ThreadViewModel threadViewModel;
    private long mLastClickTime = 0;
    private Context mContext;
    private ThreadsFragment.ActionListener mListener;
    private RecyclerView mRecyclerView;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private ActionMode mActionMode;
    private SelectionTracker<Long> mSelectionTracker;

    private static String getCompactString(@NonNull String title) {
        checkNotNull(title);
        return title.replace("\n", " ");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
        try {
            mListener = (ThreadsFragment.ActionListener) getActivity();
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mContext = null;
        mListener = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        mSelectionTracker.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);


        mSelectionViewModel = new ViewModelProvider(getActivity()).get(SelectionViewModel.class);


        final Observer<Long> parentThread = new Observer<Long>() {
            @Override
            public void onChanged(@Nullable final Long threadIdx) {
                if (threadIdx != null) {
                    updateDirectory(threadIdx);
                }
            }
        };
        mSelectionViewModel.getParentThread().observe(this, parentThread);


    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_threads, menu);
        MenuItem actionDaemon = menu.findItem(R.id.action_daemon);
        if (!DaemonService.DAEMON_RUNNING.get()) {
            actionDaemon.setIcon(R.drawable.play_circle);
        } else {
            actionDaemon.setIcon(R.drawable.stop_circle);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.threads_view, container, false);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_mark_all: {

                if (SystemClock.elapsedRealtime() - mLastClickTime < CLICK_OFFSET) {
                    break;
                }
                mLastClickTime = SystemClock.elapsedRealtime();

                mThreadsViewAdapter.selectAllThreads();

                return true;
            }
            case R.id.action_daemon: {

                if (SystemClock.elapsedRealtime() - mLastClickTime < CLICK_OFFSET) {
                    break;
                }
                mLastClickTime = SystemClock.elapsedRealtime();

                if (DaemonService.DAEMON_RUNNING.get()) {
                    DaemonService.DAEMON_RUNNING.set(false);
                } else {
                    DaemonService.DAEMON_RUNNING.set(true);
                }
                DaemonService.invoke(mContext);

                getActivity().invalidateOptionsMenu();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        threadViewModel = new ViewModelProvider(this).get(ThreadViewModel.class);

        mRecyclerView = view.findViewById(R.id.recycler_view_message_list);
        mRecyclerView.setItemAnimator(null); // no animation of the item when something changed

        mSwipeRefreshLayout = view.findViewById(R.id.swipe_container);
        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorAccent,
                android.R.color.holo_green_dark,
                android.R.color.holo_orange_dark,
                android.R.color.holo_blue_dark);

        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(mContext);
        mRecyclerView.setLayoutManager(linearLayoutManager);

        mThreadsViewAdapter = new ThreadsViewAdapter(mContext, this);
        mRecyclerView.setAdapter(mThreadsViewAdapter);

        mSelectionTracker = new SelectionTracker.Builder<>(
                "threds-selection",//unique id
                mRecyclerView,
                new ThreadsItemKeyProvider(mThreadsViewAdapter),
                new ThreadItemDetailsLookup(mRecyclerView),
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
                                getActivity()).startSupportActionMode(
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
                                getActivity()).startSupportActionMode(
                                createActionModeCallback());
                    }
                }
                if (mActionMode != null) {
                    mActionMode.setTitle("" + mSelectionTracker.getSelection().size());
                }
                super.onSelectionRestored();
            }
        });

        mThreadsViewAdapter.setSelectionTracker(mSelectionTracker);


        if (savedInstanceState != null) {
            mSelectionTracker.onRestoreInstanceState(savedInstanceState);
        }

    }

    private void updateDirectory(long thread) {
        try {
            topLevel.set(thread == 0L);

            LiveData<List<Thread>> obs = observer.get();
            if (obs != null) {
                obs.removeObservers(this);
            }

            LiveData<List<Thread>> liveData = threadViewModel.getThreadsByThread(thread);
            observer.set(liveData);

            liveData.observe(getViewLifecycleOwner(), (threads) -> {

                if (threads != null) {

                    List<Thread> data = new ArrayList<>();
                    for (Thread threadObject : threads) {
                        if (threadObject.getStatus() != Status.DELETING) {
                            data.add(threadObject);
                        }
                    }
                    data.sort(Comparator.comparing(Thread::getDate).reversed());

                    boolean scrollToTop = false;

                    if (!data.isEmpty()) {
                        int pos = mThreadsViewAdapter.getPositionOfItem(data.get(0).getIdx());
                        scrollToTop = pos != 0;
                    }

                    mThreadsViewAdapter.updateData(data);

                    if (scrollToTop) {
                        mRecyclerView.postDelayed(() -> {

                            try {
                                mRecyclerView.scrollToPosition(0);
                            } catch (Throwable e) {
                                Log.e(TAG, "" + e.getLocalizedMessage(), e);
                            }

                        }, 10);
                    }
                }
            });
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }


    private void sendAction() {

        Selection<Long> selection = mSelectionTracker.getSelection();
        if (selection.size() == 0) {
            THREADS threads = Singleton.getInstance(mContext).getThreads();
            Preferences.warning(threads,
                    mContext.getString(R.string.no_marked_file_send));
            return;
        }

        try {

            long[] entries = convert(selection);

            mListener.clickThreadsSend(entries);

            mSelectionTracker.clearSelection();
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }


    private void clearUnreadNotes() {
        final THREADS threadsAPI = Singleton.getInstance(mContext).getThreads();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                Long idx = mSelectionViewModel.getParentThread().getValue();
                checkNotNull(idx);
                threadsAPI.resetThreadNumber(idx);
            } catch (Throwable e) {
                Preferences.evaluateException(threadsAPI, Preferences.EXCEPTION, e);
            }
        });
    }


    private long[] convert(Selection<Long> entries) {
        long[] basic = new long[entries.size()];
        int i = 0;
        for (Long entry : entries) {
            basic[i] = entry;
            i++;
        }

        return basic;
    }

    private void deleteAction() {


        if (!topLevel.get()) {
            THREADS threads = Singleton.getInstance(mContext).getThreads();
            Preferences.warning(threads,
                    mContext.getString(R.string.deleting_files_within_directory_not_supported));
            return;
        }

        if (!mSelectionTracker.hasSelection()) {
            THREADS threads = Singleton.getInstance(mContext).getThreads();
            Preferences.warning(threads,
                    mContext.getString(R.string.no_marked_file_delete));
            return;
        }

        long[] entries = convert(mSelectionTracker.getSelection());

        JobServiceDeleteThreads.removeThreads(mContext, entries);

        try {

            mSelectionTracker.clearSelection();

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }

    }


    @Override
    public boolean generalActionSupport(@NonNull Thread thread) {
        checkNotNull(thread);
        if (thread.isPublishing()) {
            return false;
        }
        return thread.getStatus() == Status.DONE;
    }

    @Override
    public void invokeGeneralAction(@NonNull Thread thread) {
        try {
            // mis-clicking prevention, using threshold of 1000 ms
            if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                return;
            }
            mLastClickTime = SystemClock.elapsedRealtime();

            boolean sendActive = Service.isSendNotificationsEnabled(mContext);

            ThreadActionDialogFragment.newInstance(
                    thread.getIdx(), true, true,
                    topLevel.get(), true, sendActive,
                    true, true, thread.isPinned())
                    .show(getChildFragmentManager(), ThreadActionDialogFragment.TAG);

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }


    private ActionMode.Callback createActionModeCallback() {
        return new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                mode.getMenuInflater().inflate(R.menu.menu_threads_action_mode, menu);
                MenuItem action_send = menu.findItem(R.id.action_mode_send);
                boolean active = Service.isSendNotificationsEnabled(mContext);
                action_send.setVisible(active);

                mListener.showBottomNavigation(false);
                mListener.setPagingEnabled(false);
                mListener.showMainFab(false);
                mHandler.post(() -> mThreadsViewAdapter.notifyDataSetChanged());

                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return true;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_mode_delete: {

                        if (SystemClock.elapsedRealtime() - mLastClickTime < CLICK_OFFSET) {
                            break;
                        }
                        mLastClickTime = SystemClock.elapsedRealtime();

                        deleteAction();

                        return true;
                    }

                    case R.id.action_mode_send: {

                        if (SystemClock.elapsedRealtime() - mLastClickTime < CLICK_OFFSET) {
                            break;
                        }
                        mLastClickTime = SystemClock.elapsedRealtime();

                        sendAction();

                        return true;
                    }
                }
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {

                mSelectionTracker.clearSelection();
                clearUnreadNotes();
                mListener.showBottomNavigation(true);
                mListener.setPagingEnabled(true);
                mListener.showMainFab(true);
                if (mActionMode != null) {
                    mActionMode = null;
                }
                mHandler.post(() -> mThreadsViewAdapter.notifyDataSetChanged());

            }
        };

    }

    @Override
    public void onClick(@NonNull Thread thread) {
        checkNotNull(thread);
        try {
            if (SystemClock.elapsedRealtime() - mLastClickTime < 500) {
                return;
            }
            mLastClickTime = SystemClock.elapsedRealtime();

            if (!mSelectionTracker.hasSelection()) {
                long threadIdx = thread.getIdx();


                final THREADS threads = Singleton.getInstance(mContext).getThreads();

                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.submit(() -> {
                    try {
                        int children = threads.getThreadReferences(threadIdx);
                        if (children > 0) {
                            mSelectionViewModel.setParentThread(threadIdx, true);
                        } else {
                            mHandler.post(() -> clickThreadPlay(threadIdx));
                        }
                    } catch (Throwable e) {
                        Preferences.evaluateException(threads, Preferences.EXCEPTION, e);
                    }
                });
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }

    }


    private void clickThreadPlay(long idx) {

        final THREADS threads = Singleton.getInstance(mContext).getThreads();
        final IPFS ipfs = Singleton.getInstance(mContext).getIpfs();
        final int timeout = Preferences.getConnectionTimeout(mContext);
        if (ipfs != null) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    Thread thread = threads.getThreadByIdx(idx);
                    checkNotNull(thread);
                    Status status = thread.getStatus();
                    if (status == Status.DONE) {

                        CID cid = thread.getCid();
                        checkNotNull(cid);

                        String filename = thread.getAdditionalValue(Content.FILENAME);
                        String mimeType = thread.getMimeType();
                        String fileSize = thread.getAdditionalValue(Content.FILESIZE);
                        long size = Long.valueOf(fileSize);

                        if (mimeType.startsWith("image")) {
                            ImageDialogFragment.newInstance(cid.getCid()).show(
                                    getChildFragmentManager(), ImageDialogFragment.TAG);

                        } else if (mimeType.startsWith("video")) {

                            if (size >= 1e+8) { // 100 MB
                                Intent intent = new Intent(mContext, VideoActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                                intent.putExtra(VideoActivity.CID_ID, cid.getCid());
                                startActivity(intent);

                            } else {
                                VideoDialogFragment dialogFragment = VideoDialogFragment.newInstance(
                                        cid.getCid());

                                getChildFragmentManager().beginTransaction().
                                        add(dialogFragment, VideoDialogFragment.TAG).
                                        commitAllowingStateLoss();
                            }

                        } else if (mimeType.startsWith("audio")) {

                            AudioDialogFragment.newInstance(cid.getCid(), filename,
                                    thread.getSenderAlias())
                                    .show(getChildFragmentManager(), AudioDialogFragment.TAG);


                        } else if (mimeType.startsWith(MimeType.PDF_MIME_TYPE)) {

                            PDFView.with(getActivity())
                                    .cid(cid.getCid())
                                    .swipeHorizontal(true)
                                    .start();

                        } else if (mimeType.equals(MimeType.LINK_MIME_TYPE)) {
                            byte[] data = ipfs.getData(cid, timeout, true);
                            checkNotNull(data);
                            Uri uri = Uri.parse(new String(data));

                            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);

                        } else if (mimeType.startsWith("text")) {

                            byte[] data = ipfs.getData(cid, timeout, true);
                            checkNotNull(data);

                            String content = Base64.encodeToString(data, Base64.NO_PADDING);
                            WebViewDialogFragment.newInstance(mimeType, content, "base64").
                                    show(getChildFragmentManager(), WebViewDialogFragment.TAG);

                        } else if (mimeType.equals(MimeType.OCTET_MIME_TYPE)) {
                            byte[] data = ipfs.getData(cid, timeout, true);
                            checkNotNull(data);
                            int length = data.length;
                            if (length > 0 && length < 64000) {
                                String content = new String(data);
                                WebViewDialogFragment.newInstance(WebViewDialogFragment.Type.TEXT, content).
                                        show(getChildFragmentManager(), WebViewDialogFragment.TAG);
                            }
                        }
                    }
                } catch (Throwable ex) {
                    Preferences.error(threads, getString(R.string.no_activity_found_to_handle_uri));
                }
            });
        }
    }


    @Override
    public void invokeActionError(@NonNull Thread thread) {
        checkNotNull(thread);
        try {
            // CHECKED
            Singleton singleton = Singleton.getInstance(mContext);
            if (!Network.isConnected(mContext)) {
                Preferences.error(singleton.getThreads(), getString(R.string.offline_mode));
                return;
            }


            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    Service.getInstance(mContext).downloadThread(mContext, thread);
                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
                }
            });

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    @Override
    @NonNull
    public String getContent(@NonNull Thread thread) {
        checkNotNull(thread);

        return thread.getSenderAlias();
    }


    @NonNull
    @Override
    public String getTitle(@NonNull Thread thread) {
        return getCompactString(thread.getAdditionalValue(Content.FILENAME));
    }


    @Override
    public int getMediaResource(@NonNull Thread thread) {
        checkNotNull(thread);
        return -1;
    }

    @Override
    public void onRefresh() {
        loadNotifications();
    }

    private void loadNotifications() {

        mSwipeRefreshLayout.setRefreshing(true);

        try {
            JobServiceLoadNotifications.notifications(mContext);
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        } finally {
            mHandler.post(() -> mSwipeRefreshLayout.setRefreshing(false));
        }

    }


    public interface ActionListener {

        void clickThreadsSend(long[] idxs);

        void showBottomNavigation(boolean visible);

        void showMainFab(boolean visible);

        void setPagingEnabled(boolean enabled);
    }
}
