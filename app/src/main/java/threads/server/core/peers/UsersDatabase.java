package threads.server.core.peers;

import androidx.room.RoomDatabase;

@androidx.room.Database(entities = {User.class}, version = 111, exportSchema = false)
public abstract class UsersDatabase extends RoomDatabase {

    public abstract UserDao userDao();
}
