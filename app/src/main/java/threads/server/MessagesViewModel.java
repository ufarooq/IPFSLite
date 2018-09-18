package threads.server;

import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;

import java.util.List;

import threads.iri.event.EventsDatabase;
import threads.iri.event.Message;

public class MessagesViewModel extends AndroidViewModel {

    private final LiveData<List<Message>> messages;
    private final EventsDatabase eventsDatabase;

    public MessagesViewModel(android.app.Application application) {
        super(application);
        eventsDatabase = Application.getEventsDatabase();

        messages = eventsDatabase.messageDao().getLiveDataMessages();
    }


    public LiveData<List<Message>> getMessages() {
        return messages;
    }
}