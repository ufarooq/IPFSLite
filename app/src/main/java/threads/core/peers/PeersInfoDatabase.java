package threads.core.peers;


import androidx.room.RoomDatabase;

@androidx.room.Database(entities = {PeerInfo.class}, version = 1, exportSchema = false)
public abstract class PeersInfoDatabase extends RoomDatabase {

    public abstract PeerInfoDao peersInfoDao();
}
