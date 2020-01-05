package threads.core.api;

import androidx.room.RoomDatabase;

@androidx.room.Database(entities = {User.class, Thread.class}, version = 1, exportSchema = false)
public abstract class ThreadsDatabase extends RoomDatabase {


    public abstract UserDao userDao();

    public abstract ThreadDao threadDao();


    public void clear() {
        userDao().clear();
        threadDao().clear();
    }

}
