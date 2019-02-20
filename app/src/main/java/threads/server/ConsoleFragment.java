package threads.server;

import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;

import org.apache.tools.ant.types.Commandline;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import threads.core.Singleton;
import threads.core.api.Message;
import threads.core.mdl.MessagesViewModel;
import threads.ipfs.IPFS;
import threads.share.MessageViewAdapter;

public class ConsoleFragment extends Fragment {
    private static final String TAG = ConsoleFragment.class.getSimpleName();
    private RecyclerView mRecyclerView;
    private MessageViewAdapter messageViewAdapter;
    private EditText console_box;

    private long mLastClickTime = 0;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.messages_view, container, false);


        mRecyclerView = view.findViewById(R.id.view_message_list);

        LinearLayoutManager linearLayout = new LinearLayoutManager(getContext());
        mRecyclerView.addOnLayoutChangeListener((View v,
                                                 int left, int top, int right, int bottom,
                                                 int oldLeft, int oldTop,
                                                 int oldRight, int oldBottom) -> {

            if (bottom < oldBottom) {
                mRecyclerView.postDelayed(() -> {

                    try {
                        RecyclerView.Adapter adapter = mRecyclerView.getAdapter();
                        if (adapter != null) {
                            mRecyclerView.smoothScrollToPosition(
                                    adapter.getItemCount());
                        }
                    } catch (Throwable e) {
                        Log.e(TAG, "" + e.getLocalizedMessage(), e);
                    }

                }, 50);
            }

        });


        mRecyclerView.setLayoutManager(linearLayout);
        messageViewAdapter = new MessageViewAdapter();
        mRecyclerView.setAdapter(messageViewAdapter);

        ImageView console_send = view.findViewById(R.id.console_send);

        console_box = view.findViewById(R.id.console_box);

        console_send.setOnClickListener((v) -> {

            // mis-clicking prevention, using threshold of 1500 ms
            if (SystemClock.elapsedRealtime() - mLastClickTime < 1500) {
                return;
            }
            mLastClickTime = SystemClock.elapsedRealtime();

            removeKeyboards();

            String text = console_box.getText().toString();

            if (!text.isEmpty()) {
                // Hack to mack sure that last index is line separator
                if (text.contains("pubsub pub") && !text.endsWith(System.lineSeparator())) {
                    text = text.concat(System.lineSeparator());
                }

                console_box.setText("");

                String[] parts = Commandline.translateCommandline(text);
                if (parts.length > 0) {


                    if (parts[0].equalsIgnoreCase("ipfs")) {
                        String[] commands = Arrays.copyOfRange(parts, 1, parts.length);
                        ExecutorService executor = Executors.newSingleThreadExecutor();
                        executor.submit(() -> {
                            try {
                                IPFS ipfs = Singleton.getInstance().getIpfs();

                                if (ipfs != null) {
                                    ipfs.cmd(commands);
                                }

                            } catch (Throwable e) {
                                Log.e(TAG, "" + e.getLocalizedMessage(), e);
                            }
                        });
                    } else {
                        ExecutorService executor = Executors.newSingleThreadExecutor();
                        executor.submit(() -> {
                            try {
                                IPFS ipfs = Singleton.getInstance().getIpfs();

                                if (ipfs != null) {
                                    ipfs.cmd(parts);
                                }

                            } catch (Throwable e) {
                                Log.e(TAG, "" + e.getLocalizedMessage(), e);
                            }
                        });
                    }
                }
            }

        });


        MessagesViewModel messagesViewModel = ViewModelProviders.of(this).get(MessagesViewModel.class);
        messagesViewModel.getMessages().observe(this, (messages) -> {

            try {
                if (messages != null) {
                    updateMessages(messages);
                }
            } catch (Throwable e) {
                Log.e(TAG, "" + e.getLocalizedMessage());
            }

        });

        return view;
    }


    private void removeKeyboards() {
        if (getActivity() != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(console_box.getWindowToken(), 0);
            }
        }
    }

    private void updateMessages(@NonNull List<Message> messages) {
        try {
            messageViewAdapter.updateData(messages);

            mRecyclerView.scrollToPosition(messageViewAdapter.getItemCount() - 1);
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }

    }

}
