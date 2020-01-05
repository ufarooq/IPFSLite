package threads.core.api;

import androidx.room.RoomDatabase;

@androidx.room.Database(entities = {Peer.class}, version = 1, exportSchema = false)
public abstract class PeersDatabase extends RoomDatabase {
    public abstract PeerDao peersDao();
}
