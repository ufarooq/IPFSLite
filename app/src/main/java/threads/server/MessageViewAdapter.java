package threads.server;

import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import threads.iota.event.Message;

public class MessageViewAdapter extends RecyclerView.Adapter<MessageViewAdapter.ViewHolder> {
    private static final String TAG = "MessageViewAdapter";
    private final List<Message> messages = new ArrayList<>();


    @Override
    public int getItemViewType(int position) {
        return 0;
    }


    @Override
    public MessageViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                            int viewType) {

        View v = null;
        switch (viewType) {
            case 0:
                v = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.message_item, parent, false);
                return new MessageViewHolder(v);
        }
        return null;
    }


    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        final Message message = messages.get(position);

        if (holder instanceof MessageViewHolder) {
            MessageViewHolder messageViewHolder = (MessageViewHolder) holder;

            try {

                messageViewHolder.view.setBackgroundColor(Color.WHITE);

                messageViewHolder.text.setText(message.getMessage());

            } catch (Exception e) {
                Log.e(TAG, e.getLocalizedMessage());
            }


        }

    }


    @Override
    public int getItemCount() {

        return messages.size();
    }

    public Message getItemAtPosition(int position) {
        try {
            return messages.get(position);
        } catch (Throwable e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
        return null;
    }


    public void updateData(@NonNull List<Message> messageThreads) {

        final MessageDiffCallback diffCallback = new MessageDiffCallback(this.messages, messageThreads);
        final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);

        this.messages.clear();
        this.messages.addAll(messageThreads);
        diffResult.dispatchUpdatesTo(this);


    }


    private int getPositionOfItem(@NonNull Message message) {
        try {
            return messages.indexOf(message);
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage());
        }
        return -1;
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        final View view;

        ViewHolder(View v) {
            super(v);
            view = v;

        }
    }


    static class MessageViewHolder extends ViewHolder {
        // each data item is just a string in this case
        final TextView text;

        MessageViewHolder(View v) {
            super(v);
            text = itemView.findViewById(R.id.text);
        }
    }

}
