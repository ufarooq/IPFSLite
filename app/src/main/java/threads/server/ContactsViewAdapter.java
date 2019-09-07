package threads.server;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.amulyakhare.textdrawable.TextDrawable;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import threads.core.Preferences;
import threads.core.Singleton;
import threads.core.api.User;
import threads.share.IPFSData;


public class ContactsViewAdapter extends RecyclerView.Adapter<ContactsViewAdapter.ViewHolder> {
    private static final String TAG = ContactsViewAdapter.class.getSimpleName();
    private final ValidateListener listener;
    private final List<Pair<User, AtomicBoolean>> accounts = new ArrayList<>();
    private final Context context;
    private final int timeout;

    public ContactsViewAdapter(@NonNull Context context, @NonNull ValidateListener listener) {
        this.context = context;
        this.listener = listener;
        this.timeout = Preferences.getConnectionTimeout(context);
    }


    public void setAccounts(@NonNull List<User> accounts) {
        this.accounts.clear();
        for (User account : accounts) {
            this.accounts.add(Pair.create(account, new AtomicBoolean(false)));

        }
        this.notifyDataSetChanged();
    }

    @NonNull
    public List<User> getSelectedAccounts() {
        List<User> selected = new ArrayList<>();

        for (Pair<User, AtomicBoolean> pair : accounts) {
            if (pair.second.get()) {
                selected.add(pair.first);
            }

        }
        return selected;
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    @NonNull
    public ContactsViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                                             int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.send_entry, parent, false);
        return new ViewHolder(v);

    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {


        final Pair<User, AtomicBoolean> pair = accounts.get(position);
        final AtomicBoolean selected = pair.second;
        final User account = pair.first;


        try {

            holder.account_name.setText(account.getAlias());


            if (account.getImage() != null) {
                Singleton singleton = Singleton.getInstance(context);
                IPFSData data = IPFSData.create(singleton.getIpfs(),
                        account.getImage(), timeout);
                Glide.with(context).load(data).into(holder.account_icon);
            } else {
                holder.account_icon.setVisibility(View.GONE);
            }

            holder.view.setClickable(true);

            holder.view.setOnClickListener((v) -> {

                if (selected.get()) {
                    v.setBackgroundColor(android.R.drawable.list_selector_background);

                    if (account.getImage() != null) {
                        Singleton singleton = Singleton.getInstance(context);
                        IPFSData data = IPFSData.create(singleton.getIpfs(),
                                account.getImage(), timeout);
                        Glide.with(context).load(data).into(holder.account_icon);
                    } else {
                        holder.account_icon.setVisibility(View.GONE);
                    }
                } else {
                    v.setBackgroundColor(Color.GRAY);
                    TextDrawable drawable = TextDrawable.builder()
                            .buildRound("\u2713", Color.DKGRAY);
                    holder.account_icon.setImageDrawable(drawable);
                }
                selected.set(!selected.get());

                listener.validate();

            });


        } catch (Throwable e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }


    }


    @Override
    public int getItemCount() {
        return accounts.size();
    }

    public interface ValidateListener {
        void validate();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        final View view;
        final TextView account_name;
        final ImageView account_icon;

        ViewHolder(View v) {
            super(v);

            view = v;
            account_name = itemView.findViewById(R.id.account_name);
            account_icon = itemView.findViewById(R.id.account_icon);
        }

    }
}
