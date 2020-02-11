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
import androidx.annotation.Nullable;
import androidx.recyclerview.selection.ItemDetailsLookup;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.amulyakhare.textdrawable.util.ColorGenerator;

import java.util.ArrayList;
import java.util.List;

import threads.server.R;
import threads.server.core.peers.User;

public class UsersViewAdapter extends
        RecyclerView.Adapter<UsersViewAdapter.ViewHolder> implements UserItemPosition {

    private static final String TAG = UsersViewAdapter.class.getSimpleName();
    private final List<User> users = new ArrayList<>();
    private final Context context;
    private final UsersViewAdapterListener listener;

    @Nullable
    private SelectionTracker<String> mSelectionTracker;

    public UsersViewAdapter(@NonNull Context context,
                            @NonNull UsersViewAdapter.UsersViewAdapterListener listener) {

        this.context = context;
        this.listener = listener;
    }


    public void setSelectionTracker(SelectionTracker<String> selectionTracker) {
        this.mSelectionTracker = selectionTracker;
    }

    private boolean hasSelection() {
        if (mSelectionTracker != null) {
            return mSelectionTracker.hasSelection();
        }
        return false;
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    @NonNull
    public UsersViewAdapter.UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                                              int viewType) {

        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.users, parent, false);
        return new UsersViewAdapter.UserViewHolder(this, v);

    }


    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        final User user = users.get(position);

        if (holder instanceof UsersViewAdapter.UserViewHolder) {
            UsersViewAdapter.UserViewHolder userViewHolder = (UsersViewAdapter.UserViewHolder) holder;

            boolean isSelected = false;
            if (mSelectionTracker != null) {
                if (mSelectionTracker.isSelected(user.getPid())) {
                    isSelected = true;
                }
            }

            userViewHolder.bind(isSelected, user);


            try {

                if (hasSelection()) {
                    if (isSelected) {
                        userViewHolder.user_action.setVisibility(View.VISIBLE);
                        userViewHolder.user_action.setImageResource(R.drawable.check_circle_outline);
                    } else {
                        userViewHolder.user_action.setVisibility(View.VISIBLE);
                        userViewHolder.user_action.setImageResource(R.drawable.checkbox_blank_circle_outline);
                    }
                    userViewHolder.progress_bar.setVisibility(View.INVISIBLE);
                } else {
                    if (user.isDialing()) {
                        userViewHolder.user_action.setImageResource(R.drawable.pause);
                        userViewHolder.user_action.setVisibility(View.VISIBLE);
                        userViewHolder.user_action.setOnClickListener((v) ->
                                listener.invokeAbortDialing(user)
                        );
                    } else {
                        userViewHolder.user_action.setImageResource(R.drawable.dots);
                        userViewHolder.user_action.setVisibility(View.VISIBLE);
                        userViewHolder.user_action.setOnClickListener((v) ->
                                listener.invokeGeneralAction(user, v)
                        );
                    }
                }

                if (user.isBlocked()) {
                    userViewHolder.progress_bar.setVisibility(View.INVISIBLE);
                    userViewHolder.user_alias.setCompoundDrawablesRelativeWithIntrinsicBounds(
                            R.drawable.blocked, 0, 0, 0);
                } else if (user.isDialing()) {
                    userViewHolder.progress_bar.setVisibility(View.VISIBLE);
                    userViewHolder.user_alias.setCompoundDrawablesRelativeWithIntrinsicBounds(
                            R.drawable.text_lan_pending, 0, 0, 0);
                } else if (user.isConnected()) {
                    userViewHolder.progress_bar.setVisibility(View.INVISIBLE);
                    userViewHolder.user_alias.setCompoundDrawablesRelativeWithIntrinsicBounds(
                            R.drawable.green_bubble, 0, 0, 0);
                } else {
                    userViewHolder.progress_bar.setVisibility(View.INVISIBLE);
                    userViewHolder.user_alias.setCompoundDrawablesRelativeWithIntrinsicBounds(
                            R.drawable.record, 0, 0, 0);
                }

                int res = R.drawable.server_network;
                if (user.isLite()) {
                    res = R.drawable.account;
                }
                String name = user.getAlias();
                int color = ColorGenerator.MATERIAL.getColor(name);
                userViewHolder.user_image.setImageResource(res);
                userViewHolder.user_image.setColorFilter(color);

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

    @Override
    public synchronized int getPosition(String pid) {
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getPid().equals(pid)) {
                return i;
            }
        }
        return 0;
    }

    String getPid(int position) {
        return users.get(position).getPid();
    }

    public void selectAllUsers() {
        try {
            for (User user : users) {
                if (mSelectionTracker != null) {
                    mSelectionTracker.select(user.getPid());
                }
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }


    public interface UsersViewAdapterListener {

        void invokeGeneralAction(@NonNull User user, @NonNull View view);

        void invokeAbortDialing(@NonNull User user);
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
        final UserItemDetails userItemDetails;


        UserViewHolder(UserItemPosition pos, View v) {
            super(v);
            user_image = v.findViewById(R.id.user_image);
            user_alias = v.findViewById(R.id.user_alias);
            user_action = v.findViewById(R.id.user_action);
            progress_bar = v.findViewById(R.id.progress_bar);
            userItemDetails = new UserItemDetails(pos);
        }

        void bind(boolean isSelected, User user) {

            userItemDetails.pid = user.getPid();

            itemView.setActivated(isSelected);


        }

        ItemDetailsLookup.ItemDetails<String> getUserItemDetails() {

            return userItemDetails;
        }
    }
}
