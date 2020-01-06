package threads.core.events;

import androidx.annotation.NonNull;

import static androidx.core.util.Preconditions.checkNotNull;


public class EventsAPI {
    private final static String TAG = EventsAPI.class.getSimpleName();


    private final EventsDatabase eventsDatabase;

    public EventsAPI(@NonNull EventsDatabase eventsDatabase) {
        checkNotNull(eventsDatabase);

        this.eventsDatabase = eventsDatabase;

    }


    @NonNull
    private EventsDatabase getEventsDatabase() {
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


    public void invokeEvent(@NonNull String identifier, @NonNull String content) {
        checkNotNull(identifier);
        checkNotNull(content);
        storeEvent(createEvent(identifier, content));
    }

    public void storeEvent(@NonNull Event event) {
        checkNotNull(event);
        getEventsDatabase().eventDao().insertEvent(event);
    }


    public Message createMessage(@NonNull MessageKind messageKind, @NonNull String message, long timestamp) {
        checkNotNull(messageKind);
        checkNotNull(message);
        return Message.createMessage(messageKind, message, timestamp);
    }


    public void removeMessage(@NonNull Message message) {
        checkNotNull(message);
        getEventsDatabase().messageDao().deleteMessage(message);
    }


    public void storeMessage(@NonNull Message message) {
        checkNotNull(message);
        getEventsDatabase().messageDao().insertMessages(message);
    }


    public void clearMessages() {
        getEventsDatabase().messageDao().clear();
    }


}
