package threads.server.fragments;

import android.Manifest;
import android.app.Activity;
import android.app.SearchManager;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.DocumentsContract;
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
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuPopupHelper;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.app.ShareCompat;
import androidx.core.content.ContextCompat;
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

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.io.OutputStream;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import threads.ipfs.CID;
import threads.ipfs.IPFS;
import threads.server.InitApplication;
import threads.server.MainActivity;
import threads.server.R;
import threads.server.core.events.EVENTS;
import threads.server.core.peers.PEERS;
import threads.server.core.peers.User;
import threads.server.core.threads.THREADS;
import threads.server.core.threads.Thread;
import threads.server.jobs.JobServiceDeleteThreads;
import threads.server.jobs.JobServicePublish;
import threads.server.model.SelectionViewModel;
import threads.server.model.ThreadViewModel;
import threads.server.provider.FileDocumentsProvider;
import threads.server.services.BootstrapService;
import threads.server.services.DownloaderService;
import threads.server.services.LiteService;
import threads.server.services.UploadService;
import threads.server.services.WorkerService;
import threads.server.utils.CodecDecider;
import threads.server.utils.Network;
import threads.server.utils.ThreadItemDetailsLookup;
import threads.server.utils.ThreadsItemKeyProvider;
import threads.server.utils.ThreadsViewAdapter;
import threads.server.work.ConnectPeersWorker;
import threads.server.work.DownloadThreadWorker;
import threads.server.work.LoadNotificationsWorker;

import static androidx.core.util.Preconditions.checkNotNull;

public class ThreadsFragment extends Fragment implements
        SwipeRefreshLayout.OnRefreshListener, ThreadsViewAdapter.ThreadsViewAdapterListener {

    private static final String TAG = ThreadsFragment.class.getSimpleName();
    private static final int REQUEST_CAMERA = 1;
    private static final int FILE_EXPORT_REQUEST = 2;
    private static final int REQUEST_SELECT_FILES = 3;

    private static final int CLICK_OFFSET = 500;
    @NonNull
    private final AtomicReference<LiveData<List<Thread>>> observer = new AtomicReference<>(null);

    @NonNull
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private SelectionViewModel mSelectionViewModel;
    private ThreadsViewAdapter mThreadsViewAdapter;
    private ThreadViewModel mThreadViewModel;
    private long mLastClickTime = 0;
    private Context mContext;
    private FragmentActivity mActivity;
    private ThreadsFragment.ActionListener mListener;
    private RecyclerView mRecyclerView;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private ActionMode mActionMode;
    private SelectionTracker<Long> mSelectionTracker;
    private SearchView mSearchView;
    private boolean isTablet;
    private boolean hasCamera;
    private CID threadContent = null;
    private FloatingActionButton mMainFab;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
        mActivity = getActivity();
        mListener = (ThreadsFragment.ActionListener) mActivity;
        isTablet = getResources().getBoolean(R.bool.isTablet);
        PackageManager pm = mContext.getPackageManager();
        hasCamera = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mContext = null;
        mActivity = null;
        mListener = null;
    }

    @Override
    public void onRequestPermissionsResult
            (int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA) {
            for (int i = 0, len = permissions.length; i < len; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    EVENTS.getInstance(mContext).postPermission(
                            getString(R.string.permission_camera_denied));

                }
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    clickMultihashWork();
                }
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        try {
            if (requestCode == REQUEST_SELECT_FILES) {

                if (data.getClipData() != null) {
                    ClipData mClipData = data.getClipData();
                    for (int i = 0; i < mClipData.getItemCount(); i++) {
                        ClipData.Item item = mClipData.getItemAt(i);
                        Uri uri = item.getUri();
                        UploadService.invoke(mContext, uri);
                    }
                } else if (data.getData() != null) {
                    Uri uri = data.getData();
                    UploadService.invoke(mContext, uri);
                }
            } else if (requestCode == FILE_EXPORT_REQUEST) {
                if (data != null) {
                    Uri uri = data.getData();
                    if (uri != null) {
                        IPFS ipfs = IPFS.getInstance(mContext);

                        OutputStream os = mActivity.getContentResolver().openOutputStream(uri);
                        if (os != null) {
                            try {
                                ipfs.storeToOutputStream(os, threadContent);
                            } catch (Throwable e) {
                                Log.e(TAG, "" + e.getLocalizedMessage(), e);
                            } finally {
                                os.close();
                            }
                        }
                    }
                }
            } else {
                IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
                if (result != null) {
                    if (result.getContents() != null) {
                        String multihash = result.getContents();
                        downloadMultihash(multihash);
                    } else {
                        EVENTS.getInstance(mContext).postError(getString(R.string.scan_failed));
                    }
                }
            }

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
        super.onActivityResult(requestCode, resultCode, data);
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

        MenuItem actionUpload = menu.findItem(R.id.action_upload);
        actionUpload.setVisible(isTablet);

        MenuItem actionEditCode = menu.findItem(R.id.action_edit_cid);
        actionEditCode.setVisible(true);

        MenuItem actionScanCid = menu.findItem(R.id.action_scan_cid);
        actionScanCid.setVisible(hasCamera);

        SearchManager searchManager = (SearchManager)
                mActivity.getSystemService(Context.SEARCH_SERVICE);
        checkNotNull(searchManager);

        MenuItem searchMenuItem = menu.findItem(R.id.action_search);


        mSearchView = (SearchView) searchMenuItem.getActionView();
        mSearchView.setSearchableInfo(searchManager.getSearchableInfo(mActivity.getComponentName()));
        mSearchView.setOnCloseListener(() -> {
            removeKeyboards();
            mListener.showBottomNavigation(true);
            mListener.setPagingEnabled(true);
            showMainFab(true);

            return false;

        });
        mSearchView.setOnSearchClickListener((v) -> {
            mListener.showBottomNavigation(false);
            mListener.setPagingEnabled(false);
            showMainFab(false);
        });
        mSearchView.setIconifiedByDefault(true);
        String query = mSelectionViewModel.getQuery().getValue();
        checkNotNull(query);
        mSearchView.setQuery(query, true);
        mSearchView.setIconified(query.isEmpty());


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

        } else if (item.getItemId() == R.id.action_upload) {

            if (SystemClock.elapsedRealtime() - mLastClickTime < CLICK_OFFSET) {
                return true;
            }

            mLastClickTime = SystemClock.elapsedRealtime();

            clickUpload();
            return true;

        } else if (item.getItemId() == R.id.action_select_all) {

            if (SystemClock.elapsedRealtime() - mLastClickTime < CLICK_OFFSET) {
                return true;
            }

            mLastClickTime = SystemClock.elapsedRealtime();

            mThreadsViewAdapter.selectAllThreads();

            return true;

        } else if (item.getItemId() == R.id.action_scan_cid) {

            if (SystemClock.elapsedRealtime() - mLastClickTime < CLICK_OFFSET) {
                return true;
            }

            mLastClickTime = SystemClock.elapsedRealtime();

            clickMultihash();
            return true;

        } else if (item.getItemId() == R.id.action_edit_cid) {

            if (SystemClock.elapsedRealtime() - mLastClickTime < CLICK_OFFSET) {
                return true;
            }

            mLastClickTime = SystemClock.elapsedRealtime();

            clickEditMultihash();
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


        mMainFab = view.findViewById(R.id.fab_main);
        if (isTablet) {
            mMainFab.setVisibility(View.GONE);
        }

        mMainFab.setOnClickListener((v) -> {

            if (SystemClock.elapsedRealtime() - mLastClickTime < 500) {
                return;
            }
            mLastClickTime = SystemClock.elapsedRealtime();

            threadsFabAction();

        });

        mThreadViewModel = new ViewModelProvider(this).get(ThreadViewModel.class);

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


        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                boolean hasSelection = mSelectionTracker.hasSelection();
                if (dy > 0 && !hasSelection) {
                    showMainFab(false);
                } else if (dy < 0 && !hasSelection) {
                    showMainFab(true);
                }

            }
        });

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

            LiveData<List<Thread>> liveData = mThreadViewModel.getVisibleChildrenByQuery(parent, query);
            observer.set(liveData);

            liveData.observe(this, (threads) -> {

                if (threads != null) {

                    threads.sort(Comparator.comparing(Thread::getLastModified).reversed());


                    int size = mThreadsViewAdapter.getItemCount();
                    boolean scrollToTop = size < threads.size();


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


    private void clickThreadsSend(final long[] indices) {


        // CHECKED
        if (!Network.isConnected(mContext)) {
            EVENTS.getInstance(mContext).postWarning(
                    getString(R.string.offline_mode));
        }


        final PEERS peers = PEERS.getInstance(mContext);
        final EVENTS events = EVENTS.getInstance(mContext);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                List<User> users = peers.getNonBlockedLiteUsers();

                if (users.isEmpty()) {
                    events.error(getString(R.string.no_sharing_peers));
                } else if (users.size() == 1) {
                    LiteService.getInstance(mContext).sendThreads(mContext, users, indices);
                } else {

                    SendDialogFragment dialogFragment = new SendDialogFragment();
                    Bundle bundle = new Bundle();
                    bundle.putLongArray(SendDialogFragment.IDXS, indices);
                    dialogFragment.setArguments(bundle);
                    dialogFragment.show(getChildFragmentManager(), SendDialogFragment.TAG);
                }

            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }
        });


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

            clickThreadsSend(entries);

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
    public void invokeAction(@NonNull Thread thread, @NonNull View view) {
        checkNotNull(thread);
        checkNotNull(view);


        try {
            boolean sendActive = LiteService.isSendNotificationsEnabled(mContext);
            boolean copyActive = !thread.isDir();
            boolean isPinned = thread.isPinned();
            boolean deleteActive = mSelectionViewModel.isTopLevel();

            PopupMenu menu = new PopupMenu(mContext, view);
            menu.inflate(R.menu.popup_threads_menu);
            menu.getMenu().findItem(R.id.popup_pin).setVisible(!isPinned);
            menu.getMenu().findItem(R.id.popup_unpin).setVisible(isPinned);
            menu.getMenu().findItem(R.id.popup_share).setVisible(sendActive);
            menu.getMenu().findItem(R.id.popup_delete).setVisible(deleteActive);
            menu.getMenu().findItem(R.id.popup_copy_to).setVisible(copyActive);
            menu.setOnMenuItemClickListener((item) -> {

                if (SystemClock.elapsedRealtime() - mLastClickTime < CLICK_OFFSET) {
                    return true;
                }

                mLastClickTime = SystemClock.elapsedRealtime();
                if (item.getItemId() == R.id.popup_info) {
                    clickThreadInfo(thread.getIdx());
                    return true;
                } else if (item.getItemId() == R.id.popup_view) {
                    clickThreadView(thread.getIdx());
                    return true;
                } else if (item.getItemId() == R.id.popup_delete) {
                    clickThreadDelete(thread.getIdx());
                    return true;
                } else if (item.getItemId() == R.id.popup_share) {
                    clickThreadShare(thread.getIdx());
                    return true;
                } else if (item.getItemId() == R.id.popup_send) {
                    long[] indices = {thread.getIdx()};
                    clickThreadsSend(indices);
                    return true;
                } else if (item.getItemId() == R.id.popup_copy_to) {
                    clickThreadCopy(thread.getIdx());
                    return true;
                } else if (item.getItemId() == R.id.popup_unpin) {
                    clickThreadPublish(thread.getIdx(), false);
                    return true;
                } else if (item.getItemId() == R.id.popup_pin) {
                    clickThreadPublish(thread.getIdx(), true);
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

    private void clickThreadShare(long idx) {
        final EVENTS events = EVENTS.getInstance(mContext);
        final THREADS threads = THREADS.getInstance(mContext);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                Thread thread = threads.getThreadByIdx(idx);
                checkNotNull(thread);
                ComponentName[] names = {new ComponentName(
                        mActivity.getApplicationContext(), MainActivity.class)};
                CID cid = thread.getContent();
                checkNotNull(cid);
                Uri uri = FileDocumentsProvider.getUriForBitmap(cid.getCid());
                Intent intent = ShareCompat.IntentBuilder.from(mActivity)
                        .setStream(uri)
                        .setType(thread.getMimeType())
                        .getIntent();
                intent.setAction(Intent.ACTION_SEND);
                intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.content_id));
                intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.content_file_access,
                        cid.getCid(), thread.getName()));
                intent.setData(uri);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.putExtra(Intent.EXTRA_TITLE, thread.getName());


                if (intent.resolveActivity(mActivity.getPackageManager()) != null) {
                    Intent chooser = Intent.createChooser(intent, getText(R.string.share));
                    chooser.putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, names);
                    startActivity(chooser);
                } else {
                    events.error(getString(R.string.no_activity_found_to_handle_uri));
                }

            } catch (Throwable e) {
                events.exception(e);
            }
        });


    }

    private void clickThreadCopy(long idx) {

        try {
            final THREADS threadsAPI = THREADS.getInstance(mContext);
            final EVENTS events = EVENTS.getInstance(mContext);


            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {

                Thread thread = threadsAPI.getThreadByIdx(idx);
                checkNotNull(thread);

                threadContent = thread.getContent();

                Intent intent = ShareCompat.IntentBuilder.from(mActivity).getIntent();
                intent.setType(thread.getMimeType());
                intent.setAction(Intent.ACTION_CREATE_DOCUMENT);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.putExtra(Intent.EXTRA_TITLE, thread.getName());
                intent.putExtra(DocumentsContract.EXTRA_EXCLUDE_SELF, true);
                intent.addCategory(Intent.CATEGORY_OPENABLE);


                if (intent.resolveActivity(mActivity.getPackageManager()) != null) {
                    startActivityForResult(intent, FILE_EXPORT_REQUEST);
                } else {
                    events.error(getString(R.string.no_activity_found_to_handle_uri));
                }
            });
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }


    }

    private void clickThreadPublish(long idx, boolean pinned) {

        if (pinned) {
            if (!InitApplication.getDontShowAgain(mContext, LiteService.PIN_SERVICE_KEY)) {
                try {
                    DontShowAgainFragmentDialog.newInstance(
                            getString(R.string.pin_service_notice), LiteService.PIN_SERVICE_KEY).show(
                            getChildFragmentManager(), DontShowAgainFragmentDialog.TAG);

                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
                }
            }
        }
        final THREADS threads = THREADS.getInstance(mContext);
        final EVENTS events = EVENTS.getInstance(mContext);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                threads.setThreadPinned(idx, pinned);
            } catch (Throwable e) {
                events.exception(e);
            }
        });

    }

    private ActionMode.Callback createActionModeCallback() {
        return new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                mode.getMenuInflater().inflate(R.menu.menu_threads_action_mode, menu);
                MenuItem action_send = menu.findItem(R.id.action_mode_send);
                boolean active = LiteService.isSendNotificationsEnabled(mContext);
                action_send.setVisible(active);

                MenuItem action_delete = menu.findItem(R.id.action_mode_delete);
                active = mSelectionViewModel.isTopLevel();
                action_delete.setVisible(active);

                mListener.showBottomNavigation(false);
                mListener.setPagingEnabled(false);
                showMainFab(false);
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

                boolean stillSearchView = true;
                if (mSearchView != null) {
                    stillSearchView = mSearchView.isIconified();
                }
                mListener.showBottomNavigation(stillSearchView);
                mListener.setPagingEnabled(stillSearchView);
                showMainFab(stillSearchView);
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


    private void threadsFabAction() {

        if (mSelectionViewModel.isTopLevel()) {

            clickUpload();

        } else {
            Long idx = mSelectionViewModel.getParentThread().getValue();
            checkNotNull(idx);
            final THREADS threads = THREADS.getInstance(mContext);
            final EVENTS events = EVENTS.getInstance(mContext);
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {

                    Thread thread = threads.getThreadByIdx(idx);
                    if (thread != null) {
                        long parent = thread.getParent();
                        mSelectionViewModel.setParentThread(parent);
                    }

                } catch (Throwable e) {
                    events.exception(e);
                }
            });

        }
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
                EVENTS.getInstance(mContext).postWarning(getString(R.string.offline_mode));
            }

            BootstrapService.bootstrap(mContext);
            ConnectPeersWorker.connect(mContext, 5);

            WorkerService.markThreadDownload(mContext, thread.getIdx());
            DownloadThreadWorker.download(mContext, thread.getIdx());
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    @Override
    public void invokePauseAction(@NonNull Thread thread) {
        checkNotNull(thread);

        WorkerService.cancelThreadDownload(mContext, thread.getIdx());
    }


    @Override
    public void onRefresh() {
        mSwipeRefreshLayout.setRefreshing(true);

        try {
            LoadNotificationsWorker.notifications(mContext, 0);
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        } finally {
            mSwipeRefreshLayout.setRefreshing(false);
        }

    }

    private void clickThreadView(long idx) {

        final THREADS threads = THREADS.getInstance(mContext);
        final EVENTS events = EVENTS.getInstance(mContext);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {

                CID cid = threads.getThreadContent(idx);
                checkNotNull(cid);


                BootstrapService.bootstrap(mContext);
                JobServicePublish.publish(mContext, cid);

                String gateway = LiteService.getGateway(mContext);
                Uri uri = Uri.parse(gateway + "/ipfs/" + cid.getCid());

                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);

            } catch (Throwable e) {
                events.exception(e);
            }
        });

    }


    private void showMainFab(boolean visible) {
        if (!isTablet) {
            if (visible) {
                mMainFab.show();
            } else {
                mMainFab.hide();
            }
        }
    }

    private void clickThreadDelete(long idx) {
        JobServiceDeleteThreads.removeThreads(mContext, idx);
    }


    private void clickMultihash() {

        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);
            return;
        }
        clickMultihashWork();
    }


    private void downloadMultihash(@NonNull String codec) {
        checkNotNull(codec);


        try {
            CodecDecider codecDecider = CodecDecider.evaluate(codec);
            if (codecDecider.getCodex() == CodecDecider.Codec.MULTIHASH ||
                    codecDecider.getCodex() == CodecDecider.Codec.URI) {

                String multihash = codecDecider.getMultihash();
                BootstrapService.bootstrap(mContext);
                DownloaderService.download(mContext, CID.create(multihash));
            } else {
                EVENTS.getInstance(mContext).postError(getString(R.string.codec_not_supported));
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    private void clickMultihashWork() {
        PackageManager pm = mContext.getPackageManager();
        try {
            if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
                IntentIntegrator integrator = IntentIntegrator.forSupportFragment(this);
                integrator.setOrientationLocked(false);
                integrator.initiateScan();
            } else {
                EVENTS.getInstance(mContext).postError(getString(R.string.feature_camera_required));
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    private void clickEditMultihash() {
        try {
            EditMultihashDialogFragment editMultihashDialogFragment =
                    new EditMultihashDialogFragment();
            editMultihashDialogFragment.show(getChildFragmentManager(),
                    EditMultihashDialogFragment.TAG);
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }


    private void clickUpload() {

        try {
            Intent intent = ShareCompat.IntentBuilder.from(mActivity).getIntent();
            intent.setType("*/*");

            String[] mimeTypes = {"*/*"};
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            intent.putExtra(DocumentsContract.EXTRA_EXCLUDE_SELF, true);
            intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);


            if (intent.resolveActivity(mContext.getPackageManager()) != null) {
                startActivityForResult(Intent.createChooser(intent,
                        getString(R.string.select_files)), REQUEST_SELECT_FILES);
            } else {
                EVENTS.getInstance(mContext).postError(
                        getString(R.string.no_activity_found_to_handle_uri));
            }

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }


    public interface ActionListener {

        void showBottomNavigation(boolean visible);

        void setPagingEnabled(boolean enabled);

    }
}
