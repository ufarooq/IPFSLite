package threads.server.model;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import threads.server.core.events.EVENTS;
import threads.server.core.events.Event;
import threads.server.core.events.EventsDatabase;

public class EventViewModel extends AndroidViewModel {

    private final EventsDatabase eventsDatabase;

    public EventViewModel(@NonNull Application application) {
        super(application);
        eventsDatabase = EVENTS.getInstance(
                application.getApplicationContext()).getEventsDatabase();
    }

    public LiveData<Event> getException() {
        return eventsDatabase.eventDao().getEvent(EVENTS.ERROR);
    }

    public LiveData<Event> getPermission() {
        return eventsDatabase.eventDao().getEvent(EVENTS.PERMISSION);
    }

    public LiveData<Event> getWarning() {
        return eventsDatabase.eventDao().getEvent(EVENTS.WARNING);
    }

    public LiveData<Event> getInfo() {
        return eventsDatabase.eventDao().getEvent(EVENTS.INFO);
    }


    public void removeEvent(@NonNull final Event event) {
        new Thread(() -> eventsDatabase.eventDao().deleteEvent(event)).start();
    }

}