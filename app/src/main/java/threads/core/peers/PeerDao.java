package threads.core.peers;


import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.TypeConverters;
import androidx.room.Update;

import java.util.List;

import threads.ipfs.api.CID;

@Dao
public interface PeerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertPeer(Peer peer);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    void updatePeer(Peer peer);

    @Delete
    void deletePeer(Peer peer);

    @Query("DELETE FROM Peer")
    void clear();

    @Query("SELECT * FROM Peer WHERE pid = :pid")
    Peer getPeerByPid(String pid);

    @Query("SELECT * FROM Peer WHERE isRelay = 1")
    List<Peer> getRelayPeers();

    @Query("SELECT * FROM Peer WHERE isAutonat = 1")
    List<Peer> getAutonatPeers();

    @Query("SELECT * FROM Peer WHERE isPubsub = 1")
    List<Peer> getPubsubPeers();


    @Query("SELECT * FROM Peer")
    LiveData<List<Peer>> getLiveDataPeers();


    @Query("SELECT COUNT(pid) FROM Peer WHERE image =:cid")
    @TypeConverters({Converter.class})
    int references(CID cid);

    @Query("UPDATE Peer SET connected = 0")
    void resetPeersConnected();

    @Query("UPDATE Peer SET connected = :connected WHERE pid = :pid")
    void setConnected(String pid, boolean connected);

    @Query("SELECT connected FROM Peer WHERE pid = :pid ")
    boolean isConnected(String pid);

    @Query("SELECT * FROM Peer")
    List<Peer> getPeers();

    @Query("UPDATE Peer SET timestamp = :timestamp WHERE pid = :pid")
    void setTimestamp(String pid, long timestamp);
}
