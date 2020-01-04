package threads.core.mdl;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

import threads.core.Singleton;
import threads.core.api.EventsDatabase;
import threads.core.api.Message;

public class MessagesViewModel extends AndroidViewModel {

    private final LiveData<List<Message>> messages;

    public MessagesViewModel(Application application) {
        super(application);
        EventsDatabase eventsDatabase = Singleton.getInstance(
                application.getApplicationContext()).getEventsDatabase();

        messages = eventsDatabase.messageDao().getLiveDataMessages();
    }


    public LiveData<List<Message>> getMessages() {
        return messages;
    }
}