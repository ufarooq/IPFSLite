package threads.server;

import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;

import java.util.List;

public class MessagesViewModel extends AndroidViewModel {

    private final LiveData<List<Message>> messages;
    private final DaemonDatabase daemonDatabase;

    public MessagesViewModel(android.app.Application application) {
        super(application);
        daemonDatabase = Application.getDaemonDatabase();

        messages = daemonDatabase.messageDao().getLiveDataMessages();
    }


    public LiveData<List<Message>> getMessages() {
        return messages;
    }
}