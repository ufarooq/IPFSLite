package threads.share;

import androidx.recyclerview.widget.DiffUtil;

import java.util.List;

import threads.core.api.Note;


public class NoteDiffCallback extends DiffUtil.Callback {
    private final List<Note> mOldList;
    private final List<Note> mNewList;

    public NoteDiffCallback(List<Note> oldNotes, List<Note> newNotes) {
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


