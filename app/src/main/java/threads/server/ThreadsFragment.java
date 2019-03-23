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

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.collect.Iterables;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import threads.core.Preferences;
import threads.core.Singleton;
import threads.core.THREADS;
import threads.core.api.Content;
import threads.core.api.Thread;
import threads.core.api.ThreadStatus;
import threads.core.mdl.EventViewModel;
import threads.core.mdl.ThreadViewModel;
import threads.ipfs.Network;
import threads.ipfs.api.CID;
import threads.ipfs.api.PID;
import threads.share.ThreadActionDialogFragment;
import threads.share.ThreadsViewAdapter;
import threads.share.UtilitySevice;

import static com.google.common.base.Preconditions.checkNotNull;

public class ThreadsFragment extends Fragment implements ThreadsViewAdapter.ThreadsViewAdapterListener {


    private static final String DIRECTORY = "DIRECTORY";
    private static final String IDXS = "IDXS";
    private static final String SELECTION = "SELECTION";


    @NonNull
    private final List<Long> threads = new ArrayList<>();
    @NonNull
    private final AtomicReference<LiveData<List<Thread>>> observer = new AtomicReference<>(null);
    @NonNull
    private final AtomicReference<CID> directory = new AtomicReference<>();
    @NonNull
    private final AtomicBoolean topLevel = new AtomicBoolean(true);

    private long threadIdx;
    private CID root;
    private View view;
    private ThreadsViewAdapter threadsViewAdapter;
    private ThreadViewModel threadViewModel;
    private long mLastClickTime = 0;


    private static String getCompactString(@NonNull String title) {
        checkNotNull(title);
        return title.replace("\n", " ");
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_threads, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_mark_all: {

                if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                    break;
                }
                mLastClickTime = SystemClock.elapsedRealtime();

                markThreads();
                return true;
            }
            case R.id.action_unmark_all: {

                if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
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
    @SuppressWarnings("all")
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.threads_view, null);
        return view;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        long[] storedEntries = new long[threads.size()];
        for (int i = 0; i < threads.size(); i++) {
            storedEntries[i] = threads.get(i);
        }
        outState.putLongArray(IDXS, storedEntries);
        outState.putLong(SELECTION, threadIdx);
        CID dir = directory.get();
        outState.putString(DIRECTORY, dir.getCid());
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

            String value = savedInstanceState.getString(DIRECTORY);
            if (value != null) {
                directory.set(CID.create(value));
            }
        }

        Activity activity = getActivity();
        checkNotNull(activity);
        PID pid = Preferences.getPID(activity);
        checkNotNull(pid);
        root = CID.create(pid.getPid());
        CID current = directory.get();
        if (current == null) {
            directory.set(root);
        }
        update(directory.get());

    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        threadViewModel = ViewModelProviders.of(this).get(ThreadViewModel.class);

        RecyclerView mRecyclerView = view.findViewById(R.id.recycler_view_message_list);
        mRecyclerView.setItemAnimator(null); // no animation of the item when something changed

        Activity activity = getActivity();
        checkNotNull(activity);


        LinearLayoutManager linearLayout = new LinearLayoutManager(activity);
        mRecyclerView.setLayoutManager(linearLayout);
        threadsViewAdapter = new ThreadsViewAdapter(activity, this);
        mRecyclerView.setAdapter(threadsViewAdapter);


        FloatingActionButton fab_action = view.findViewById(R.id.fab_action);

        fab_action.setOnClickListener((v) -> {

            if (topLevel.get()) {

                if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                    return;
                }
                mLastClickTime = SystemClock.elapsedRealtime();

                if (getActivity() != null) {
                    ThreadsDialogFragment.newInstance(true, true, true)
                            .show(getActivity().getSupportFragmentManager(), ThreadsDialogFragment.TAG);
                }

            } else {

                if (SystemClock.elapsedRealtime() - mLastClickTime < 500) {
                    return;
                }
                mLastClickTime = SystemClock.elapsedRealtime();


                back();

            }

        });


        FloatingActionButton fab_delete = view.findViewById(R.id.fab_delete);
        fab_delete.setOnClickListener((v) -> {

            // mis-clicking prevention, using threshold of 1000 ms
            if (SystemClock.elapsedRealtime() - mLastClickTime < 1500) {
                return;
            }
            mLastClickTime = SystemClock.elapsedRealtime();

            deleteAction();

        });


        FloatingActionButton fab_send = view.findViewById(R.id.fab_send);
        fab_send.setOnClickListener((v) -> {

            // mis-clicking prevention, using threshold of 1000 ms
            if (SystemClock.elapsedRealtime() - mLastClickTime < 1500) {
                return;
            }
            mLastClickTime = SystemClock.elapsedRealtime();

            sendAction();

        });


        EventViewModel eventViewModel =
                ViewModelProviders.of(this).get(EventViewModel.class);


        eventViewModel.getThreadSelectEvent().observe(this, (event) -> {
            try {
                if (event != null) {

                    if (threadsViewAdapter != null) {
                        String content = event.getContent();
                        try {
                            long idx = Long.valueOf(content);
                            int pos = threadsViewAdapter.getPositionOfItem(idx);
                            if (pos > -1) {
                                threadIdx = idx;
                                threadsViewAdapter.setState(threadIdx,
                                        ThreadsViewAdapter.State.SELECTED);
                                mRecyclerView.scrollToPosition(pos);
                            }
                        } catch (Throwable e) {
                            // ignore exception
                        }
                    }

                }
            } catch (Throwable e) {
                Preferences.evaluateException(Preferences.EXCEPTION, e);
            }

        });

    }

    private void update(@NonNull CID thread) {
        checkNotNull(thread);
        try {

            topLevel.set(thread.equals(root));

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
                    threadsViewAdapter.updateData(data);
                }
            });
        } catch (Throwable e) {
            Preferences.evaluateException(Preferences.EXCEPTION, e);
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

            if (threads.isEmpty()) {
                view.findViewById(R.id.fab_delete).setVisibility(View.INVISIBLE);
                view.findViewById(R.id.fab_send).setVisibility(View.INVISIBLE);
            } else {
                if (topLevel.get()) {
                    view.findViewById(R.id.fab_delete).setVisibility(View.VISIBLE);
                } else {
                    view.findViewById(R.id.fab_delete).setVisibility(View.INVISIBLE);
                }
                view.findViewById(R.id.fab_send).setVisibility(View.VISIBLE);
            }
        } catch (Throwable e) {
            Preferences.evaluateException(Preferences.EXCEPTION, e);
        }
    }

    private void sendAction() {
        Context context = getContext();
        if (context != null) {
            // CHECKED
            if (!Network.isConnected(context)) {
                Preferences.error(getString(R.string.offline_mode));
                return;
            }
            // CHECKED
            if (!Preferences.isDaemonRunning(context)) {
                Preferences.error(getString(R.string.daemon_not_running));
                return;
            }

            Service.sendThreads(context, () -> {
            }, Iterables.toArray(threads, Long.class));
            unmarkThreads();
        }
    }

    private void markThreads() {
        final THREADS threadsAPI = Singleton.getInstance().getThreads();

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

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> threadsViewAdapter.notifyDataSetChanged());
                }
            } catch (Throwable e) {
                Preferences.evaluateException(Preferences.EXCEPTION, e);
            } finally {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(this::evaluateFabDeleteVisibility);
                }
            }
        });
    }

    private void clearUnreadNotes() {
        final THREADS threadsAPI = Singleton.getInstance().getThreads();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try { // TODO faster DB
                threadsAPI.resetThreadUnreadNotesNumber(directory.get());
            } catch (Throwable e) {
                Preferences.evaluateException(Preferences.EXCEPTION, e);
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
            Preferences.evaluateException(Preferences.EXCEPTION, e);
        } finally {
            evaluateFabDeleteVisibility();
        }
    }

    private void deleteAction() {

        Service.deleteThreads(Iterables.toArray(threads, Long.class));
        try {
            if (threads.contains(threadIdx)) {
                threadIdx = -1;
            }
            threads.clear();
        } catch (Throwable e) {
            Preferences.evaluateException(Preferences.EXCEPTION, e);
        } finally {
            evaluateFabDeleteVisibility();
        }

    }

    private void back() {

        unmarkThreads();

        final THREADS threadsAPI = Singleton.getInstance().getThreads();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                CID cid = directory.get();
                if (cid != null) {
                    List<Thread> threads = threadsAPI.getThreadsByCID(cid);
                    if (!threads.isEmpty()) {
                        Thread thread = threads.get(0);
                        CID dir = thread.getThread();
                        checkNotNull(dir);
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> update(dir));
                        }
                    }
                }
            } catch (Throwable e) {
                Preferences.evaluateException(Preferences.EXCEPTION, e);
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
            if (getActivity() != null) {
                boolean sendActive = Preferences.isPubsubEnabled(getActivity());
                FragmentManager fm = getActivity().getSupportFragmentManager();

                ThreadActionDialogFragment.newInstance(
                        thread.getIdx(), true, true, true,
                        topLevel.get(), false, true, sendActive, true)
                        .show(fm, ThreadActionDialogFragment.TAG);
            }
        } catch (Throwable e) {
            Preferences.evaluateException(Preferences.EXCEPTION, e);
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
                    Activity activity = getActivity();
                    if (activity != null) {
                        evaluateFabDeleteVisibility();
                    }
                }
            }
        } catch (Throwable e) {
            Preferences.evaluateException(Preferences.EXCEPTION, e);
        }

    }

    @Override
    public void onClick(@NonNull Thread thread) {
        checkNotNull(thread);
        try {
            if (threads.isEmpty()) {
                threadIdx = thread.getIdx();

                String threadKind = thread.getAdditional(Application.THREAD_KIND);
                checkNotNull(threadKind);
                Service.ThreadKind kind = Service.ThreadKind.valueOf(threadKind);
                if (kind == Service.ThreadKind.NODE) {
                    CID cid = thread.getCid();
                    checkNotNull(cid);
                    update(cid);
                }
            }
        } catch (Throwable e) {
            Preferences.evaluateException(Preferences.EXCEPTION, e);
        }

    }

    @Override
    public void onUnmarkClick(@NonNull Thread thread) {
        checkNotNull(thread);
        try {
            threads.remove(thread.getIdx());
            if (threads.isEmpty()) {
                Activity activity = getActivity();
                if (activity != null) {
                    evaluateFabDeleteVisibility();
                }
            }
        } catch (Throwable e) {
            Preferences.evaluateException(Preferences.EXCEPTION, e);
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
            Activity activity = getActivity();
            if (activity != null) {

                // CHECKED
                if (!Network.isConnected(activity)) {
                    Preferences.error(getString(R.string.offline_mode));
                    return;
                }

                // CHECKED
                if (!Preferences.isDaemonRunning(activity)) {
                    Preferences.error(getString(R.string.daemon_not_running));
                    return;
                }

                Service.downloadThread(activity, thread);
            }
        } catch (Throwable e) {
            Preferences.evaluateException(Preferences.EXCEPTION, e);
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
        return getCompactString(thread.getAdditional(Content.TITLE));
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
        return UtilitySevice.getMediaResource(thread.getMimeType(), false);
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

}
