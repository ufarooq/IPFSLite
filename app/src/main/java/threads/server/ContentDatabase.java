package threads.server;

import androidx.room.RoomDatabase;


@androidx.room.Database(entities = {Content.class}, version = 1, exportSchema = false)
public abstract class ContentDatabase extends RoomDatabase {

    public abstract ContentDao contentDao();

    public void clear() {
        contentDao().clear();
    }
}
