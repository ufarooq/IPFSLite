package threads.server;

import androidx.annotation.NonNull;
import androidx.room.RoomDatabase;

import static com.google.common.base.Preconditions.checkNotNull;

@androidx.room.Database(entities = {Event.class, Message.class}, version = 4, exportSchema = false)
public abstract class EventsDatabase extends RoomDatabase {

    public abstract EventDao eventDao();

    public abstract MessageDao messageDao();


    public void insertMessage(@NonNull MessageKind messageKind, @NonNull String message, long timestamp) {
        checkNotNull(messageKind);
        checkNotNull(message);
        messageDao().insertMessages(Message.createMessage(messageKind, message, timestamp));
    }

    public void clear() {
        eventDao().clear();
        messageDao().clear();
    }

    public void insertEvent(@NonNull Event event) {
        checkNotNull(event);
        eventDao().insertEvent(event);
    }


    public Event createEvent(@NonNull String identifier) {
        checkNotNull(identifier);
        return Event.createEvent(identifier);
    }


    public void deleteEvent(@NonNull Event event) {
        checkNotNull(event);
        eventDao().deleteEvent(event);
    }
}
