package threads.server.fragments;

import android.app.SearchManager;
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
import android.view.inputmethod.InputMethodManager;
import android.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.core.app.ShareCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.selection.Selection;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.selection.StorageStrategy;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import threads.ipfs.CID;
import threads.server.R;
import threads.server.core.events.EVENTS;
import threads.server.core.threads.THREADS;
import threads.server.core.threads.Thread;
import threads.server.jobs.JobServiceDeleteThreads;
import threads.server.mdl.SelectionViewModel;
import threads.server.mdl.ThreadViewModel;
import threads.server.provider.FileDocumentsProvider;
import threads.server.services.Service;
import threads.server.utils.Network;
import threads.server.utils.ThreadItemDetailsLookup;
import threads.server.utils.ThreadsItemKeyProvider;
import threads.server.utils.ThreadsViewAdapter;
import threads.server.work.LoadNotificationsWorker;

import static androidx.core.util.Preconditions.checkNotNull;

public class ThreadsFragment extends Fragment implements
        SwipeRefreshLayout.OnRefreshListener, ThreadsViewAdapter.ThreadsViewAdapterListener {

    private static final String TAG = ThreadsFragment.class.getSimpleName();
    private static final int CLICK_OFFSET = 500;
    @NonNull
    private final AtomicReference<LiveData<List<Thread>>> observer = new AtomicReference<>(null);

    @NonNull
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private SelectionViewModel mSelectionViewModel;
    private ThreadsViewAdapter mThreadsViewAdapter;
    private ThreadViewModel threadViewModel;
    private long mLastClickTime = 0;
    private Context mContext;
    private FragmentActivity mActivity;
    private ThreadsFragment.ActionListener mListener;
    private RecyclerView mRecyclerView;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private ActionMode mActionMode;
    private SelectionTracker<Long> mSelectionTracker;
    private SearchView mSearchView;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
        mActivity = getActivity();
        mListener = (ThreadsFragment.ActionListener) mActivity;
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
        this.setHasOptionsMenu(true);

        mSelectionViewModel = new ViewModelProvider(mActivity).get(SelectionViewModel.class);


        mSelectionViewModel.getParentThread().observe(this, (threadIdx) -> {

            if (threadIdx != null) {
                updateDirectory(threadIdx, mSelectionViewModel.getQuery().getValue());
            }

        });

        mSelectionViewModel.getQuery().observe(this, (query) -> {

            if (query != null) {
                Long parent = mSelectionViewModel.getParentThread().getValue();
                checkNotNull(parent);
                updateDirectory(parent, query);
            }

        });

    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);
        menuInflater.inflate(R.menu.menu_threads_fragment, menu);


        SearchManager searchManager = (SearchManager)
                mActivity.getSystemService(Context.SEARCH_SERVICE);
        checkNotNull(searchManager);

        MenuItem searchMenuItem = menu.findItem(R.id.action_search);
        searchMenuItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                mListener.showBottomNavigation(false);
                mListener.setPagingEnabled(false);
                mListener.showMainFab(false);
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                String query = "";
                mSearchView.setQuery(query, false);
                mSelectionViewModel.setQuery(query);
                removeKeyboards();
                mListener.showBottomNavigation(true);
                mListener.setPagingEnabled(true);
                mListener.showMainFab(true);

                return true;
            }
        });
        mSearchView = (SearchView) searchMenuItem.getActionView();
        mSearchView.setSearchableInfo(searchManager.getSearchableInfo(mActivity.getComponentName()));
        mSearchView.setIconifiedByDefault(false);
        String query = mSelectionViewModel.getQuery().getValue();
        checkNotNull(query);
        mSearchView.setQuery(query, false);
        if (!query.isEmpty()) {
            searchMenuItem.expandActionView();
        }


        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {

                mSelectionViewModel.getQuery().setValue(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {

                mSelectionViewModel.getQuery().setValue(newText);
                return false;
            }
        });
    }

    private void removeKeyboards() {
        try {
            InputMethodManager imm = (InputMethodManager)
                    mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null && mSearchView != null) {
                imm.hideSoftInputFromWindow(mSearchView.getWindowToken(), 0);
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (item.getItemId() == R.id.action_search) {

            if (SystemClock.elapsedRealtime() - mLastClickTime < CLICK_OFFSET) {
                return true;
            }

            mLastClickTime = SystemClock.elapsedRealtime();

            mSearchView.setQuery("", false);
            mActivity.onSearchRequested();

            return true;

        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.threads_view, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        threadViewModel = new ViewModelProvider(this).get(ThreadViewModel.class);

        mRecyclerView = view.findViewById(R.id.recycler_view_message_list);

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
                "threads-selection",//unique id
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

        mThreadsViewAdapter.setSelectionTracker(mSelectionTracker);


        if (savedInstanceState != null) {
            mSelectionTracker.onRestoreInstanceState(savedInstanceState);
        }

    }

    private void updateDirectory(Long parent, String query) {
        try {

            LiveData<List<Thread>> obs = observer.get();
            if (obs != null) {
                obs.removeObservers(this);
            }

            LiveData<List<Thread>> liveData = threadViewModel.getVisibleChildrenByQuery(parent, query);
            observer.set(liveData);

            liveData.observe(this, (threads) -> {

                if (threads != null) {

                    threads.sort(Comparator.comparing(Thread::getLastModified).reversed());

                    boolean scrollToTop = false;

                    if (!threads.isEmpty()) {
                        int pos = mThreadsViewAdapter.getPosition(
                                threads.get(0).getIdx());
                        scrollToTop = pos != 0;
                    }

                    mThreadsViewAdapter.updateData(threads);

                    if (scrollToTop) {
                        try {
                            mRecyclerView.scrollToPosition(0);
                        } catch (Throwable e) {
                            Log.e(TAG, "" + e.getLocalizedMessage(), e);
                        }
                    }
                }
            });
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }


    private void sendAction() {
        final EVENTS events = EVENTS.getInstance(mContext);

        Selection<Long> selection = mSelectionTracker.getSelection();
        if (selection.size() == 0) {
            java.lang.Thread thread = new java.lang.Thread(() -> events.invokeEvent(
                    EVENTS.WARNING,
                    getString(R.string.no_marked_file_send)));
            thread.start();
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
        final THREADS threadsAPI = THREADS.getInstance(mContext);
        final EVENTS events = EVENTS.getInstance(mContext);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                Long idx = mSelectionViewModel.getParentThread().getValue();
                checkNotNull(idx);
                threadsAPI.resetParentThreadsNumber(idx);
            } catch (Throwable e) {
                events.exception(e);
            }
        });
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

        final EVENTS events = EVENTS.getInstance(mContext);
        if (!mSelectionViewModel.isTopLevel()) {
            java.lang.Thread thread = new java.lang.Thread(() -> events.invokeEvent(
                    EVENTS.WARNING,
                    getString(R.string.deleting_files_within_directory_not_supported)));
            thread.start();
            return;
        }

        if (!mSelectionTracker.hasSelection()) {
            java.lang.Thread thread = new java.lang.Thread(() -> events.invokeEvent(
                    EVENTS.WARNING,
                    getString(R.string.no_marked_file_delete)));
            thread.start();
            return;
        }


        try {
            long[] entries = convert(mSelectionTracker.getSelection());

            JobServiceDeleteThreads.removeThreads(mContext, entries);

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
        return thread.isSeeding();
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
            boolean copyActive = !thread.isDir();

            ThreadActionDialogFragment.newInstance(
                    thread.getIdx(), true, true,
                    mSelectionViewModel.isTopLevel(), true, sendActive,
                    copyActive, true, thread.isPinned())
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

                MenuItem action_delete = menu.findItem(R.id.action_mode_delete);
                active = mSelectionViewModel.isTopLevel();
                action_delete.setVisible(active);

                mListener.showBottomNavigation(false);
                mListener.setPagingEnabled(false);
                mListener.showMainFab(false);
                removeKeyboards();
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
                    case R.id.action_mode_mark_all: {

                        if (SystemClock.elapsedRealtime() - mLastClickTime < CLICK_OFFSET) {
                            break;
                        }
                        mLastClickTime = SystemClock.elapsedRealtime();

                        mThreadsViewAdapter.selectAllThreads();

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
                clearUnreadNotes(); // todo check if this is nice solution

                boolean stillSearchView = true;
                if (mSearchView != null) {
                    stillSearchView = !mSearchView.isShown();
                }
                mListener.showBottomNavigation(stillSearchView);
                mListener.setPagingEnabled(stillSearchView);
                mListener.showMainFab(stillSearchView);
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


                final THREADS threads = THREADS.getInstance(mContext);

                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.submit(() -> {
                    try {
                        int children = threads.getThreadReferences(threadIdx);
                        if (children > 0) {
                            mSelectionViewModel.setParentThread(threadIdx);
                        } else {
                            mHandler.post(() -> clickThreadPlay(threadIdx));
                        }
                    } catch (Throwable e) {
                        Log.e(TAG, "" + e.getLocalizedMessage(), e);
                    }
                });
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }

    }


    private void clickThreadPlay(long idx) {
        final EVENTS events = EVENTS.getInstance(mContext);
        final THREADS threads = THREADS.getInstance(mContext);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                Thread thread = threads.getThreadByIdx(idx);
                checkNotNull(thread);

                if (thread.isSeeding()) {

                    CID cid = thread.getContent();
                    checkNotNull(cid);


                    String mimeType = thread.getMimeType();


                    Uri uri = FileDocumentsProvider.getUriForThread(thread);
                    Intent intent = ShareCompat.IntentBuilder.from(mActivity)
                            .setStream(uri)
                            .setType(mimeType)
                            .getIntent();
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.putExtra(Intent.EXTRA_TITLE, thread.getName());
                    intent.setData(uri);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);


                    if (intent.resolveActivity(
                            mContext.getPackageManager()) != null) {
                        startActivity(intent);
                    } else {
                        events.error(getString(R.string.no_activity_found_to_handle_uri));
                    }
                }
            } catch (Throwable ex) {
                events.error(getString(R.string.no_activity_found_to_handle_uri));
            }
        });

    }


    @Override
    public void invokeDownload(@NonNull Thread thread) {
        checkNotNull(thread);
        try {

            if (!Network.isConnected(mContext)) {

                java.lang.Thread threadError = new java.lang.Thread(()
                        -> EVENTS.getInstance(mContext).error(getString(R.string.offline_mode)));
                threadError.start();

                return;
            }


            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    Service.getInstance(mContext).retryDownloadThread(mContext, thread);
                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
                }
            });

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }


    @Override
    public void onRefresh() {
        mSwipeRefreshLayout.setRefreshing(true);

        try {
            LoadNotificationsWorker.notifications(mContext);
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        } finally {
            mSwipeRefreshLayout.setRefreshing(false);
        }

    }


    public interface ActionListener {

        void clickThreadsSend(long[] indices);

        void showBottomNavigation(boolean visible);

        void showMainFab(boolean visible);

        void setPagingEnabled(boolean enabled);
    }
}
