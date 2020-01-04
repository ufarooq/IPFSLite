package threads.core.api;

import androidx.room.RoomDatabase;

@androidx.room.Database(entities = {User.class,
        Thread.class, Note.class, Settings.class}, version = 74, exportSchema = false)
public abstract class ThreadsDatabase extends RoomDatabase {


    public abstract UserDao userDao();

    public abstract ThreadDao threadDao();

    public abstract NoteDao noteDao();

    public abstract SettingsDao settingsDao();


    public void clear() {
        userDao().clear();
        threadDao().clear();
        noteDao().clear();
        settingsDao().clear();
    }

}
