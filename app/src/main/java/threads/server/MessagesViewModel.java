package threads.server;

import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;

import java.util.List;

public class MessagesViewModel extends AndroidViewModel {

    private final LiveData<List<Message>> messages;
    private final MessageDatabase messagesDatabase;

    public MessagesViewModel(android.app.Application application) {
        super(application);
        messagesDatabase = Application.getMessagesDatabase();

        messages = messagesDatabase.messageDao().getLiveDataMessages();
    }


    public LiveData<List<Message>> getMessages() {
        return messages;
    }
}