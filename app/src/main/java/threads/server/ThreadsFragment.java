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
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
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
import threads.core.api.Thread;
import threads.core.api.ThreadStatus;
import threads.core.mdl.ThreadViewModel;
import threads.ipfs.IPFS;
import threads.ipfs.api.CID;
import threads.share.AudioDialogFragment;
import threads.share.ImageDialogFragment;
import threads.share.PDFView;
import threads.share.ThreadActionDialogFragment;
import threads.share.ThreadsViewAdapter;
import threads.share.VideoDialogFragment;
import threads.share.WebViewDialogFragment;

import static androidx.core.util.Preconditions.checkNotNull;

public class ThreadsFragment extends Fragment implements ThreadsViewAdapter.ThreadsViewAdapterListener {

    private static final String TAG = ThreadsFragment.class.getSimpleName();
    private static final String DIRECTORY = "DIRECTORY";
    private static final String IDXS = "IDXS";
    private static final String SELECTION = "SELECTION";
    private static final int CLICK_OFFSET = 500;


    @NonNull
    private final List<Long> threads = new ArrayList<>();
    @NonNull
    private final AtomicReference<LiveData<List<Thread>>> observer = new AtomicReference<>(null);
    @NonNull
    private final AtomicReference<Long> directory = new AtomicReference<>();
    @NonNull
    private final AtomicBoolean topLevel = new AtomicBoolean(true);

    private long threadIdx;

    private View view;
    private ThreadsViewAdapter threadsViewAdapter;
    private ThreadViewModel threadViewModel;
    private long mLastClickTime = 0;
    private Context mContext;
    private ThreadsFragment.ActionListener mListener;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private RecyclerView mRecyclerView;
    private static String getCompactString(@NonNull String title) {
        checkNotNull(title);
        return title.replace("\n", " ");
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
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_threads, menu);
        MenuItem action_delete = menu.findItem(R.id.action_delete);
        action_delete.setVisible(!threads.isEmpty());
        MenuItem action_send = menu.findItem(R.id.action_send);
        boolean active = Service.isSendNotificationsEnabled(mContext);
        action_send.setVisible(!threads.isEmpty() && active);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_mark_all: {

                if (SystemClock.elapsedRealtime() - mLastClickTime < CLICK_OFFSET) {
                    break;
                }
                mLastClickTime = SystemClock.elapsedRealtime();

                markThreads();

                return true;
            }
            case R.id.action_delete: {

                if (SystemClock.elapsedRealtime() - mLastClickTime < CLICK_OFFSET) {
                    break;
                }
                mLastClickTime = SystemClock.elapsedRealtime();

                deleteAction();

                return true;
            }

            case R.id.action_send: {

                if (SystemClock.elapsedRealtime() - mLastClickTime < CLICK_OFFSET) {
                    break;
                }
                mLastClickTime = SystemClock.elapsedRealtime();

                sendAction();

                return true;
            }
            case R.id.action_unmark_all: {

                if (SystemClock.elapsedRealtime() - mLastClickTime < CLICK_OFFSET) {
                    break;
                }
                mLastClickTime = SystemClock.elapsedRealtime();

                clearUnreadNotes();
                unmarkThreads();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.threads_view, container, false);
        return view;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        try {
            long[] entries = convert(threads);
            outState.putLongArray(IDXS, entries);
            outState.putLong(SELECTION, threadIdx);
            Long dir = directory.get();
            if (dir != null) {
                outState.putLong(DIRECTORY, dir);
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            long[] storedThreads = savedInstanceState.getLongArray(IDXS);
            if (storedThreads != null) {
                for (long idx : storedThreads) {
                    threads.add(idx);
                }
                if (threadsViewAdapter != null) {
                    for (long idx : storedThreads) {
                        threadsViewAdapter.setState(idx, ThreadsViewAdapter.State.MARKED);
                    }
                }
            }
            long selection = savedInstanceState.getLong(SELECTION);
            if (selection > 0) {
                threadIdx = selection;
                if (threadsViewAdapter != null) {
                    threadsViewAdapter.setState(threadIdx, ThreadsViewAdapter.State.SELECTED);
                }
            }

            Long value = savedInstanceState.getLong(DIRECTORY);
            directory.set(value);
        }

        try {
            Long current = directory.get();
            if (current == null) {
                directory.set(0L);
            }
            update(directory.get());
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }

    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        threadViewModel = ViewModelProviders.of(this).get(ThreadViewModel.class);

        mRecyclerView = view.findViewById(R.id.recycler_view_message_list);
        mRecyclerView.setItemAnimator(null); // no animation of the item when something changed


        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(mContext);
        mRecyclerView.setLayoutManager(linearLayoutManager);

        threadsViewAdapter = new ThreadsViewAdapter(mContext, this);
        mRecyclerView.setAdapter(threadsViewAdapter);


        FloatingActionButton fab_action = view.findViewById(R.id.fab_action);

        fab_action.setOnClickListener((v) -> {

            if (topLevel.get()) {

                if (SystemClock.elapsedRealtime() - mLastClickTime < CLICK_OFFSET) {
                    return;
                }
                mLastClickTime = SystemClock.elapsedRealtime();


                    ThreadsDialogFragment.newInstance(true, true, true)
                            .show(getChildFragmentManager(), ThreadsDialogFragment.TAG);


            } else {

                if (SystemClock.elapsedRealtime() - mLastClickTime < CLICK_OFFSET) {
                    return;
                }
                mLastClickTime = SystemClock.elapsedRealtime();


                back();

            }

        });


    }

    private void update(long thread) {
        try {
            topLevel.set(thread == 0L);

            directory.set(thread);

            LiveData<List<Thread>> obs = observer.get();
            if (obs != null) {
                obs.removeObservers(this);
            }

            LiveData<List<Thread>> liveData = threadViewModel.getThreadsByThread(thread);
            observer.set(liveData);

            liveData.observe(this, (threads) -> {

                if (threads != null) {

                    List<Thread> data = new ArrayList<>();
                    for (Thread threadObject : threads) {
                        if (threadObject.getStatus() != ThreadStatus.DELETING) {
                            data.add(threadObject);
                        }
                    }
                    data.sort(Comparator.comparing(Thread::getDate).reversed());

                    boolean scrollToTop = false;

                    if (!data.isEmpty()) {
                        int pos = threadsViewAdapter.getPositionOfItem(data.get(0).getIdx());
                        scrollToTop = pos != 0;
                    }

                    threadsViewAdapter.updateData(data);

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
        } finally {
            evaluateFabDeleteVisibility();
        }

    }


    private void evaluateFabDeleteVisibility() {
        try {
            FloatingActionButton fab_action = view.findViewById(R.id.fab_action);
            if (topLevel.get()) {
                fab_action.setImageResource(R.drawable.dots);
            } else {
                fab_action.setImageResource(R.drawable.arrow_left);
            }

            mListener.invalidateOptionsMenu();

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    private void sendAction() {

        if (threads.isEmpty()) {
            THREADS threads = Singleton.getInstance(mContext).getThreads();
            Preferences.warning(threads,
                    mContext.getString(R.string.no_marked_file_send));
            return;
        }

        try {

            long[] entries = convert(threads);

            mListener.clickThreadsSend(entries);

            unmarkThreads();
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    private void markThreads() {
        final THREADS threadsAPI = Singleton.getInstance(mContext).getThreads();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                List<Thread> threadObjects = threadsAPI.getThreadsByThread(directory.get());
                for (Thread thread : threadObjects) {
                    if (!threads.contains(thread.getIdx())) {
                        threads.add(thread.getIdx());
                        threadIdx = thread.getIdx();
                    }
                }

                for (long idx : threads) {
                    threadsViewAdapter.setState(idx, ThreadsViewAdapter.State.MARKED);
                }

                mHandler.post(() -> threadsViewAdapter.notifyDataSetChanged());

            } catch (Throwable e) {
                Preferences.evaluateException(threadsAPI, Preferences.EXCEPTION, e);
            } finally {
                mHandler.post(this::evaluateFabDeleteVisibility);

            }
        });
    }

    private void clearUnreadNotes() {
        final THREADS threadsAPI = Singleton.getInstance(mContext).getThreads();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                threadsAPI.resetThreadUnreadNotesNumber(directory.get());
            } catch (Throwable e) {
                Preferences.evaluateException(threadsAPI, Preferences.EXCEPTION, e);
            }
        });
    }

    private void unmarkThreads() {
        try {
            for (long idx : threads) {
                threadsViewAdapter.setState(idx, ThreadsViewAdapter.State.NONE);
            }
            threads.clear();

            threadIdx = -1;
            threadsViewAdapter.notifyDataSetChanged();
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        } finally {
            evaluateFabDeleteVisibility();
        }
    }

    private long[] convert(List<Long> entries) {
        long[] basic = new long[entries.size()];
        for (int i = 0; i < entries.size(); i++) {
            basic[i] = entries.get(i);
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

        if (threads.isEmpty()) {
            THREADS threads = Singleton.getInstance(mContext).getThreads();
            Preferences.warning(threads,
                    mContext.getString(R.string.no_marked_file_delete));
            return;
        }

        long[] entries = convert(threads);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                Service.removeThreads(mContext, entries);
            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }
        });

        try {
            if (threads.contains(threadIdx)) {
                threadIdx = -1;
            }
            threads.clear();

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        } finally {
            evaluateFabDeleteVisibility();
        }

    }

    private void back() {

        unmarkThreads();

        final THREADS threads = Singleton.getInstance(mContext).getThreads();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                Long idx = directory.get();

                Thread thread = threads.getThreadByIdx(idx);
                if (thread != null) {
                    long parent = thread.getThread();
                    mHandler.post(() -> update(parent));
                }

            } catch (Throwable e) {
                Preferences.evaluateException(threads, Preferences.EXCEPTION, e);
            }
        });
    }

    @Override
    public boolean generalActionSupport(@NonNull Thread thread) {
        checkNotNull(thread);
        return thread.getStatus() == ThreadStatus.ONLINE;
    }

    @Override
    public void invokeGeneralAction(@NonNull Thread thread) {
        try {

            boolean online = thread.getStatus() == ThreadStatus.ONLINE;
            boolean pinned = thread.isPinned();
            boolean sendActive = Service.isSendNotificationsEnabled(mContext);

            FragmentManager fm = getChildFragmentManager();

            ThreadActionDialogFragment.newInstance(
                    thread.getIdx(), true, true,
                    topLevel.get(), online, true, sendActive,
                    true, !pinned, pinned)
                    .show(fm, ThreadActionDialogFragment.TAG);

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    @Override
    public void onMarkClick(@NonNull Thread thread) {
        checkNotNull(thread);
        try {
            if (!threads.contains(thread.getIdx())) {
                boolean isEmpty = threads.isEmpty();
                threads.add(thread.getIdx());
                if (isEmpty) {
                    evaluateFabDeleteVisibility();
                }
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }

    }

    @Override
    public void onClick(@NonNull Thread thread) {
        checkNotNull(thread);
        try {
            if (SystemClock.elapsedRealtime() - mLastClickTime < 500) {
                return;
            }
            mLastClickTime = SystemClock.elapsedRealtime();

            if (threads.isEmpty()) {
                threadIdx = thread.getIdx();

                String threadKind = thread.getAdditional(Preferences.THREAD_KIND);
                checkNotNull(threadKind);
                Service.ThreadKind kind = Service.ThreadKind.valueOf(threadKind);
                if (kind == Service.ThreadKind.NODE) {
                    long idx = thread.getIdx();
                    update(idx);
                } else {
                    clickThreadPlay(threadIdx);
                }
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }

    }


    public void clickThreadPlay(long idx) {

        final THREADS threads = Singleton.getInstance(mContext).getThreads();
        final IPFS ipfs = Singleton.getInstance(mContext).getIpfs();
        final int timeout = Preferences.getConnectionTimeout(mContext);
        if (ipfs != null) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    Thread thread = threads.getThreadByIdx(idx);
                    checkNotNull(thread);
                    ThreadStatus status = thread.getStatus();
                    if (status == ThreadStatus.ONLINE || status == ThreadStatus.PUBLISHING) {

                        CID cid = thread.getCid();
                        checkNotNull(cid);

                        String filename = thread.getAdditional(Content.FILENAME);
                        String filesize = thread.getAdditional(Content.FILESIZE);
                        String mimeType = thread.getMimeType();


                        if (mimeType.startsWith("image")) {
                            ImageDialogFragment.newInstance(cid.getCid(), thread.getSesKey()).show(
                                    getChildFragmentManager(), ImageDialogFragment.TAG);

                        } else if (mimeType.startsWith("video")) {

                            VideoDialogFragment dialogFragment = VideoDialogFragment.newInstance(
                                    cid.getCid(), thread.getSesKey(), Long.valueOf(filesize));

                            getChildFragmentManager().beginTransaction().
                                    add(dialogFragment, VideoDialogFragment.TAG).
                                    commitAllowingStateLoss();

                        } else if (mimeType.startsWith("audio")) {
                            File file = new File(ipfs.getCacheDir(), cid.getCid());
                            if (!file.exists()) {
                                ipfs.storeToFile(file, cid, "", timeout, true);
                            }

                            Uri uri = Uri.fromFile(file);
                            AudioDialogFragment.newInstance(uri, filename,
                                    thread.getSenderAlias(), thread.getSesKey())
                                    .show(getChildFragmentManager(), AudioDialogFragment.TAG);


                        } else if (mimeType.startsWith(MimeType.PDF_MIME_TYPE)) {

                            File file = new File(ipfs.getCacheDir(), cid.getCid());
                            if (!file.exists()) {
                                ipfs.storeToFile(file, cid, "", timeout, true);
                            }
                            PDFView.with(getActivity())
                                    .fromfilepath(file.getAbsolutePath())
                                    .swipeHorizontal(true)
                                    .start();

                        } else if (mimeType.equals(MimeType.LINK_MIME_TYPE)) {
                            byte[] data = ipfs.get(cid, "", timeout, true);
                            Uri uri = Uri.parse(new String(data));

                            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);

                        } else if (mimeType.startsWith("text")) {

                            byte[] data = ipfs.get(cid, "", timeout, true);
                            if (data.length > 0) {
                                String content = Base64.encodeToString(data, Base64.NO_PADDING);
                                WebViewDialogFragment.newInstance(mimeType, content, "base64").
                                        show(getChildFragmentManager(), WebViewDialogFragment.TAG);
                            }
                        } else if (mimeType.equals(MimeType.OCTET_MIME_TYPE)) {
                            // TODO improve this (should show text)
                            byte[] data = ipfs.get(cid, "", timeout, true);
                            int length = data.length;
                            if (length > 0 && length < 64000) { // TODO 64kb (better check if content is text)
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
    public void onUnmarkClick(@NonNull Thread thread) {
        checkNotNull(thread);
        try {
            threads.remove(thread.getIdx());
            if (threads.isEmpty()) {
                evaluateFabDeleteVisibility();
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    @Override
    public void onRejectClick(@NonNull Thread thread) {
        checkNotNull(thread);
        // not relevant here
    }

    @Override
    public void onAcceptClick(@NonNull Thread thread) {
        checkNotNull(thread);
        // not relevant here

    }

    @Override
    public void invokeActionError(@NonNull Thread thread) {
        checkNotNull(thread);
        try {
            // CHECKED
            if (!Network.isConnected(mContext)) {
                Singleton singleton = Singleton.getInstance(mContext);
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

    @Override
    @NonNull
    public String getHeader(@NonNull Thread thread) {

        throw new RuntimeException("Not expected here");
    }

    @NonNull
    @Override
    public String getTitle(@NonNull Thread thread) {
        return getCompactString(thread.getAdditional(Content.FILENAME));
    }

    @Override
    public boolean roundImages() {
        return false;
    }

    @Override
    public boolean showProgress() {
        return true;
    }

    @Override
    public int getMediaResource(@NonNull Thread thread) {
        checkNotNull(thread);
        return -1;
    }

    @NonNull
    @Override
    public String getDate(@NonNull Thread thread) {
        checkNotNull(thread);
        return Preferences.getDate(thread.getDate());
    }

    @Override
    public boolean showDate(@NonNull Thread thread) {
        return false;
    }

    @Override
    public int getStyle(@NonNull Thread thread) {
        return 0;
    }

    @Override
    public int getHeaderMediaResource(@NonNull Thread thread) {
        return 0;
    }


    public interface ActionListener {

        void clickThreadsSend(long[] idxs);

        void invalidateOptionsMenu();
    }
}
