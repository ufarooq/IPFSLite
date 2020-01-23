package threads.server.utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Handler;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.selection.ItemDetailsLookup;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.amulyakhare.textdrawable.TextDrawable;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import threads.ipfs.IPFS;
import threads.server.R;
import threads.server.core.threads.Thread;
import threads.server.services.MimeTypeService;

import static androidx.core.util.Preconditions.checkNotNull;

public class ThreadsViewAdapter extends RecyclerView.Adapter<ThreadsViewAdapter.ViewHolder> implements ThreadItemPosition {

    private static final String TAG = ThreadsViewAdapter.class.getSimpleName();
    private final Context mContext;
    private final Handler mHandler;
    private final ThreadsViewAdapterListener listener;
    private final List<Thread> threads = new ArrayList<>();
    private final int accentColor;
    private final int selectedItemColor;
    private final int timeout;
    @Nullable
    private SelectionTracker<Long> mSelectionTracker;

    public ThreadsViewAdapter(@NonNull Context context,
                              @NonNull ThreadsViewAdapterListener listener) {
        this.mContext = context;
        this.listener = listener;
        mHandler = new Handler(context.getMainLooper());
        accentColor = getThemeAccentColor(context);
        selectedItemColor = getThemeSelectedItemColor(context);
        timeout = Preferences.getConnectionTimeout(context);

    }

    private static int getThemeAccentColor(final Context context) {
        final TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.colorAccent, value, true);
        return value.data;
    }

    private static int getThemeSelectedItemColor(final Context context) {
        return ContextCompat.getColor(context, R.color.colorSelectedItem);
    }

    private static String getCompactString(@NonNull String title) {
        checkNotNull(title);
        return title.replace("\n", " ");
    }

    private void runOnUiThread(Runnable r) {
        mHandler.post(r);
    }

    public void setSelectionTracker(SelectionTracker<Long> selectionTracker) {
        this.mSelectionTracker = selectionTracker;
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    @NonNull
    public ThreadsViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                                            int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.threads, parent, false);
        return new ThreadViewHolder(this, v);
    }

    long getIdx(int position) {
        return threads.get(position).getIdx();
    }

    private boolean hasSelection() {
        if (mSelectionTracker != null) {
            return mSelectionTracker.hasSelection();
        }
        return false;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        final Thread thread = threads.get(position);


        ThreadViewHolder threadViewHolder = (ThreadViewHolder) holder;

        boolean isSelected = false;
        if (mSelectionTracker != null) {
            if (mSelectionTracker.isSelected(thread.getIdx())) {
                isSelected = true;
            }
        }

        threadViewHolder.bind(isSelected, thread);
        try {
            if (isSelected) {
                threadViewHolder.view.setBackgroundColor(selectedItemColor);
            } else {
                threadViewHolder.view.setBackgroundColor(
                        android.R.drawable.list_selector_background);
            }

            if (thread.getThumbnail() != null) {
                IPFS ipfs = IPFS.getInstance(mContext);
                IPFSData data = IPFSData.create(ipfs, thread.getThumbnail(), timeout);
                Glide.with(mContext).
                        load(data).
                        into(threadViewHolder.main_image);

            } else {
                int resId = MimeTypeService.getMediaResource(thread.getMimeType());
                threadViewHolder.main_image.setImageResource(resId);
            }

            threadViewHolder.view.setOnClickListener((v) -> {
                try {
                    listener.onClick(thread);
                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
                }
            });


            String title = getCompactString(thread.getName());
            threadViewHolder.content_title.setText(title);
            if (thread.isPinned()) {
                threadViewHolder.content_title.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        R.drawable.pin, 0, 0, 0);
                threadViewHolder.content_title.setCompoundDrawablePadding(8);
            } else {
                threadViewHolder.content_title.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        0, 0, 0, 0);
                threadViewHolder.content_title.setCompoundDrawablePadding(0);
            }


            String message = thread.getSenderAlias();
            threadViewHolder.content_subtitle.setText(message);

            int number = thread.getNumber();

            threadViewHolder.content_subtitle.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    0, 0, 0, 0);
            threadViewHolder.content_subtitle.setCompoundDrawablePadding(0);

            int progress = thread.getProgress();
            if (progress > 0 && progress < 101) {
                runOnUiThread(() ->
                        threadViewHolder.progress_bar.setProgress(progress)
                );
            } else {
                threadViewHolder.progress_bar.setIndeterminate(true);
            }

            if (number > 0) {
                TextDrawable right = TextDrawable.builder().beginConfig()
                        .textColor(Color.BLACK).bold().height(dpToPx()).width(dpToPx()).endConfig()
                        .buildRound("" + number, accentColor);
                threadViewHolder.content_subtitle.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        null, null, right, null);
                threadViewHolder.content_subtitle.setCompoundDrawablePadding(8);
            }

            if (hasSelection()) {
                if (isSelected) {
                    threadViewHolder.general_action.setVisibility(View.VISIBLE);
                    threadViewHolder.general_action.setImageResource(R.drawable.check_circle_outline);
                } else {
                    threadViewHolder.general_action.setVisibility(View.VISIBLE);
                    threadViewHolder.general_action.setImageResource(R.drawable.checkbox_blank_circle_outline);
                }
                threadViewHolder.progress_bar.setVisibility(View.INVISIBLE);
            } else if (thread.isPublishing()) {
                if (listener.generalActionSupport(thread)) {
                    threadViewHolder.general_action.setVisibility(View.VISIBLE);
                    threadViewHolder.general_action.setImageResource(R.drawable.dots);
                    threadViewHolder.general_action.setOnClickListener((v) ->
                            listener.invokeGeneralAction(thread)
                    );
                } else {
                    threadViewHolder.general_action.setVisibility(View.INVISIBLE);
                }

                threadViewHolder.progress_bar.setVisibility(View.VISIBLE);

            } else if (thread.isSeeding()) {
                threadViewHolder.progress_bar.setVisibility(View.INVISIBLE);
                if (listener.generalActionSupport(thread)) {
                    threadViewHolder.general_action.setImageResource(R.drawable.dots);
                    threadViewHolder.general_action.setVisibility(View.VISIBLE);

                    threadViewHolder.general_action.setOnClickListener((v) ->
                            listener.invokeGeneralAction(thread)
                    );
                } else {
                    threadViewHolder.general_action.setVisibility(View.INVISIBLE);
                }
            } else if (thread.isLeaching()) {

                if (listener.generalActionSupport(thread)) {
                    threadViewHolder.general_action.setVisibility(View.VISIBLE);
                    threadViewHolder.general_action.setImageResource(R.drawable.dots);
                    threadViewHolder.general_action.setOnClickListener((v) ->
                            listener.invokeGeneralAction(thread)
                    );
                } else {
                    threadViewHolder.general_action.setVisibility(View.INVISIBLE);
                }

                threadViewHolder.progress_bar.setVisibility(View.VISIBLE);
            } else if (thread.isDeleting()) {
                threadViewHolder.progress_bar.setVisibility(View.INVISIBLE);
                threadViewHolder.general_action.setVisibility(View.INVISIBLE);
            } else {
                threadViewHolder.progress_bar.setVisibility(View.INVISIBLE);
                threadViewHolder.general_action.setVisibility(View.VISIBLE);
                threadViewHolder.general_action.setImageResource(R.drawable.download);
                threadViewHolder.general_action.setOnClickListener((v) ->
                        listener.invokeDownload(thread)
                );
            }

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }


    }

    private int dpToPx() {
        return (int) (25 * Resources.getSystem().getDisplayMetrics().density);
    }

    @Override
    public int getItemCount() {
        return threads.size();
    }

    public void updateData(@NonNull List<Thread> messageThreads) {

        final ThreadDiffCallback diffCallback = new ThreadDiffCallback(this.threads, messageThreads);
        final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);

        this.threads.clear();
        this.threads.addAll(messageThreads);
        diffResult.dispatchUpdatesTo(this);


    }

    public int getPositionOfItem(long idx) {
        try {
            for (int i = 0; i < threads.size(); i++) {
                if (threads.get(i).getIdx() == idx) {
                    return i;
                }
            }
        } catch (Throwable e) {
            // ignore exception
        }
        return -1;
    }

    public void selectAllThreads() {
        try {
            for (Thread thread : threads) {
                if (mSelectionTracker != null) {
                    mSelectionTracker.select(thread.getIdx());
                }
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }

    @Override
    public synchronized int getPosition(long idx) {
        for (int i = 0; i < threads.size(); i++) {
            if (threads.get(i).getIdx() == idx) {
                return i;
            }
        }
        return 0;
    }

    public interface ThreadsViewAdapterListener {

        boolean generalActionSupport(@NonNull Thread thread);

        void invokeGeneralAction(@NonNull Thread thread);

        void onClick(@NonNull Thread thread);

        void invokeDownload(@NonNull Thread thread);


    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        final View view;
        final TextView content_title;
        final TextView content_subtitle;

        ViewHolder(View v) {
            super(v);
            v.setLongClickable(true);
            v.setClickable(true);
            view = v;
            content_title = v.findViewById(R.id.content_title);
            content_subtitle = v.findViewById(R.id.content_subtitle);
        }
    }


    static class ThreadViewHolder extends ViewHolder {
        final ImageView main_image;
        final TextView session_date;
        final ImageView general_action;
        final ProgressBar progress_bar;
        final ThreadItemDetails threadItemDetails;

        ThreadViewHolder(ThreadItemPosition pos, View v) {
            super(v);
            session_date = v.findViewById(R.id.session_date);
            general_action = v.findViewById(R.id.general_action);
            progress_bar = v.findViewById(R.id.progress_bar);
            main_image = v.findViewById(R.id.main_image);
            threadItemDetails = new ThreadItemDetails(pos);

        }

        void bind(boolean isSelected, Thread thread) {

            threadItemDetails.idx = thread.getIdx();

            itemView.setActivated(isSelected);


        }

        ItemDetailsLookup.ItemDetails<Long> getThreadsItemDetails() {

            return threadItemDetails;
        }
    }
}
