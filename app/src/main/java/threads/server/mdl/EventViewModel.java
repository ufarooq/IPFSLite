package threads.server.mdl;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import threads.core.Preferences;
import threads.core.events.EVENTS;
import threads.core.events.Event;
import threads.core.events.EventsDatabase;

import static androidx.core.util.Preconditions.checkNotNull;

public class EventViewModel extends AndroidViewModel {

    private final EventsDatabase eventsDatabase;

    public EventViewModel(@NonNull Application application) {
        super(application);
        eventsDatabase = EVENTS.getInstance(
                application.getApplicationContext()).getEventsDatabase();
    }


    public LiveData<Event> getException() {
        return eventsDatabase.eventDao().getEvent(Preferences.EXCEPTION);
    }

    public LiveData<Event> getWarning() {
        return eventsDatabase.eventDao().getEvent(Preferences.WARNING);
    }


    public LiveData<Event> getInfo() {
        return eventsDatabase.eventDao().getEvent(Preferences.INFO);
    }

    public LiveData<Event> getEvent(@NonNull String event) {
        checkNotNull(event);
        return eventsDatabase.eventDao().getEvent(event);
    }


    public void removeEvent(@NonNull final Event event) {
        new Thread(() -> eventsDatabase.eventDao().deleteEvent(event)).start();
    }

}