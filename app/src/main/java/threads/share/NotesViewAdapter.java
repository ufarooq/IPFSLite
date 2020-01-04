package threads.share;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.text.Html;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.MultiTransformation;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import threads.core.Preferences;
import threads.core.Singleton;
import threads.core.THREADS;
import threads.core.api.Kind;
import threads.core.api.Note;
import threads.core.api.NoteType;
import threads.core.api.Status;
import threads.ipfs.IPFS;
import threads.server.R;

import static androidx.core.util.Preconditions.checkNotNull;

public class NotesViewAdapter extends RecyclerView.Adapter<NotesViewAdapter.ViewHolder> {
    private static final String TAG = NotesViewAdapter.class.getSimpleName();
    private final Context context;
    private final List<Note> notes = new ArrayList<>();
    private final NotesViewAdapterListener listener;
    private final int timeout;
    private final int accentColor;
    private long mLastClickTime = 0;

    public NotesViewAdapter(@NonNull Context context, @NonNull NotesViewAdapterListener listener) {
        this.context = context;
        this.listener = listener;
        accentColor = getThemeAccentColor(context);
        timeout = Preferences.getConnectionTimeout(context);
    }

    private static int getThemeAccentColor(final Context context) {
        final TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.colorAccent, value, true);
        return value.data;
    }


    @NonNull
    @Override
    public NotesViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View v;
        switch (viewType) {
            case 1:
                v = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.thread_message, parent, false);
                return new MessageHolder(v);
            case 2:
                v = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.thread_info, parent, false);
                return new InfoHolder(v);
            case 3:
                v = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.thread_link, parent, false);
                return new LinkHolder(v);
            case 4:
                v = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.thread_data, parent, false);
                return new DataHolder(v);
            case 5:
                v = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.thread_audio, parent, false);
                return new AudioHolder(v);
            case 6:
                v = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.thread_html, parent, false);
                return new HtmlHolder(v);
            case 7:
                v = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.thread_location, parent, false);
                return new LocationHolder(v);

        }
        throw new RuntimeException("View Type not supported !!!");
    }

    @Override
    public void onBindViewHolder(@NonNull NotesViewAdapter.ViewHolder holder, final int position) {
        Note note = notes.get(position);
        checkNotNull(note);
        try {
            if (holder instanceof InfoHolder) {
                InfoHolder noteHolder = (InfoHolder) holder;
                handleInfoNote(noteHolder, note);
            }
            if (holder instanceof HtmlHolder) {
                HtmlHolder htmlHolder = (HtmlHolder) holder;
                handleHtmlNote(htmlHolder, note);
            }
            if (holder instanceof AudioHolder) {
                AudioHolder audioHolder = (AudioHolder) holder;
                handleAudioNote(audioHolder, note);
            }
            if (holder instanceof MessageHolder) {
                MessageHolder messageHolder = (MessageHolder) holder;
                handleMessageNote(messageHolder, note);
            }
            if (holder instanceof LinkHolder) {
                LinkHolder linkHolder = (LinkHolder) holder;
                handleLinkNote(linkHolder, note);
            }
            if (holder instanceof DataHolder) {
                DataHolder dataHolder = (DataHolder) holder;
                handleDataNote(dataHolder, note);
            }
            if (holder instanceof LocationHolder) {
                LocationHolder locationHolder = (LocationHolder) holder;
                handleLocationNote(locationHolder, note);
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }

    }

    private void handleInfoNote(@NonNull InfoHolder infoHolder, @NonNull Note note) {
        checkNotNull(infoHolder);
        checkNotNull(note);

        TextView textView = infoHolder.message_body;
        textView.setTextAppearance(android.R.style.TextAppearance_Small);
        String content = listener.getContent(note);
        textView.setText(content);

        if (listener.showHeader(note)) {
            Date date = new Date(note.getDate());
            infoHolder.date.setText(Preferences.getDate(date));
        }
    }

    private void handleHtmlNote(@NonNull HtmlHolder htmlHolder, @NonNull Note note) {
        checkNotNull(htmlHolder);
        checkNotNull(note);

        TextView textView = htmlHolder.message_body;
        textView.setTextAppearance(android.R.style.TextAppearance_Medium);

        String content = listener.getContent(note);
        textView.setTextAppearance(android.R.style.TextAppearance_Small);
        textView.setText(Html.fromHtml(content, Html.FROM_HTML_MODE_LEGACY));


        int dateStartRes = 0;
        int padding = 0;
        if (listener.showHeader(note)) {
            Date date = new Date(note.getDate());
            htmlHolder.date.setText(Preferences.getDate(date));

            if (listener.isEncrypted(note)) {
                padding = 8;
                if (note.isEncrypted()) {
                    dateStartRes = R.drawable.text_lock;
                } else {
                    dateStartRes = R.drawable.text_lock_open;
                }
            }
        }
        htmlHolder.image_action.setVisibility(View.INVISIBLE);


        if (note.getImage() != null) {
            MultiTransformation transform = new MultiTransformation(
                    new CenterCrop(), new RoundedCorners(5));
            checkNotNull(transform);
            IPFS ipfs = Singleton.getInstance(context).getIpfs();
            IPFSData data = IPFSData.create(ipfs, note.getImage(), timeout);
            Glide.with(context).load(data).apply(
                    new RequestOptions()
                            .timeout(timeout)
                            .placeholder(R.drawable.text_gallery)
                            .error(R.drawable.no_image_available)
                            .transform(transform))
                    .into(htmlHolder.image_main);

        } else {
            htmlHolder.image_main.setVisibility(View.GONE);
        }

        if (note.isPublishing()) {
            htmlHolder.date.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    dateStartRes, 0, 0, 0);
            htmlHolder.date.setCompoundDrawablePadding(padding);
            htmlHolder.image_action.setVisibility(View.VISIBLE);
            htmlHolder.image_action.setClickable(false);
            htmlHolder.image_action.setImageResource(R.drawable.sync_upload);

        } else if (note.isLeaching()) {
            htmlHolder.date.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    dateStartRes, 0, 0, 0);
            htmlHolder.date.setCompoundDrawablePadding(padding);
            htmlHolder.image_action.setVisibility(View.VISIBLE);
            htmlHolder.image_action.setClickable(false);
            htmlHolder.image_action.setImageResource(R.drawable.sync_active_download);

        } else {
            switch (note.getStatus()) {
                case DONE: {
                    htmlHolder.date.setCompoundDrawablesRelativeWithIntrinsicBounds(
                            dateStartRes, 0, 0, 0);
                    htmlHolder.date.setCompoundDrawablePadding(padding);
                    htmlHolder.image_action.setVisibility(View.INVISIBLE);
                    break;
                }
                case DELETING:
                    htmlHolder.date.setCompoundDrawablesRelativeWithIntrinsicBounds(
                            dateStartRes, 0, R.drawable.text_delete, 0);
                    htmlHolder.date.setCompoundDrawablePadding(padding);
                    break;
                case ERROR: {
                    if (!note.isExpired()) {
                        htmlHolder.date.setCompoundDrawablesRelativeWithIntrinsicBounds(
                                dateStartRes, 0, 0, 0);
                        htmlHolder.date.setCompoundDrawablePadding(padding);
                        htmlHolder.image_action.setVisibility(View.VISIBLE);
                        htmlHolder.image_action.setClickable(true);
                        if (note.getKind() == Kind.OUT) {
                            htmlHolder.image_action.setImageResource(R.drawable.sync_alert);
                        } else {
                            htmlHolder.image_action.setImageResource(R.drawable.sync_download);
                        }
                        htmlHolder.image_action.setOnClickListener((v) -> {

                            if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                                return;
                            }
                            mLastClickTime = SystemClock.elapsedRealtime();

                            listener.invokeHtmlActionError(note);
                        });
                    } else {
                        htmlHolder.date.setCompoundDrawablesRelativeWithIntrinsicBounds(
                                dateStartRes, 0, 0, 0);
                        htmlHolder.date.setCompoundDrawablePadding(padding);
                        htmlHolder.image_action.setVisibility(View.INVISIBLE);
                    }
                    break;
                }
                default:
                    htmlHolder.date.setCompoundDrawablesRelativeWithIntrinsicBounds(
                            dateStartRes, 0, 0, 0);
                    htmlHolder.date.setCompoundDrawablePadding(padding);
                    htmlHolder.image_action.setVisibility(View.INVISIBLE);

                    break;
            }
        }
    }


    private int dpToPx() {
        return (int) (25 * Resources.getSystem().getDisplayMetrics().density);
    }

    private void handleDate(@NonNull ViewHolder holder, @NonNull Note note) {
        Date date = new Date(note.getDate());
        holder.date.setText(Preferences.getDate(date));

        int dateStartRes = 0;
        int dateEndRes = 0;
        int padding = 0;
        if (listener.isEncrypted(note)) {
            if (note.isEncrypted()) {
                padding = 8;
                dateStartRes = R.drawable.text_lock;
            } /* else {
                padding = 8;
                dateStartRes = R.drawable.text_lock_open;
            }*/

        }
        if (note.isExpired()) {
            dateEndRes = R.drawable.error_timer_off;
            padding = 8;
        }
        if (note.getStatus() == Status.DELETING) {
            dateEndRes = R.drawable.text_delete;
            padding = 8;
        }

        if (dateEndRes == 0) {
            int number = note.getNumber();
            if (number > 0) {
                TextDrawable right = TextDrawable.builder().beginConfig()
                        .textColor(Color.BLACK).bold().height(dpToPx()).width(dpToPx()).endConfig()
                        .buildRound("" + number, accentColor);
                if (dateStartRes > 0) {
                    Drawable left = context.getDrawable(dateStartRes);
                    holder.date.setCompoundDrawablesRelativeWithIntrinsicBounds(
                            left, null, right, null);
                    holder.date.setCompoundDrawablePadding(8);
                } else {
                    holder.date.setCompoundDrawablesRelativeWithIntrinsicBounds(
                            null, null, right, null);
                    holder.date.setCompoundDrawablePadding(8);
                }

            } else {
                holder.date.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        dateStartRes, 0, 0, 0);
                holder.date.setCompoundDrawablePadding(padding);
            }
        } else {
            holder.date.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    dateStartRes, 0, dateEndRes, 0);
            holder.date.setCompoundDrawablePadding(padding);
        }

    }

    private void handleMessageNote(@NonNull MessageHolder messageHolder, @NonNull Note note) {
        checkNotNull(messageHolder);
        checkNotNull(note);


        boolean showHeader = listener.showHeader(note);

        if (showHeader) {
            messageHolder.header.setVisibility(View.VISIBLE);

            String author = note.getSenderAlias();
            messageHolder.user.setText(author);
            int color = ColorGenerator.MATERIAL.getColor(author);
            messageHolder.user.setTextColor(color);

            if (note.isBlocked()) {
                messageHolder.user.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        R.drawable.blocked, 0, 0, 0);
                messageHolder.user.setCompoundDrawablePadding(8);
            } else {
                messageHolder.user.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        0, 0, 0, 0);
                messageHolder.user.setCompoundDrawablePadding(0);
            }

            handleDate(messageHolder, note);

        } else {
            messageHolder.header.setVisibility(View.GONE);
        }

        TextView textView = messageHolder.message_body;
        if (note.getNoteType() == NoteType.MESSAGE) {
            textView.setTextAppearance(android.R.style.TextAppearance_Medium);
        } else {
            textView.setTextAppearance(android.R.style.TextAppearance_Small);
        }

        String content = listener.getContent(note);
        textView.setText(content);

        if (note.isPublishing()) {
            messageHolder.image_action.setVisibility(View.GONE);
            messageHolder.image_action.setClickable(false);
            if (showHeader) {
                messageHolder.general_action.setVisibility(View.VISIBLE);
                messageHolder.general_action.setClickable(false);
                messageHolder.general_action.setImageResource(R.drawable.sync_upload);
            }
            messageHolder.progress_bar.setVisibility(View.VISIBLE);
            messageHolder.progress_bar.setIndeterminate(true);

        } else if (note.isLeaching()) {
            messageHolder.image_action.setVisibility(View.GONE);
            messageHolder.image_action.setClickable(false);
            if (showHeader) {
                messageHolder.general_action.setVisibility(View.VISIBLE);
                messageHolder.general_action.setClickable(false);
                messageHolder.general_action.setImageResource(R.drawable.sync_active_download);
            }
            messageHolder.progress_bar.setVisibility(View.VISIBLE);
            messageHolder.progress_bar.setIndeterminate(true);
        } else {
            messageHolder.progress_bar.setVisibility(View.GONE);

            if (showHeader) {
                if (listener.generalActionSupport(note)) {
                    messageHolder.general_action.setImageResource(R.drawable.dots);
                    messageHolder.general_action.setClickable(true);
                    messageHolder.general_action.setVisibility(View.VISIBLE);
                    messageHolder.general_action.setOnClickListener((v) ->
                            listener.invokeGeneralAction(note)
                    );
                } else {
                    messageHolder.general_action.setVisibility(View.GONE);
                }
            }

            switch (note.getStatus()) {

                case ERROR: {
                    if (!note.isExpired()) {
                        messageHolder.image_action.setVisibility(View.VISIBLE);
                        messageHolder.image_action.setClickable(true);
                        if (note.getKind() == Kind.OUT) {
                            messageHolder.image_action.setImageResource(R.drawable.sync_alert);
                        } else {
                            messageHolder.image_action.setImageResource(R.drawable.sync_download);
                        }
                        messageHolder.image_action.setOnClickListener((v) -> {
                            if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                                return;
                            }
                            mLastClickTime = SystemClock.elapsedRealtime();

                            listener.invokeMessageActionError(note);
                        });
                    } else {
                        messageHolder.image_action.setVisibility(View.GONE);
                    }
                    break;
                }
                case DONE:
                default: {
                    messageHolder.image_action.setVisibility(View.GONE);
                    break;
                }
            }
        }
    }


    private void handleAudioNote(@NonNull AudioHolder audioHolder, @NonNull Note note) {
        checkNotNull(audioHolder);
        checkNotNull(note);

        boolean showHeader = listener.showHeader(note);
        if (showHeader) {
            audioHolder.header.setVisibility(View.VISIBLE);


            String author = note.getSenderAlias();
            audioHolder.user.setText(author);
            int color = ColorGenerator.MATERIAL.getColor(author);
            audioHolder.user.setTextColor(color);
            if (note.isBlocked()) {
                audioHolder.user.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        R.drawable.blocked, 0, 0, 0);
                audioHolder.user.setCompoundDrawablePadding(8);
            } else {
                audioHolder.user.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        0, 0, 0, 0);
                audioHolder.user.setCompoundDrawablePadding(0);

            }

            handleDate(audioHolder, note);

        } else {
            audioHolder.header.setVisibility(View.GONE);
        }

        String title = listener.getTitle(note);
        audioHolder.audio_title.setText(title);


        if (note.isPublishing()) {
            audioHolder.image_action.setVisibility(View.INVISIBLE);
            audioHolder.image_action.setClickable(false);
            if (showHeader) {
                audioHolder.general_action.setVisibility(View.VISIBLE);
                audioHolder.general_action.setClickable(false);
                audioHolder.general_action.setImageResource(R.drawable.sync_upload);
            }
            audioHolder.progress_bar.setVisibility(View.VISIBLE);
            audioHolder.progress_bar.setIndeterminate(true);
        } else if (note.isLeaching()) {

            audioHolder.image_action.setVisibility(View.INVISIBLE);
            audioHolder.image_action.setClickable(false);
            if (showHeader) {
                audioHolder.general_action.setVisibility(View.VISIBLE);
                audioHolder.general_action.setClickable(false);
                audioHolder.general_action.setImageResource(R.drawable.sync_active_download);
            }
            audioHolder.progress_bar.setVisibility(View.VISIBLE);
            audioHolder.progress_bar.setIndeterminate(true);


        } else {
            audioHolder.progress_bar.setVisibility(View.GONE);

            if (showHeader) {
                if (listener.generalActionSupport(note)) {
                    audioHolder.general_action.setImageResource(R.drawable.dots);
                    audioHolder.general_action.setClickable(true);
                    audioHolder.general_action.setVisibility(View.VISIBLE);
                    audioHolder.general_action.setOnClickListener((v) ->
                            listener.invokeGeneralAction(note)
                    );
                } else {
                    audioHolder.general_action.setVisibility(View.GONE);
                }
            }
            switch (note.getStatus()) {
                case SEMI: {
                    if (!note.isExpired()) {
                        audioHolder.image_action.setVisibility(View.VISIBLE);
                        if (note.getKind() == Kind.OUT) {
                            audioHolder.image_action.setImageResource(R.drawable.sync_alert);
                        } else {
                            audioHolder.image_action.setImageResource(R.drawable.sync_download);
                        }
                        audioHolder.image_action.setClickable(true);
                        audioHolder.image_action.setOnClickListener((view) -> {
                            if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                                return;
                            }
                            mLastClickTime = SystemClock.elapsedRealtime();

                            listener.invokeAudioActionError(note);
                        });
                    } else {
                        audioHolder.image_action.setVisibility(View.INVISIBLE);
                    }
                    break;
                }

                case DONE: {
                    audioHolder.image_action.setVisibility(View.VISIBLE);
                    audioHolder.image_action.setImageResource(R.drawable.play_circle);
                    audioHolder.image_action.setClickable(true);
                    audioHolder.image_action.setOnClickListener((view) -> {
                        if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                            return;
                        }
                        mLastClickTime = SystemClock.elapsedRealtime();

                        listener.invokeAudioAction(note);
                    });

                    break;
                }

                case ERROR: {
                    if (!note.isExpired()) {
                        audioHolder.image_action.setVisibility(View.VISIBLE);
                        audioHolder.image_action.setClickable(true);
                        if (note.getKind() == Kind.OUT) {
                            audioHolder.image_action.setImageResource(R.drawable.sync_alert);
                        } else {
                            audioHolder.image_action.setImageResource(R.drawable.sync_download);
                        }
                        audioHolder.image_action.setOnClickListener((v) -> {

                            if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                                return;
                            }
                            mLastClickTime = SystemClock.elapsedRealtime();

                            listener.invokeAudioActionError(note);
                        });
                    } else {
                        audioHolder.image_action.setVisibility(View.INVISIBLE);
                    }
                    break;
                }
                default:
                    audioHolder.image_action.setVisibility(View.INVISIBLE);
                    break;
            }
        }
    }


    private void handleLocationNote(@NonNull LocationHolder locationHolder, @NonNull Note note) {
        checkNotNull(locationHolder);
        checkNotNull(note);

        boolean showHeader = listener.showHeader(note);

        if (showHeader) {
            locationHolder.header.setVisibility(View.VISIBLE);

            String author = note.getSenderAlias();
            locationHolder.user.setText(author);
            int color = ColorGenerator.MATERIAL.getColor(author);
            locationHolder.user.setTextColor(color);
            if (note.isBlocked()) {
                locationHolder.user.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        R.drawable.blocked, 0, 0, 0);
                locationHolder.user.setCompoundDrawablePadding(8);
            } else {
                locationHolder.user.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        0, 0, 0, 0);
                locationHolder.user.setCompoundDrawablePadding(0);

            }

            handleDate(locationHolder, note);

        } else {
            locationHolder.header.setVisibility(View.GONE);
        }


        if (note.getImage() != null) {
            MultiTransformation transform = new MultiTransformation(
                    new CenterCrop(), new RoundedCorners(5));
            checkNotNull(transform);
            IPFS ipfs = Singleton.getInstance(context).getIpfs();
            IPFSData data = IPFSData.create(ipfs, note.getImage(), timeout);
            Glide.with(context).load(data).apply(
                    new RequestOptions()
                            .timeout(timeout)
                            .placeholder(R.drawable.text_map_marker)
                            .error(R.drawable.no_image_available)
                            .transform(transform))
                    .into(locationHolder.image_view);

        } else {
            locationHolder.image_view.setImageResource(R.drawable.text_map_marker);
        }

        if (note.isPublishing()) {
            locationHolder.image_action.setVisibility(View.GONE);
            locationHolder.image_action.setClickable(false);
            if (showHeader) {
                locationHolder.general_action.setVisibility(View.VISIBLE);
                locationHolder.general_action.setClickable(false);
                locationHolder.general_action.setImageResource(R.drawable.sync_upload);
            }
            locationHolder.progress_bar.setVisibility(View.VISIBLE);
            locationHolder.progress_bar.setIndeterminate(true);
        } else if (note.isLeaching()) {

            locationHolder.image_action.setVisibility(View.GONE);
            locationHolder.image_action.setClickable(false);
            if (showHeader) {
                locationHolder.general_action.setVisibility(View.VISIBLE);
                locationHolder.general_action.setClickable(false);
                locationHolder.general_action.setImageResource(R.drawable.sync_active_download);
            }
            locationHolder.progress_bar.setVisibility(View.VISIBLE);
            locationHolder.progress_bar.setIndeterminate(true);

        } else {
            locationHolder.progress_bar.setVisibility(View.GONE);

            if (showHeader) {
                if (listener.generalActionSupport(note)) {
                    locationHolder.general_action.setImageResource(R.drawable.dots);
                    locationHolder.general_action.setClickable(true);
                    locationHolder.general_action.setVisibility(View.VISIBLE);
                    locationHolder.general_action.setOnClickListener((v) ->
                            listener.invokeGeneralAction(note)
                    );
                } else {
                    locationHolder.general_action.setVisibility(View.GONE);
                }
            }
            switch (note.getStatus()) {

                case DONE: {
                    locationHolder.image_action.setVisibility(View.GONE);
                    locationHolder.image_view.setClickable(true);
                    locationHolder.image_view.setOnClickListener((v) -> {
                        if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                            return;
                        }
                        mLastClickTime = SystemClock.elapsedRealtime();
                        listener.invokeLocationAction(note);
                    });


                    break;
                }
                case ERROR: {
                    if (!note.isExpired()) {
                        locationHolder.image_action.setVisibility(View.VISIBLE);
                        locationHolder.image_action.setClickable(true);
                        if (note.getKind() == Kind.OUT) {
                            locationHolder.image_action.setImageResource(R.drawable.sync_alert);
                        } else {
                            locationHolder.image_action.setImageResource(R.drawable.sync_download);
                        }
                        locationHolder.image_action.setOnClickListener((v) -> {
                            if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                                return;
                            }
                            mLastClickTime = SystemClock.elapsedRealtime();

                            listener.invokeLocationActionError(note);
                        });
                    } else {
                        locationHolder.image_action.setVisibility(View.GONE);
                    }
                    break;
                }
                default:
                    locationHolder.image_action.setVisibility(View.GONE);
                    break;
            }
        }
    }

    private void handleDataNote(@NonNull DataHolder dataHolder, @NonNull Note note) {
        checkNotNull(dataHolder);
        checkNotNull(note);

        boolean showHeader = listener.showHeader(note);
        if (showHeader) {
            dataHolder.header.setVisibility(View.VISIBLE);

            String author = note.getSenderAlias();
            dataHolder.user.setText(author);
            int color = ColorGenerator.MATERIAL.getColor(author);
            dataHolder.user.setTextColor(color);
            if (note.isBlocked()) {
                dataHolder.user.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        R.drawable.blocked, 0, 0, 0);
                dataHolder.user.setCompoundDrawablePadding(8);
            } else {
                dataHolder.user.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        0, 0, 0, 0);
                dataHolder.user.setCompoundDrawablePadding(0);

            }

            handleDate(dataHolder, note);

        } else {
            dataHolder.header.setVisibility(View.GONE);
        }

        if (note.getImage() != null) {
            MultiTransformation transform = new MultiTransformation(
                    new CenterCrop(), new RoundedCorners(5));
            checkNotNull(transform);
            IPFS ipfs = Singleton.getInstance(context).getIpfs();
            IPFSData data = IPFSData.create(ipfs, note.getImage(), timeout);
            Glide.with(context).load(data).apply(
                    new RequestOptions()
                            .timeout(timeout)
                            .placeholder(R.drawable.text_gallery)
                            .error(R.drawable.no_image_available)
                            .transform(transform))
                    .into(dataHolder.image_view);

        } else {
            dataHolder.image_view.setImageResource(R.drawable.text_file);
        }

        if (note.isPublishing()) {
            dataHolder.image_action.setVisibility(View.GONE);
            dataHolder.image_action.setClickable(false);
            if (showHeader) {
                dataHolder.general_action.setVisibility(View.VISIBLE);
                dataHolder.general_action.setClickable(false);
                dataHolder.general_action.setImageResource(R.drawable.sync_upload);
            }
            dataHolder.progress_bar.setVisibility(View.VISIBLE);
            dataHolder.progress_bar.setIndeterminate(true);
        } else if (note.isLeaching()) {
            dataHolder.image_action.setVisibility(View.GONE);
            dataHolder.image_action.setClickable(false);
            if (showHeader) {
                dataHolder.general_action.setVisibility(View.VISIBLE);
                dataHolder.general_action.setClickable(false);
                dataHolder.general_action.setImageResource(R.drawable.sync_active_download);
            }
            dataHolder.progress_bar.setVisibility(View.VISIBLE);
            dataHolder.progress_bar.setIndeterminate(true);
        } else {
            dataHolder.progress_bar.setVisibility(View.GONE);

            if (showHeader) {
                if (listener.generalActionSupport(note)) {
                    dataHolder.general_action.setImageResource(R.drawable.dots);
                    dataHolder.general_action.setClickable(true);
                    dataHolder.general_action.setVisibility(View.VISIBLE);
                    dataHolder.general_action.setOnClickListener((v) ->
                            listener.invokeGeneralAction(note)
                    );
                } else {
                    dataHolder.general_action.setVisibility(View.GONE);
                }
            }

            switch (note.getStatus()) {
                case SEMI: {
                    if (!note.isExpired()) {
                        dataHolder.image_action.setVisibility(View.VISIBLE);
                        dataHolder.image_action.setClickable(true);
                        if (note.getKind() == Kind.OUT) {
                            dataHolder.image_action.setImageResource(R.drawable.sync_alert);
                        } else {
                            dataHolder.image_action.setImageResource(R.drawable.sync_download);
                        }
                        dataHolder.image_action.setOnClickListener((v) -> {
                            if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                                return;
                            }
                            mLastClickTime = SystemClock.elapsedRealtime();

                            listener.invokeDataActionError(note);
                        });
                    } else {
                        dataHolder.image_action.setVisibility(View.GONE);
                    }
                    break;
                }

                case DONE: {
                    if (note.getMimeType().startsWith("video")) {

                        dataHolder.image_action.setVisibility(View.VISIBLE);
                        dataHolder.image_action.setImageResource(R.drawable.play_circle);
                        dataHolder.image_action.setClickable(true);
                        dataHolder.image_action.setOnClickListener((v) -> {

                            if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                                return;
                            }
                            mLastClickTime = SystemClock.elapsedRealtime();
                            listener.invokeVideoAction(note);
                        });

                    } else {

                        dataHolder.image_action.setVisibility(View.GONE);
                        dataHolder.image_view.setClickable(true);
                        dataHolder.image_view.setOnClickListener((v) -> {

                            if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                                return;
                            }
                            mLastClickTime = SystemClock.elapsedRealtime();
                            listener.invokeDataAction(note);
                        });

                    }

                    break;
                }
                case ERROR: {
                    if (!note.isExpired()) {
                        dataHolder.image_action.setVisibility(View.VISIBLE);
                        dataHolder.image_action.setClickable(true);
                        if (note.getKind() == Kind.OUT) {
                            dataHolder.image_action.setImageResource(R.drawable.sync_alert);
                        } else {
                            dataHolder.image_action.setImageResource(R.drawable.sync_download);
                        }
                        dataHolder.image_action.setOnClickListener((v) -> {
                            if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                                return;
                            }
                            mLastClickTime = SystemClock.elapsedRealtime();

                            listener.invokeDataActionError(note);
                        });
                    } else {
                        dataHolder.image_action.setVisibility(View.GONE);
                    }
                    break;
                }
                default:
                    dataHolder.image_action.setVisibility(View.GONE);
                    break;
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        Note notification = getItemAtPosition(position);
        checkNotNull(notification);
        NoteType noteType = notification.getNoteType();
        checkNotNull(noteType);

        if (noteType == NoteType.LOCATION) {
            return 7;
        }

        if (noteType == NoteType.HTML) {
            return 6;
        }

        if (noteType == NoteType.AUDIO) {
            return 5;
        }

        if (noteType == NoteType.DATA) {
            return 4;
        }

        if (noteType == NoteType.LINK) {
            return 3;
        }

        if (noteType == NoteType.INFO) {
            return 2;
        }

        return 1;
    }


    private Note getItemAtPosition(int position) {
        try {
            return notes.get(position);
        } catch (Throwable e) {
            // ignore exception
        }
        return null;
    }


    @Override
    public int getItemCount() {
        return notes.size();
    }

    public void updateNotes(@NonNull List<Note> notes) {

        final NoteDiffCallback diffCallback = new NoteDiffCallback(this.notes, notes);
        final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);

        this.notes.clear();
        this.notes.addAll(notes);
        diffResult.dispatchUpdatesTo(this);
    }

    private void handleLinkNote(@NonNull LinkHolder linkHolder, @NonNull Note note) {
        checkNotNull(linkHolder);
        checkNotNull(note);
        boolean showHeader = listener.showHeader(note);

        if (showHeader) {
            linkHolder.header.setVisibility(View.VISIBLE);


            String author = StringUtils.left(note.getSenderAlias(), 30);
            linkHolder.user.setText(author);
            int color = ColorGenerator.MATERIAL.getColor(author);
            linkHolder.user.setTextColor(color);

            if (note.isBlocked()) {
                linkHolder.user.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        R.drawable.blocked, 0, 0, 0);
                linkHolder.user.setCompoundDrawablePadding(8);
            } else {
                linkHolder.user.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        0, 0, 0, 0);
                linkHolder.user.setCompoundDrawablePadding(0);

            }

            handleDate(linkHolder, note);

        } else {
            linkHolder.header.setVisibility(View.GONE);
        }


        TextView textView = linkHolder.message_body;
        textView.setTextAppearance(android.R.style.TextAppearance_Small);

        int res = listener.getMediaResource(note);
        if (note.getImage() != null) {
            MultiTransformation transform = new MultiTransformation(
                    new CenterCrop(), new RoundedCorners(10));
            checkNotNull(transform);
            IPFS ipfs = Singleton.getInstance(context).getIpfs();
            IPFSData data = IPFSData.create(ipfs, note.getImage(), timeout);
            Glide.with(context).load(data).apply(
                    new RequestOptions()
                            .timeout(timeout)
                            .placeholder(res)
                            .error(res)
                            .transform(transform))
                    .into(linkHolder.image_view);


            linkHolder.image_view.setOnClickListener((v) -> {

                try {
                    if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                        return;
                    }
                    mLastClickTime = SystemClock.elapsedRealtime();


                    if (note.getStatus() == Status.DONE &&
                            listener.isLinkActionSupported(note)) {
                        listener.invokeLinkAction(note);
                    } else {
                        listener.invokeLinkImageAction(note);
                    }

                } catch (Throwable e) {
                    Log.e(TAG, "" + e.getLocalizedMessage(), e);
                }
            });

        } else {
            linkHolder.image_view.setImageResource(res);
            linkHolder.image_view.setClickable(false);
        }


        THREADS threads = Singleton.getInstance(context).getThreads();
        String fileName = threads.getFileName(note);
        String fileSize = String.valueOf(threads.getFileSize(note) / 1000);
        textView.setText(context.getString(R.string.link_format, fileName, fileSize));
        if (note.isPublishing()) {
            linkHolder.image_action.setVisibility(View.GONE);
            linkHolder.image_action.setClickable(false);
            if (showHeader) {
                linkHolder.general_action.setClickable(false);
                linkHolder.general_action.setVisibility(View.VISIBLE);
                linkHolder.general_action.setImageResource(R.drawable.sync_upload);
            }
            linkHolder.progress_bar.setVisibility(View.VISIBLE);
            linkHolder.progress_bar.setIndeterminate(true);

        } else if (note.isLeaching()) {
            linkHolder.image_action.setVisibility(View.GONE);
            linkHolder.image_action.setClickable(false);
            if (showHeader) {
                linkHolder.general_action.setClickable(false);
                linkHolder.general_action.setVisibility(View.VISIBLE);
                linkHolder.general_action.setImageResource(R.drawable.sync_active_download);
            }
            linkHolder.progress_bar.setVisibility(View.VISIBLE);
            linkHolder.progress_bar.setIndeterminate(true);

        } else {
            linkHolder.progress_bar.setVisibility(View.GONE);

            if (showHeader) {
                if (listener.generalActionSupport(note)) {
                    linkHolder.general_action.setImageResource(R.drawable.dots);
                    linkHolder.general_action.setClickable(true);
                    linkHolder.general_action.setVisibility(View.VISIBLE);
                    linkHolder.general_action.setOnClickListener((v) ->
                            listener.invokeGeneralAction(note)
                    );
                } else {
                    linkHolder.general_action.setVisibility(View.GONE);
                }
            }
            switch (note.getStatus()) {
                case SEMI: {
                    if (!note.isExpired()) {
                        linkHolder.image_action.setVisibility(View.VISIBLE);
                        if (note.getKind() == Kind.OUT) {
                            linkHolder.image_action.setImageResource(R.drawable.sync_alert);
                        } else {
                            linkHolder.image_action.setImageResource(R.drawable.sync_download);
                        }
                        linkHolder.image_action.setClickable(true);
                        linkHolder.image_action.setOnClickListener((v) -> {

                            try {
                                if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                                    return;
                                }
                                mLastClickTime = SystemClock.elapsedRealtime();

                                listener.invokeLinkDownloadAction(note);

                            } catch (Throwable e) {
                                Log.e(TAG, "" + e.getLocalizedMessage(), e);
                            }
                        });
                    } else {
                        linkHolder.image_action.setVisibility(View.GONE);
                    }

                    break;
                }
                case ERROR: {
                    if (!note.isExpired()) {
                        linkHolder.image_action.setVisibility(View.VISIBLE);
                        linkHolder.image_action.setClickable(true);
                        if (note.getKind() == Kind.OUT) {
                            linkHolder.image_action.setImageResource(R.drawable.sync_alert);
                        } else {
                            linkHolder.image_action.setImageResource(R.drawable.sync_download);
                        }

                        linkHolder.image_action.setOnClickListener((v) -> {

                            if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                                return;
                            }
                            mLastClickTime = SystemClock.elapsedRealtime();

                            listener.invokeLinkActionError(note);
                        });
                    } else {
                        linkHolder.image_action.setVisibility(View.GONE);
                    }
                    break;
                }
                default: {
                    linkHolder.image_action.setVisibility(View.GONE);
                    break;
                }
            }
        }
    }

    public interface NotesViewAdapterListener {

        boolean generalActionSupport(@NonNull Note note);

        void invokeGeneralAction(@NonNull Note note);

        void invokeLinkDownloadAction(@NonNull Note note);

        void invokeAudioAction(@NonNull Note note);

        void invokeVideoAction(@NonNull Note note);

        void invokeLocationAction(@NonNull Note note);

        void invokeDataAction(@NonNull Note note);

        void invokeAudioActionError(@NonNull Note note);

        void invokeLinkActionError(@NonNull Note note);

        void invokeHtmlActionError(@NonNull Note note);

        void invokeDataActionError(@NonNull Note note);

        void invokeLocationActionError(@NonNull Note note);

        void invokeMessageActionError(@NonNull Note note);

        @NonNull
        String getTitle(@NonNull Note note);

        @NonNull
        String getContent(@NonNull Note note);

        boolean showHeader(@NonNull Note note);

        int getMediaResource(@NonNull Note note);

        boolean isEncrypted(@NonNull Note note);

        void invokeLinkImageAction(@NonNull Note note);

        void invokeLinkAction(@NonNull Note note);

        boolean isLinkActionSupported(@NonNull Note note);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        final TextView date;


        ViewHolder(View v) {
            super(v);
            date = v.findViewById(R.id.date);

        }

    }

    static class LinkHolder extends NotesViewAdapter.ViewHolder {
        final ImageView image_action;
        final TextView user;
        final TextView message_body;
        final ImageView general_action;
        final ImageView image_view;
        final LinearLayout header;
        final ProgressBar progress_bar;

        LinkHolder(View v) {
            super(v);

            header = v.findViewById(R.id.header);
            image_action = v.findViewById(R.id.image_action);
            general_action = v.findViewById(R.id.general_action);
            user = v.findViewById(R.id.user);
            message_body = v.findViewById(R.id.message_body);
            image_view = v.findViewById(R.id.image_view);
            progress_bar = v.findViewById(R.id.progress_bar);

        }


    }

    static class InfoHolder extends NotesViewAdapter.ViewHolder {
        final TextView message_body;

        InfoHolder(View v) {
            super(v);
            message_body = v.findViewById(R.id.message_body);
        }


    }

    static class MessageHolder extends NotesViewAdapter.ViewHolder {
        final ImageView image_action;
        final TextView user;
        final TextView message_body;
        final ImageView general_action;
        final LinearLayout header;
        final ProgressBar progress_bar;

        MessageHolder(View v) {
            super(v);

            header = v.findViewById(R.id.header);
            image_action = v.findViewById(R.id.image_action);
            message_body = v.findViewById(R.id.message_body);
            general_action = v.findViewById(R.id.general_action);
            user = v.findViewById(R.id.user);
            progress_bar = v.findViewById(R.id.progress_bar);

        }


    }

    static class HtmlHolder extends NotesViewAdapter.ViewHolder {

        final TextView message_body;
        final ImageView image_action;
        final ImageView image_main;

        HtmlHolder(View v) {
            super(v);

            image_main = v.findViewById(R.id.image_main);
            image_action = v.findViewById(R.id.image_action);
            message_body = v.findViewById(R.id.message_body);
        }

    }

    static class AudioHolder extends NotesViewAdapter.ViewHolder {
        final ImageView image_action;
        final ProgressBar progress_bar;
        final ImageView general_action;
        final TextView user;
        final TextView audio_title;
        final LinearLayout header;


        AudioHolder(View v) {
            super(v);
            header = v.findViewById(R.id.header);
            user = v.findViewById(R.id.user);
            image_action = v.findViewById(R.id.image_action);
            general_action = v.findViewById(R.id.general_action);
            progress_bar = v.findViewById(R.id.progress_bar);
            audio_title = v.findViewById(R.id.audio_title);

        }
    }

    static class DataHolder extends NotesViewAdapter.ViewHolder {
        final ImageView image_action;
        final ImageView general_action;
        final ProgressBar progress_bar;
        final ImageView image_view;
        final TextView user;
        final LinearLayout header;

        DataHolder(View v) {
            super(v);

            header = v.findViewById(R.id.header);
            image_view = v.findViewById(R.id.image_view);
            general_action = v.findViewById(R.id.general_action);
            image_action = v.findViewById(R.id.image_action);
            user = v.findViewById(R.id.user);
            progress_bar = v.findViewById(R.id.progress_bar);
        }
    }

    static class LocationHolder extends NotesViewAdapter.ViewHolder {
        final ImageView image_action;
        final ImageView general_action;
        final ProgressBar progress_bar;
        final ImageView image_view;
        final TextView user;
        final LinearLayout header;

        LocationHolder(View v) {
            super(v);
            header = v.findViewById(R.id.header);
            image_view = v.findViewById(R.id.image_view);
            general_action = v.findViewById(R.id.general_action);
            image_action = v.findViewById(R.id.image_action);
            user = v.findViewById(R.id.user);
            progress_bar = v.findViewById(R.id.progress_bar);
        }
    }

}
