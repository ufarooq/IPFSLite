package threads.server;

import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;

import java.util.List;

import threads.server.event.EventsDatabase;
import threads.server.event.Message;

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