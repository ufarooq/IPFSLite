package threads.share;

import android.content.Context;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import threads.core.Preferences;
import threads.core.Singleton;
import threads.core.peers.IPeer;
import threads.core.peers.Peer;
import threads.ipfs.IPFS;
import threads.server.R;

public class PeersViewAdapter extends RecyclerView.Adapter<PeersViewAdapter.ViewHolder> {

    private static final String TAG = PeersViewAdapter.class.getSimpleName();
    private final List<Peer> peers = new ArrayList<>();
    private final Context context;
    private final PeersViewAdapterListener listener;
    private final int timeout;

    public PeersViewAdapter(@NonNull Context context,
                            @NonNull PeersViewAdapterListener listener) {

        timeout = Preferences.getConnectionTimeout(context);
        this.context = context;
        this.listener = listener;
    }

    private static int getThemeTextColor(final Context context) {
        final TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.textColorSecondary, value, true);
        return value.data;
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

                if (listener.generalActionSupport(peer)) {
                    peerViewHolder.user_action.setVisibility(View.VISIBLE);
                    peerViewHolder.user_action.setOnClickListener((v) ->
                            listener.invokeGeneralAction(peer)
                    );
                } else {
                    peerViewHolder.user_action.setVisibility(View.GONE);
                }


                if (peer.isRelay()) {
                    peerViewHolder.user_alias.setCompoundDrawablesRelativeWithIntrinsicBounds(
                            R.drawable.access_point, 0, 0, 0);
                } else if (peer.isPubsub()) {
                    peerViewHolder.user_alias.setCompoundDrawablesRelativeWithIntrinsicBounds(
                            R.drawable.hubspot, 0, 0, 0);
                } else {
                    peerViewHolder.user_alias.setCompoundDrawablesRelativeWithIntrinsicBounds(
                            R.drawable.network_storage, 0, 0, 0);
                }


                if (peer.getImage() != null) {
                    peerViewHolder.user_image.setVisibility(View.VISIBLE);
                    IPFS ipfs = Singleton.getInstance(context).getIpfs();
                    IPFSData data = IPFSData.create(ipfs, peer.getImage(), timeout);
                    Glide.with(context).load(data).into(peerViewHolder.user_image);
                } else {
                    peerViewHolder.user_image.setVisibility(View.GONE);
                }

                peerViewHolder.user_alias.setText(peer.getAlias());

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

        void invokeGeneralAction(@NonNull IPeer peer);

        boolean generalActionSupport(@NonNull IPeer peer);


    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        final View view;

        ViewHolder(View v) {
            super(v);
            view = v;

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
