package threads.server.core.peers;

import androidx.room.RoomDatabase;

@androidx.room.Database(entities = {Peer.class}, version = 103, exportSchema = false)
public abstract class PeersDatabase extends RoomDatabase {
    public abstract PeerDao peersDao();
}
