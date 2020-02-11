package threads.server.core.contents;

import androidx.room.RoomDatabase;


@androidx.room.Database(entities = {Content.class}, version = 105, exportSchema = false)
public abstract class ContentDatabase extends RoomDatabase {

    public abstract ContentDao contentDao();

}
