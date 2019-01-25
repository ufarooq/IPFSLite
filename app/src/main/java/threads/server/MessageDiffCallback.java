package threads.server;

import java.util.List;

import androidx.recyclerview.widget.DiffUtil;

public class MessageDiffCallback extends DiffUtil.Callback {
    private final List<Message> mOldList;
    private final List<Message> mNewList;

    MessageDiffCallback(List<Message> oldNotes, List<Message> newNotes) {
        this.mOldList = oldNotes;
        this.mNewList = newNotes;
    }

    @Override
    public int getOldListSize() {
        return mOldList.size();
    }

    @Override
    public int getNewListSize() {
        return mNewList.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return mOldList.get(oldItemPosition).areItemsTheSame(mNewList.get(
                newItemPosition));
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        return mOldList.get(oldItemPosition).sameContent(mNewList.get(newItemPosition));
    }


}


