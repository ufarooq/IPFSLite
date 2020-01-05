package threads.core.api;

import androidx.room.RoomDatabase;

@androidx.room.Database(entities = {User.class,
        Thread.class, Settings.class}, version = 1, exportSchema = false)
public abstract class ThreadsDatabase extends RoomDatabase {


    public abstract UserDao userDao();

    public abstract ThreadDao threadDao();


    public abstract SettingsDao settingsDao();


    public void clear() {
        userDao().clear();
        threadDao().clear();
        settingsDao().clear();
    }

}
