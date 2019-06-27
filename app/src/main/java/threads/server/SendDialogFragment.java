package threads.server;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import threads.core.Singleton;
import threads.core.THREADS;
import threads.core.api.User;
import threads.ipfs.api.PID;

import static androidx.core.util.Preconditions.checkNotNull;

public class SendDialogFragment extends DialogFragment implements ContactsViewAdapter.ValidateListener {
    public static final String TAG = SendDialogFragment.class.getSimpleName();
    public static final String IDXS = "IDXS";
    public static final String PIDS = "PIDS";
    private long mLastClickTime = 0;
    private ContactsViewAdapter contactsViewAdapter;
    private Context mContext;

    @Override
    public void onDetach() {
        super.onDetach();
        mContext = null;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        Activity activity = getActivity();
        checkNotNull(activity);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        LayoutInflater inflater = activity.getLayoutInflater();

        Bundle args = getArguments();
        checkNotNull(args);
        final long[] idxs = args.getLongArray(IDXS);
        final ArrayList<String> pids = args.getStringArrayList(PIDS);

        @SuppressWarnings("all")
        View view = inflater.inflate(R.layout.send_view, null);


        RecyclerView recycler_view_contact_list = view.findViewById(R.id.send_contact_list);
        recycler_view_contact_list.setLayoutManager(new LinearLayoutManager(getContext()));
        contactsViewAdapter = new ContactsViewAdapter(getActivity(), this);
        recycler_view_contact_list.setAdapter(contactsViewAdapter);

        final THREADS threads = Singleton.getInstance(mContext).getThreads();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                List<User> users = new ArrayList<>();
                for (String pid : pids) {
                    User user = threads.getUserByPID(PID.create(pid));
                    if (user != null) {
                        users.add(user);
                    }
                }
                contactsViewAdapter.setAccounts(users);
            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage(), e);
            }
        });


        builder.setView(view)
                // Add action buttons
                .setPositiveButton(R.string.send_to, (dialog, id) -> {

                    // mis-clicking prevention, using threshold of 1000 ms
                    if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                        return;
                    }

                    mLastClickTime = SystemClock.elapsedRealtime();


                    List<User> users = contactsViewAdapter.getSelectedAccounts();

                    Service.getInstance(mContext).sendThreads(mContext, users, idxs);


                    dismiss();


                })
                .setNegativeButton(R.string.cancel, (dialog, id) -> {

                    // mis-clicking prevention, using threshold of 1000 ms
                    if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                        return;
                    }

                    mLastClickTime = SystemClock.elapsedRealtime();

                    dismiss();

                });


        Dialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);

        return dialog;
    }

    private void isValidInput(Dialog dialog) {

        if (dialog instanceof AlertDialog) {
            AlertDialog alertDialog = (AlertDialog) dialog;


            boolean result = !contactsViewAdapter.getSelectedAccounts().isEmpty();

            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(result);

        }
    }


    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {

        super.onDismiss(dialog);
    }

    @Override
    public void onResume() {
        super.onResume();
        isValidInput(getDialog());
    }


    @Override
    public void validate() {
        isValidInput(getDialog());
    }


}
