package threads.server.utils;

import androidx.recyclerview.widget.DiffUtil;

import java.util.List;

import threads.server.core.peers.Peer;

public class PeerDiffCallback extends DiffUtil.Callback {
    private final List<Peer> mOldList;
    private final List<Peer> mNewList;

    public PeerDiffCallback(List<Peer> oldUsers, List<Peer> newUsers) {
        this.mOldList = oldUsers;
        this.mNewList = newUsers;
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
