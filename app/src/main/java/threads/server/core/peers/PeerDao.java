package threads.server.core.peers;


import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface PeerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertPeers(List<Peer> peers);

    @Query("DELETE FROM Peer")
    void clear();

    @Query("SELECT * FROM Peer")
    LiveData<List<Peer>> getLiveDataPeers();

    @Query("SELECT * FROM Peer")
    List<Peer> getPeers();

}
