package threads.server.event;

import android.arch.persistence.room.RoomDatabase;
import android.support.annotation.NonNull;

import static com.google.common.base.Preconditions.checkNotNull;

@android.arch.persistence.room.Database(entities = {Event.class, Message.class}, version = 3, exportSchema = false)
public abstract class EventsDatabase extends RoomDatabase {
    private static final String TAG = EventsDatabase.class.getSimpleName();

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

    public void insertEvent(@NonNull IEvent event) {
        checkNotNull(event);
        eventDao().insertEvent((Event) event);
    }


    public IEvent createEvent(@NonNull String identifier) {
        checkNotNull(identifier);
        return Event.createEvent(identifier);
    }


    public void deleteEvent(@NonNull IEvent event) {
        checkNotNull(event);
        eventDao().deleteEvent((Event) event);
    }
}
