package threads.server.core.events;

import androidx.room.RoomDatabase;

@androidx.room.Database(entities = {Event.class}, version = 100, exportSchema = false)
public abstract class EventsDatabase extends RoomDatabase {

    public abstract EventDao eventDao();

}
