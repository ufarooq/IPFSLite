package threads.server.core.events;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


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

    public void removeEvent(@NonNull Event event) {

        getEventsDatabase().eventDao().deleteEvent(event);
    }

    public void removeEvent(@NonNull String identifier) {

        getEventsDatabase().eventDao().deleteEvent(identifier);
    }

    public void setContent(@NonNull String identifier, @NonNull String content) {

        getEventsDatabase().eventDao().setContent(identifier, content);
    }

    @Nullable
    public String getContent(@NonNull String identifier) {

        return getEventsDatabase().eventDao().getContent(identifier);
    }

    public void invokeEvent(@NonNull String identifier, @NonNull String content) {

        storeEvent(createEvent(identifier, content));
    }

    void storeEvent(@NonNull Event event) {

        getEventsDatabase().eventDao().insertEvent(event);
    }


}
