package threads.core.events;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static androidx.core.util.Preconditions.checkNotNull;


public class EventsAPI {


    private final EventsDatabase eventsDatabase;

    public EventsAPI(@NonNull EventsDatabase eventsDatabase) {
        checkNotNull(eventsDatabase);

        this.eventsDatabase = eventsDatabase;

    }


    @NonNull
    public EventsDatabase getEventsDatabase() {
        return eventsDatabase;
    }


    @NonNull
    public Event createEvent(@NonNull String identifier, @NonNull String content) {
        checkNotNull(identifier);
        checkNotNull(content);
        return Event.createEvent(identifier, content);
    }

    public void removeEvent(@NonNull Event event) {
        checkNotNull(event);
        getEventsDatabase().eventDao().deleteEvent(event);
    }

    public void removeEvent(@NonNull String identifier) {
        checkNotNull(identifier);
        getEventsDatabase().eventDao().deleteEvent(identifier);
    }

    public void setContent(@NonNull String identifier, @NonNull String content) {
        checkNotNull(identifier);
        checkNotNull(content);
        getEventsDatabase().eventDao().setContent(identifier, content);
    }

    @Nullable
    public String getContent(@NonNull String identifier) {
        checkNotNull(identifier);
        return getEventsDatabase().eventDao().getContent(identifier);
    }

    public void invokeEvent(@NonNull String identifier, @NonNull String content) {
        checkNotNull(identifier);
        checkNotNull(content);
        storeEvent(createEvent(identifier, content));
    }

    public void storeEvent(@NonNull Event event) {
        checkNotNull(event);
        getEventsDatabase().eventDao().insertEvent(event);
    }


}
