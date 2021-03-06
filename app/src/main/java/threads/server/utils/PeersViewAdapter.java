package threads.server.utils;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.amulyakhare.textdrawable.util.ColorGenerator;

import java.util.ArrayList;
import java.util.List;

import threads.server.R;
import threads.server.core.peers.Peer;

public class PeersViewAdapter extends RecyclerView.Adapter<PeersViewAdapter.ViewHolder> {

    private static final String TAG = PeersViewAdapter.class.getSimpleName();
    private final List<Peer> peers = new ArrayList<>();

    private final PeersViewAdapterListener listener;


    public PeersViewAdapter(@NonNull PeersViewAdapterListener listener) {
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    @NonNull
    public PeerViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                             int viewType) {

        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.peers, parent, false);
        return new PeerViewHolder(v);

    }


    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        final Peer peer = peers.get(position);

        if (holder instanceof PeerViewHolder) {
            PeerViewHolder peerViewHolder = (PeerViewHolder) holder;

            try {


                peerViewHolder.user_action.setVisibility(View.VISIBLE);
                peerViewHolder.user_action.setOnClickListener((v) ->
                        listener.invokeAction(peer, v)
                );

                int res = R.drawable.server_network;

                String name = peer.getPid();
                int color = ColorGenerator.MATERIAL.getColor(name);
                peerViewHolder.user_image.setImageResource(res);
                peerViewHolder.user_image.setColorFilter(color);


                peerViewHolder.user_alias.setText(name);

            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }

        }

    }


    @Override
    public int getItemCount() {
        return peers.size();
    }


    public void updateData(@NonNull List<Peer> peers) {

        final PeerDiffCallback diffCallback = new PeerDiffCallback(this.peers, peers);
        final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);

        this.peers.clear();
        this.peers.addAll(peers);
        diffResult.dispatchUpdatesTo(this);
    }


    public interface PeersViewAdapterListener {
        void invokeAction(@NonNull Peer peer, @NonNull View view);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        ViewHolder(View v) {
            super(v);
        }
    }


    static class PeerViewHolder extends ViewHolder {
        // each data item is just a string in this case
        final TextView user_alias;
        final ImageView user_action;
        final ImageView user_image;

        PeerViewHolder(View v) {
            super(v);
            user_image = v.findViewById(R.id.user_image);
            user_alias = v.findViewById(R.id.user_alias);
            user_action = v.findViewById(R.id.user_action);

        }
    }
}
