package threads.server;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.amulyakhare.textdrawable.TextDrawable;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import threads.core.Preferences;
import threads.core.Singleton;
import threads.core.api.Kind;
import threads.core.api.Thread;
import threads.ipfs.IPFS;
import threads.share.IPFSData;
import threads.share.ThreadDiffCallback;

import static androidx.core.util.Preconditions.checkNotNull;

public class ThreadsViewAdapter extends RecyclerView.Adapter<ThreadsViewAdapter.ViewHolder> {

    private static final String TAG = ThreadsViewAdapter.class.getSimpleName();
    private final Context context;
    private final ThreadsViewAdapterListener listener;
    private final List<Thread> threads = new ArrayList<>();
    private final ConcurrentHashMap<Long, State> states = new ConcurrentHashMap<>();
    private final int accentColor;
    private final int timeout;


    public ThreadsViewAdapter(@NonNull Context context,
                              @NonNull ThreadsViewAdapterListener listener) {
        this.context = context;
        this.listener = listener;
        accentColor = getThemeAccentColor(context);
        timeout = Preferences.getConnectionTimeout(context);

    }


    public static int getThemeAccentColor(final Context context) {
        final TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.colorAccent, value, true);
        return value.data;
    }

    public void setState(long idx, @NonNull State state) {
        checkNotNull(state);
        states.put(idx, state);
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
        return new ThreadViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        final Thread thread = threads.get(position);


        ThreadViewHolder threadViewHolder = (ThreadViewHolder) holder;

        try {
            switch (evaluate(thread)) {
                case SELECTED:
                    threadViewHolder.view.setBackgroundColor(Color.LTGRAY);

                    if (thread.getImage() != null) {
                        threadViewHolder.main_image.setVisibility(View.VISIBLE);
                        IPFS ipfs = Singleton.getInstance(context).getIpfs();
                        IPFSData data = IPFSData.create(ipfs, thread.getImage(), timeout);
                        Glide.with(context).
                                load(data).
                                into(threadViewHolder.main_image);

                    } else {
                        threadViewHolder.main_image.setVisibility(View.GONE);
                    }


                    break;
                case MARKED:
                    threadViewHolder.view.setBackgroundColor(Color.GRAY);

                    if (thread.getImage() != null) {
                        TextDrawable drawable = TextDrawable.builder()
                                .buildRound("\u2713", Color.DKGRAY);
                        threadViewHolder.main_image.setImageDrawable(drawable);
                    } else {
                        threadViewHolder.main_image.setVisibility(View.GONE);
                    }

                    break;
                default:
                    threadViewHolder.view.setBackgroundColor(
                            android.R.drawable.list_selector_background);

                    if (thread.getImage() != null) {
                        IPFS ipfs = Singleton.getInstance(context).getIpfs();
                        threadViewHolder.main_image.setVisibility(View.VISIBLE);
                        IPFSData data = IPFSData.create(ipfs, thread.getImage(), timeout);
                        Glide.with(context).
                                load(data).
                                into(threadViewHolder.main_image);

                    } else {
                        threadViewHolder.main_image.setVisibility(View.GONE);
                    }

                    break;
            }


            threadViewHolder.view.setOnClickListener((v) -> {
                try {
                    if (evaluate(thread) == State.MARKED) {
                        if (thread.getImage() != null) {
                            IPFS ipfs = Singleton.getInstance(context).getIpfs();
                            threadViewHolder.main_image.setVisibility(View.VISIBLE);
                            IPFSData data = IPFSData.create(ipfs, thread.getImage(), timeout);
                            Glide.with(context).
                                    load(data).
                                    into(threadViewHolder.main_image);

                        } else {
                            threadViewHolder.main_image.setVisibility(View.GONE);
                        }
                        // set new status
                        states.remove(thread.getIdx());
                        notifyItemChanged(position);
                        listener.onUnmarkClick(thread);
                    } else {
                        cleanClickedStates();
                        states.put(thread.getIdx(), State.SELECTED);
                        notifyItemChanged(position);
                        listener.onClick(thread);
                    }
                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
                }


            });
            threadViewHolder.view.setOnLongClickListener((v) -> {

                threadViewHolder.view.setBackgroundColor(Color.GRAY);


                if (thread.getImage() != null) {
                    TextDrawable drawable = TextDrawable.builder()
                            .buildRound("\u2713", Color.DKGRAY);
                    threadViewHolder.main_image.setImageDrawable(drawable);
                } else {
                    threadViewHolder.main_image.setVisibility(View.GONE);
                }

                states.put(thread.getIdx(), State.MARKED);
                listener.onMarkClick(thread);
                return true;

            });


            String title = listener.getTitle(thread);
            threadViewHolder.content_title.setText(title);
            if (thread.isPinned()) {
                threadViewHolder.content_title.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        R.drawable.text_pin_outline, 0, 0, 0);
                threadViewHolder.content_title.setCompoundDrawablePadding(8);
            } else {
                threadViewHolder.content_title.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        0, 0, 0, 0);
                threadViewHolder.content_title.setCompoundDrawablePadding(0);
            }


            String message = listener.getContent(thread);
            threadViewHolder.content_subtitle.setText(message);

            int number = thread.getNumber();
            int resource = listener.getMediaResource(thread);

            threadViewHolder.content_subtitle.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    0, 0, 0, 0);
            threadViewHolder.content_subtitle.setCompoundDrawablePadding(0);

            if (resource > 0 && number > 0) {
                TextDrawable right = TextDrawable.builder().beginConfig()
                        .textColor(Color.BLACK).bold().height(dpToPx()).width(dpToPx()).endConfig()
                        .buildRound("" + number, accentColor);
                Drawable left = context.getDrawable(resource);
                threadViewHolder.content_subtitle.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        left, null, right, null);
                threadViewHolder.content_subtitle.setCompoundDrawablePadding(8);
            }
            if (resource > 0 && number <= 0) {
                Drawable left = context.getDrawable(resource);
                threadViewHolder.content_subtitle.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        left, null, null, null);
                threadViewHolder.content_subtitle.setCompoundDrawablePadding(8);
            }
            if (resource <= 0 && number > 0) {
                TextDrawable right = TextDrawable.builder().beginConfig()
                        .textColor(Color.BLACK).bold().height(dpToPx()).width(dpToPx()).endConfig()
                        .buildRound("" + number, accentColor);
                threadViewHolder.content_subtitle.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        null, null, right, null);
                threadViewHolder.content_subtitle.setCompoundDrawablePadding(8);
            }

            if (thread.isPublishing()) {
                if (listener.generalActionSupport(thread)) {
                    threadViewHolder.general_action.setVisibility(View.VISIBLE);
                    threadViewHolder.general_action.setImageResource(R.drawable.dots);
                    threadViewHolder.general_action.setOnClickListener((v) ->
                            listener.invokeGeneralAction(thread)
                    );
                } else {
                    threadViewHolder.general_action.setVisibility(View.GONE);
                }

                threadViewHolder.progress_bar.setVisibility(View.VISIBLE);
                threadViewHolder.session_date.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        0, 0, 0, 0);
                threadViewHolder.session_date.setCompoundDrawablePadding(0);

            } else if (thread.isLeaching()) {

                if (listener.generalActionSupport(thread)) {
                    threadViewHolder.general_action.setVisibility(View.VISIBLE);
                    threadViewHolder.general_action.setImageResource(R.drawable.dots);
                    threadViewHolder.general_action.setOnClickListener((v) ->
                            listener.invokeGeneralAction(thread)
                    );
                } else {
                    threadViewHolder.general_action.setVisibility(View.GONE);
                }

                threadViewHolder.progress_bar.setVisibility(View.VISIBLE);
                threadViewHolder.session_date.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        0, 0, 0, 0);
                threadViewHolder.session_date.setCompoundDrawablePadding(0);
            } else {
                switch (thread.getStatus()) {
                    case ERROR: {
                        threadViewHolder.progress_bar.setVisibility(View.GONE);
                        threadViewHolder.general_action.setVisibility(View.VISIBLE);
                        threadViewHolder.general_action.setImageResource(R.drawable.text_download);
                        threadViewHolder.general_action.setOnClickListener((v) ->
                                listener.invokeActionError(thread)
                        );


                        threadViewHolder.session_date.setCompoundDrawablesRelativeWithIntrinsicBounds(
                                0, 0, 0, 0);
                        threadViewHolder.session_date.setCompoundDrawablePadding(8);

                        break;
                    }

                    case INIT: {

                        if (listener.generalActionSupport(thread)) {
                            threadViewHolder.general_action.setVisibility(View.VISIBLE);
                            threadViewHolder.general_action.setImageResource(R.drawable.dots);
                            threadViewHolder.general_action.setOnClickListener((v) ->
                                    listener.invokeGeneralAction(thread)
                            );
                        } else {
                            threadViewHolder.general_action.setVisibility(View.GONE);
                        }

                        threadViewHolder.progress_bar.setVisibility(View.GONE);


                        if (thread.getKind() == Kind.IN) {
                            threadViewHolder.session_date.setCompoundDrawablesRelativeWithIntrinsicBounds(
                                    0, 0, R.drawable.text_upload, 0);
                            threadViewHolder.session_date.setCompoundDrawablePadding(8);
                        } else {
                            threadViewHolder.session_date.setCompoundDrawablesRelativeWithIntrinsicBounds(
                                    0, 0, R.drawable.text_download, 0);
                            threadViewHolder.session_date.setCompoundDrawablePadding(8);
                        }


                        break;
                    }
                    case DELETING: {
                        threadViewHolder.progress_bar.setVisibility(View.GONE);
                        threadViewHolder.general_action.setVisibility(View.GONE);


                        threadViewHolder.session_date.setCompoundDrawablesRelativeWithIntrinsicBounds(
                                0, 0, R.drawable.text_delete, 0);
                        threadViewHolder.session_date.setCompoundDrawablePadding(8);
                        break;
                    }
                    default: {
                        threadViewHolder.progress_bar.setVisibility(View.GONE);
                        if (listener.generalActionSupport(thread)) {
                            threadViewHolder.general_action.setImageResource(R.drawable.dots);
                            threadViewHolder.general_action.setVisibility(View.VISIBLE);

                            threadViewHolder.general_action.setOnClickListener((v) ->
                                    listener.invokeGeneralAction(thread)
                            );


                        } else {
                            threadViewHolder.general_action.setVisibility(View.GONE);
                        }

                        break;
                    }
                }
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

    private void cleanClickedStates() {
        for (Long idx : states.keySet()) {
            if (states.get(idx) == State.SELECTED) {
                states.remove(idx);
                int position = getPositionOfItem(idx);
                if (position >= 0) {
                    notifyItemChanged(position);
                }
            }
        }
    }

    private State evaluate(@NonNull Thread thread) {
        checkNotNull(thread);
        State state = states.get(thread.getIdx());
        if (state == null) {
            return State.NONE;
        }
        return state;
    }

    public enum State {
        NONE, SELECTED, MARKED
    }

    public interface ThreadsViewAdapterListener {

        boolean generalActionSupport(@NonNull Thread thread);

        void invokeGeneralAction(@NonNull Thread thread);

        void onMarkClick(@NonNull Thread thread);

        void onClick(@NonNull Thread thread);

        void onUnmarkClick(@NonNull Thread thread);


        void invokeActionError(@NonNull Thread thread);

        @NonNull
        String getContent(@NonNull Thread thread);


        @NonNull
        String getTitle(@NonNull Thread thread);


        int getMediaResource(@NonNull Thread thread);


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

        ThreadViewHolder(View v) {
            super(v);
            session_date = v.findViewById(R.id.session_date);
            general_action = v.findViewById(R.id.general_action);
            progress_bar = v.findViewById(R.id.progress_bar);
            main_image = v.findViewById(R.id.main_image);
        }


    }
}
