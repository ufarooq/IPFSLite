package threads.server.utils;

import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.selection.ItemDetailsLookup;
import androidx.recyclerview.widget.RecyclerView;

public class ThreadItemDetailsLookup extends ItemDetailsLookup<Long> {
    RecyclerView mRecyclerView;

    public ThreadItemDetailsLookup(RecyclerView recyclerView) {
        this.mRecyclerView = recyclerView;
    }

    @Nullable
    @Override
    public ItemDetails<Long> getItemDetails(@NonNull MotionEvent e) {
        View view = mRecyclerView.findChildViewUnder(e.getX(), e.getY());
        if (view != null) {
            RecyclerView.ViewHolder viewHolder = mRecyclerView.getChildViewHolder(view);
            if (viewHolder instanceof ThreadsViewAdapter.ThreadViewHolder) {
                return ((ThreadsViewAdapter.ThreadViewHolder) viewHolder).getThreadsItemDetails();
            }
        }
        return null;
    }
}
