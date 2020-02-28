package threads.server.core.events;

import androidx.annotation.NonNull;


public class EventsAPI {


    private final EventsDatabase eventsDatabase;

    EventsAPI(@NonNull EventsDatabase eventsDatabase) {

        this.eventsDatabase = eventsDatabase;

    }


    @NonNull
    public EventsDatabase getEventsDatabase() {
        return eventsDatabase;
    }


    @NonNull
    Event createEvent(@NonNull String identifier, @NonNull String content) {

        return Event.createEvent(identifier, content);
    }

    public void invokeEvent(@NonNull String identifier, @NonNull String content) {

        storeEvent(createEvent(identifier, content));
    }

    void storeEvent(@NonNull Event event) {

        getEventsDatabase().eventDao().insertEvent(event);
    }


}
