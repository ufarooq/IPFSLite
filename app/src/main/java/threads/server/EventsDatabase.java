package threads.server;

import android.arch.persistence.room.RoomDatabase;
import android.support.annotation.NonNull;

import static com.google.common.base.Preconditions.checkNotNull;

@android.arch.persistence.room.Database(entities = {Event.class, Message.class}, version = 4, exportSchema = false)
public abstract class EventsDatabase extends RoomDatabase {

    public abstract EventDao eventDao();

    public abstract MessageDao messageDao();


    public void insertMessage(@NonNull String message) {
        checkNotNull(message);
        messageDao().insertMessages(Message.createMessage(message));
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
