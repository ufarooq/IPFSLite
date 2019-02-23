package threads.server;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.collect.Iterables;

import java.util.ArrayList;
import java.util.Comparator;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import threads.core.Preferences;
import threads.core.api.Thread;
import threads.core.api.ThreadStatus;
import threads.core.mdl.ThreadsViewModel;
import threads.share.ThreadActionDialogFragment;
import threads.share.ThreadsViewAdapter;

import static com.google.common.base.Preconditions.checkNotNull;

public class ThreadsFragment extends Fragment implements ThreadsViewAdapter.ThreadsViewAdapterListener {
    private static final String TAG = ThreadsFragment.class.getSimpleName();
    private static final String SELECTION = "SELECTION";
    @NonNull
    private final ArrayList<String> threads = new ArrayList<>();
    @NonNull
    private String threadAddress = "";


    private RecyclerView mRecyclerView;
    private View view;
    private ThreadsViewAdapter threadsViewAdapter;
    private long mLastClickTime = 0;


    @Override
    @SuppressWarnings("all")
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.threads_view, null);
        return view;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {

        outState.putStringArrayList(TAG, threads);
        outState.putString(SELECTION, threadAddress);

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            ArrayList<String> storedThreads = savedInstanceState.getStringArrayList(TAG);
            if (storedThreads != null) {
                threads.addAll(storedThreads);
                if (threadsViewAdapter != null) {
                    for (String address : storedThreads) {
                        threadsViewAdapter.setState(address, ThreadsViewAdapter.State.MARKED);
                    }
                }
            }
            String selection = savedInstanceState.getString(SELECTION);
            if (selection != null) {
                threadAddress = selection;
                if (threadsViewAdapter != null) {
                    threadsViewAdapter.setState(threadAddress, ThreadsViewAdapter.State.SELECTED);
                }
            }
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Activity activity = getActivity();
        if (activity != null) {
            mRecyclerView = view.findViewById(R.id.recycler_view_message_list);
            LinearLayoutManager linearLayout = new LinearLayoutManager(activity);
            mRecyclerView.setLayoutManager(linearLayout);
            threadsViewAdapter = new ThreadsViewAdapter(activity, this);
            mRecyclerView.setAdapter(threadsViewAdapter);
            ThreadsViewModel threadsViewModel = ViewModelProviders.of(this).get(ThreadsViewModel.class);
            threadsViewModel.getThreads().observe(this, (threads) -> {
                try {
                    if (threads != null) {
                        threads.sort(Comparator.comparing(Thread::getDate).reversed());
                        threadsViewAdapter.updateData(threads);

                        mRecyclerView.scrollToPosition(0);
                    }
                } catch (Throwable e) {
                    Preferences.evaluateException(Preferences.EXCEPTION, e);
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

            if (getActivity() != null) {
                ActionDialogFragment.newInstance(false, false,
                        true, true)
                        .show(getActivity().getSupportFragmentManager(), ActionDialogFragment.TAG);
            }

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


    }

    private void evaluateFabDeleteVisibility() {
        try {
            if (threads.isEmpty()) {
                view.findViewById(R.id.fab_delete).setVisibility(View.INVISIBLE);
            } else {
                view.findViewById(R.id.fab_delete).setVisibility(View.VISIBLE);
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }


    private void deleteAction() {

        Context context = getContext();
        if (context != null) {

            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);

            alertDialogBuilder.setTitle(R.string.delete_threads);


            alertDialogBuilder
                    .setCancelable(false)
                    .setMessage(R.string.delete_threads_message)
                    .setPositiveButton(android.R.string.yes, (dialog, id) -> {

                        Service.deleteThreads(context, Iterables.toArray(threads, String.class));
                        dialog.dismiss();
                        try {
                            if (threads.contains(threadAddress)) {
                                threadAddress = "";
                            }
                            threads.clear();
                        } finally {
                            evaluateFabDeleteVisibility();
                        }
                    })
                    .setNegativeButton(android.R.string.no, (dialog, id) -> dialog.cancel());


            AlertDialog alertDialog = alertDialogBuilder.create();

            alertDialog.show();

        }
    }

    @Override
    public boolean generalActionSupport(@NonNull ThreadStatus threadStatus) {
        checkNotNull(threadStatus);
        return threadStatus == ThreadStatus.ONLINE;
    }

    @Override
    public void invokeGeneralAction(@NonNull Thread thread) {
        if (getActivity() != null) {
            boolean shareActive = Preferences.isPubsubEnabled(getActivity());
            FragmentManager fm = getActivity().getSupportFragmentManager();

            ThreadActionDialogFragment.newInstance(
                    thread.getAddress(), true, true, true,
                    true, true, shareActive)
                    .show(fm, ThreadActionDialogFragment.TAG);
        }
    }

    @Override
    public void onMarkClick(@NonNull Thread thread) {
        checkNotNull(thread);
        if (!threads.contains(thread.getAddress())) {
            boolean isEmpty = threads.isEmpty();
            threads.add(thread.getAddress());
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
            threadAddress = thread.getAddress();
        }

    }

    @Override
    public void onUnmarkClick(@NonNull Thread thread) {
        checkNotNull(thread);
        threads.remove(thread.getAddress());
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
        Activity activity = getActivity();
        // not relevant here

    }

    @Override
    public void invokeActionError(@NonNull Thread thread) {
        checkNotNull(thread);
        Preferences.warning(getString(R.string.sorry_not_yet_implemented));
    }
}
