package threads.server.utils;

import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.selection.ItemDetailsLookup;

public class ThreadItemDetails extends ItemDetailsLookup.ItemDetails<Long> {
    int position;
    long idx;

    @Override
    public int getPosition() {
        return position;
    }

    @Nullable
    @Override
    public Long getSelectionKey() {
        return idx;
    }

    @Override
    public boolean inSelectionHotspot(@NonNull MotionEvent e) {
        return false;//don't consider taps as selections => Similar to google photos.
        // if true then consider click as selection
    }

    @Override
    public boolean inDragRegion(@NonNull MotionEvent e) {
        return true;
    }
}
