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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import threads.core.Preferences;
import threads.core.Singleton;
import threads.core.THREADS;
import threads.core.api.Thread;
import threads.core.api.ThreadStatus;
import threads.core.mdl.EventViewModel;
import threads.core.mdl.ThreadViewModel;
import threads.ipfs.Network;
import threads.share.ThreadActionDialogFragment;
import threads.share.ThreadsViewAdapter;

import static com.google.common.base.Preconditions.checkNotNull;

public class ThreadsFragment extends Fragment implements ThreadsViewAdapter.ThreadsViewAdapterListener {
    public static final String TAG = ThreadsFragment.class.getSimpleName();
    public static final String ADDRESS = "ADDRESS";
    private static final String SELECTION = "SELECTION";
    @NonNull
    private final List<Long> threads = new ArrayList<>();

    private long threadIdx;

    private ActionListener actionListener;

    private View view;
    private ThreadsViewAdapter threadsViewAdapter;
    private long mLastClickTime = 0;


    private static String getCompactString(@NonNull String title) {
        checkNotNull(title);
        return title.replace("\n", " ");
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        try {
            actionListener = (ThreadsFragment.ActionListener) getActivity();
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
        inflater.inflate(R.menu.menu_threads, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_download: {

                if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                    break;
                }
                mLastClickTime = SystemClock.elapsedRealtime();

                actionListener.scanMultihash();
                return true;
            }
            case R.id.action_mark_all: {

                if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                    break;
                }
                mLastClickTime = SystemClock.elapsedRealtime();

                new java.lang.Thread(this::markThreads).start();
                return true;
            }
            case R.id.action_unmark_all: {

                if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                    break;
                }
                mLastClickTime = SystemClock.elapsedRealtime();

                new java.lang.Thread(this::unmarkThreads).start();
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
        outState.putLongArray(TAG, storedEntries);
        outState.putLong(SELECTION, threadIdx);

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            long[] storedThreads = savedInstanceState.getLongArray(TAG);
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
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        Bundle args = getArguments();
        checkNotNull(args);
        String address = args.getString(ADDRESS);
        checkNotNull(address);


        RecyclerView mRecyclerView = view.findViewById(R.id.recycler_view_message_list);
        mRecyclerView.setItemAnimator(null); // no animation of the item when something changed


        Activity activity = getActivity();
        if (activity != null) {

            LinearLayoutManager linearLayout = new LinearLayoutManager(activity);
            mRecyclerView.setLayoutManager(linearLayout);
            threadsViewAdapter = new ThreadsViewAdapter(activity, this);
            mRecyclerView.setAdapter(threadsViewAdapter);

            ThreadViewModel threadViewModel = ViewModelProviders.of(this).get(ThreadViewModel.class);

            threadViewModel.getThreads(address).observe(this, (threads) -> {
                if (threads != null) {
                    threads.sort(Comparator.comparing(Thread::getDate).reversed());
                    threadsViewAdapter.updateData(threads);
                }

            });


        }
        threads.clear();


        evaluateFabDeleteVisibility();

        FloatingActionButton fab_action = view.findViewById(R.id.fab_action);
        fab_action.setOnClickListener((v) -> {

            if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                return;
            }
            mLastClickTime = SystemClock.elapsedRealtime();

            actionListener.clickUploadMultihash();

        });


        FloatingActionButton fab_delete = view.findViewById(R.id.fab_delete);
        fab_delete.setOnClickListener((v) -> {

            // mis-clicking prevention, using threshold of 1000 ms
            if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                return;
            }
            mLastClickTime = SystemClock.elapsedRealtime();

            deleteAction();

        });


        FloatingActionButton fab_share = view.findViewById(R.id.fab_share);
        fab_share.setOnClickListener((v) -> {

            // mis-clicking prevention, using threshold of 1000 ms
            if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                return;
            }
            mLastClickTime = SystemClock.elapsedRealtime();

            shareAction();

        });


        EventViewModel eventViewModel = ViewModelProviders.of(this).get(EventViewModel.class);


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
                                threadsViewAdapter.setState(threadIdx, ThreadsViewAdapter.State.SELECTED);
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

    private void evaluateFabDeleteVisibility() {
        try {
            if (threads.isEmpty()) {
                view.findViewById(R.id.fab_delete).setVisibility(View.INVISIBLE);
                view.findViewById(R.id.fab_share).setVisibility(View.INVISIBLE);
            } else {
                view.findViewById(R.id.fab_delete).setVisibility(View.VISIBLE);
                view.findViewById(R.id.fab_share).setVisibility(View.VISIBLE);
            }
        } catch (Throwable e) {
            Preferences.evaluateException(Preferences.EXCEPTION, e);
        }
    }

    private void shareAction() {
        Context context = getContext();
        if (context != null) {
            // CHECKED
            if (!Network.isConnected(context)) {
                Preferences.error(getString(R.string.offline_mode));
                return;
            }
            // CHECKED
            if (!DaemonService.DAEMON_RUNNING.get()) {
                Preferences.error(getString(R.string.daemon_not_running));
                return;
            }

            Service.shareThreads(context, this::unmarkThreads,
                    Iterables.toArray(threads, Long.class));
        }
    }

    private void markThreads() {
        try {
            THREADS threadsAPI = Singleton.getInstance().getThreads();
            List<Thread> threadObjects = threadsAPI.getThreads();
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
    }

    private void unmarkThreads() {
        try {
            for (long idx : threads) {
                threadsViewAdapter.setState(idx, ThreadsViewAdapter.State.NONE);
            }
            threads.clear();
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

    @Override
    public boolean generalActionSupport(@NonNull Thread thread) {
        checkNotNull(thread);
        return thread.getStatus() == ThreadStatus.ONLINE;
    }

    @Override
    public void invokeGeneralAction(@NonNull Thread thread) {
        if (getActivity() != null) {
            boolean shareActive = Preferences.isPubsubEnabled(getActivity());
            FragmentManager fm = getActivity().getSupportFragmentManager();

            ThreadActionDialogFragment.newInstance(
                    thread.getIdx(), true, true, true,
                    true, false, shareActive, true)
                    .show(fm, ThreadActionDialogFragment.TAG);
        }
    }

    @Override
    public void onMarkClick(@NonNull Thread thread) {
        checkNotNull(thread);
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

    }

    @Override
    public void onClick(@NonNull Thread thread) {
        checkNotNull(thread);
        if (threads.isEmpty()) {
            threadIdx = thread.getIdx();

            String threadKind = thread.getAdditional(Application.THREAD_KIND);
            checkNotNull(threadKind);
            Service.ThreadKind kind = Service.ThreadKind.valueOf(threadKind);
            if (kind == Service.ThreadKind.NODE) {
                actionListener.selectThread(thread, this);
            }
        }

    }

    @Override
    public void onUnmarkClick(@NonNull Thread thread) {
        checkNotNull(thread);
        threads.remove(thread.getIdx());
        if (threads.isEmpty()) {
            Activity activity = getActivity();
            if (activity != null) {
                evaluateFabDeleteVisibility();
            }
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

        Activity activity = getActivity();
        if (activity != null) {

            // CHECKED
            if (!Network.isConnected(activity)) {
                Preferences.error(getString(R.string.offline_mode));
                return;
            }

            // CHECKED
            if (!DaemonService.DAEMON_RUNNING.get()) {
                Preferences.error(getString(R.string.daemon_not_running));
                return;
            }

            Service.downloadThread(activity, thread);
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
        return getCompactString(thread.getTitle());
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

        void clickUploadMultihash();

        void scanMultihash();

        void selectThread(@NonNull Thread thread, @NonNull Fragment fragment);
    }
}
