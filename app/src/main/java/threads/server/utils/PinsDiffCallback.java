package threads.server.utils;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import java.util.List;
import java.util.Objects;

import threads.server.core.threads.Thread;

import static androidx.core.util.Preconditions.checkNotNull;

public class PinsDiffCallback extends DiffUtil.Callback {
    private final List<Thread> mOldList;
    private final List<Thread> mNewList;

    public PinsDiffCallback(List<Thread> messages, List<Thread> messageThreads) {
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
        return sameContent(mOldList.get(oldItemPosition), mNewList.get(newItemPosition));
    }

    private boolean sameContent(@NonNull Thread t, @NonNull Thread o) {
        checkNotNull(t);
        checkNotNull(o);
        if (t == o) return true;
        return t.isPinned() == o.isPinned() &&
                t.isDeleting() == o.isDeleting() &&
                Objects.equals(t.getStatus(), o.getStatus()) &&
                Objects.equals(t.getLastModified(), o.getLastModified());
    }
}
