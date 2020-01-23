package threads.server.core.peers;

import androidx.room.RoomDatabase;

@androidx.room.Database(entities = {User.class, Peer.class}, version = 100, exportSchema = false)
public abstract class PeersDatabase extends RoomDatabase {
    public abstract PeerDao peersDao();

    public abstract UserDao userDao();


    public void clear() {
        userDao().clear();
    }
}
