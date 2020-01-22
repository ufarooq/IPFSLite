package threads.server.utils;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import threads.ipfs.IPFS;
import threads.server.R;
import threads.server.core.peers.User;

public class UsersViewAdapter extends RecyclerView.Adapter<UsersViewAdapter.ViewHolder> {

    private static final String TAG = UsersViewAdapter.class.getSimpleName();
    private final List<User> users = new ArrayList<>();
    private final Context context;
    private final UsersViewAdapterListener listener;
    private final int timeout;

    public UsersViewAdapter(@NonNull Context context,
                            @NonNull UsersViewAdapter.UsersViewAdapterListener listener) {

        timeout = Preferences.getConnectionTimeout(context);
        this.context = context;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    @NonNull
    public UsersViewAdapter.UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                                              int viewType) {

        View v;
        switch (viewType) {
            case 0:
                v = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.users, parent, false);
                return new UsersViewAdapter.UserViewHolder(v);
        }
        throw new RuntimeException("View type not supported.");
    }


    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        final User user = users.get(position);

        if (holder instanceof UsersViewAdapter.UserViewHolder) {
            UsersViewAdapter.UserViewHolder userViewHolder = (UsersViewAdapter.UserViewHolder) holder;

            try {

                if (listener.generalActionSupport(user)) {
                    userViewHolder.user_action.setVisibility(View.VISIBLE);
                    userViewHolder.user_action.setOnClickListener((v) ->
                            listener.invokeGeneralAction(user)
                    );
                } else {
                    userViewHolder.user_action.setVisibility(View.GONE);
                }

                int end = 0;
                if (!user.isValid()) {
                    end = R.drawable.exclamation;
                } else if (user.isAutoConnect()) {
                    end = R.drawable.text_rocket;
                }

                if (user.isBlocked()) {
                    userViewHolder.progress_bar.setVisibility(View.GONE);
                    userViewHolder.user_alias.setCompoundDrawablesRelativeWithIntrinsicBounds(
                            R.drawable.blocked, 0, end, 0);
                } else if (user.isDialing()) {
                    userViewHolder.progress_bar.setVisibility(View.VISIBLE);
                    userViewHolder.user_alias.setCompoundDrawablesRelativeWithIntrinsicBounds(
                            R.drawable.text_lan_pending, 0, end, 0);
                } else if (user.isConnected()) {
                    userViewHolder.progress_bar.setVisibility(View.GONE);
                    userViewHolder.user_alias.setCompoundDrawablesRelativeWithIntrinsicBounds(
                            R.drawable.green_bubble, 0, end, 0);
                } else {
                    userViewHolder.progress_bar.setVisibility(View.GONE);
                    userViewHolder.user_alias.setCompoundDrawablesRelativeWithIntrinsicBounds(
                            R.drawable.record, 0, end, 0);
                }


                if (user.getImage() != null) {
                    userViewHolder.user_image.setVisibility(View.VISIBLE);
                    IPFS ipfs = IPFS.getInstance(context);
                    IPFSData data = IPFSData.create(ipfs, user.getImage(), timeout);
                    Glide.with(context).load(data).into(userViewHolder.user_image);
                } else {
                    userViewHolder.user_image.setVisibility(View.GONE);
                }

                userViewHolder.user_alias.setText(user.getAlias());

            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }

        }

    }


    @Override
    public int getItemCount() {
        return users.size();
    }


    public void updateData(@NonNull List<User> users) {

        final UserDiffCallback diffCallback = new UserDiffCallback(this.users, users);
        final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);

        this.users.clear();
        this.users.addAll(users);
        diffResult.dispatchUpdatesTo(this);
    }


    public interface UsersViewAdapterListener {

        void invokeGeneralAction(@NonNull User user);

        boolean generalActionSupport(@NonNull User user);


    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        ViewHolder(View v) {
            super(v);
        }
    }


    static class UserViewHolder extends ViewHolder {
        // each data item is just a string in this case
        final TextView user_alias;
        final ImageView user_action;
        final ImageView user_image;
        final ProgressBar progress_bar;

        UserViewHolder(View v) {
            super(v);
            user_image = v.findViewById(R.id.user_image);
            user_alias = v.findViewById(R.id.user_alias);
            user_action = v.findViewById(R.id.user_action);
            progress_bar = v.findViewById(R.id.progress_bar);

        }
    }
}
