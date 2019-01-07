package threads.server;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;

import threads.server.event.Event;
import threads.server.event.EventsDatabase;

public class EventViewModel extends AndroidViewModel {

    private final EventsDatabase eventsDatabase;

    public EventViewModel(@NonNull Application application) {
        super(application);
        eventsDatabase = threads.server.Application.getEventsDatabase();
    }


    LiveData<Event> getDaemonServerOnlineEvent() {
        return eventsDatabase.eventDao().getEvent(threads.server.Application.DAEMON_SERVER_ONLINE_EVENT);
    }

    LiveData<Event> getDaemonServerOfflineEvent() {
        return eventsDatabase.eventDao().getEvent(threads.server.Application.DAEMON_SERVER_OFFLINE_EVENT);
    }


}