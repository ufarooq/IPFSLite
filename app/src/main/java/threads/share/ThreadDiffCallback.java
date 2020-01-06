package threads.share;

import androidx.recyclerview.widget.DiffUtil;

import java.util.List;

import threads.core.threads.Thread;

public class ThreadDiffCallback extends DiffUtil.Callback {
    private final List<Thread> mOldList;
    private final List<Thread> mNewList;

    public ThreadDiffCallback(List<Thread> messages, List<Thread> messageThreads) {
        this.mOldList = messages;
        this.mNewList = messageThreads;
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
