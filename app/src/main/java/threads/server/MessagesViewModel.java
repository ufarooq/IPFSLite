package threads.server;

import java.util.List;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

public class MessagesViewModel extends AndroidViewModel {

    private final LiveData<List<Message>> messages;

    public MessagesViewModel(android.app.Application application) {
        super(application);
        EventsDatabase eventsDatabase = Application.getEventsDatabase();

        messages = eventsDatabase.messageDao().getLiveDataMessages();
    }


    LiveData<List<Message>> getMessages() {
        return messages;
    }
}