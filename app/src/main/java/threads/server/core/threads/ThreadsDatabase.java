package threads.server.core.threads;

import androidx.room.RoomDatabase;

@androidx.room.Database(entities = {Thread.class}, version = 105, exportSchema = false)
public abstract class ThreadsDatabase extends RoomDatabase {

    public abstract ThreadDao threadDao();

    public void clear() {
        threadDao().clear();
    }

}
