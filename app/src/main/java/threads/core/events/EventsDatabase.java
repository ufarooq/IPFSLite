package threads.core.events;

import androidx.room.RoomDatabase;

@androidx.room.Database(entities = {Event.class, Message.class}, version = 1, exportSchema = false)
public abstract class EventsDatabase extends RoomDatabase {

    public abstract EventDao eventDao();

    public abstract MessageDao messageDao();
}
