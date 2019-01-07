package threads.server;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;

import threads.server.event.Event;
import threads.server.event.EventsDatabase;

class EventViewModel extends AndroidViewModel {

    private final EventsDatabase eventsDatabase;

    public EventViewModel(@NonNull Application application) {
        super(application);
        eventsDatabase = threads.server.Application.getEventsDatabase();
    }


    LiveData<Event> getDaemonServerOnlineEvent() {
        return eventsDatabase.eventDao().getEvent(threads.server.Application.SERVER_ONLINE_EVENT);
    }

    LiveData<Event> getDaemonServerOfflineEvent() {
        return eventsDatabase.eventDao().getEvent(threads.server.Application.SERVER_OFFLINE_EVENT);
    }


}