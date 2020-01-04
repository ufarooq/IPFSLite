package threads.core.api;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface PeerInfoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertPeerInfo(PeerInfo peer);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    void updatePeerInfo(PeerInfo peer);

    @Delete
    void deletePeerInfo(PeerInfo peer);

    @Query("DELETE FROM PeerInfo")
    void clear();

    @Query("SELECT * FROM PeerInfo WHERE pid = :pid")
    PeerInfo getPeerInfoByPid(String pid);

    @Query("SELECT * FROM PeerInfo WHERE hash =:hash")
    PeerInfo getPeerInfoByHash(String hash);

    @Query("UPDATE PeerInfo SET hash = :hash WHERE pid = :pid")
    void setHash(String pid, String hash);

    @Query("SELECT hash FROM PeerInfo WHERE pid = :pid")
    String getPeerInfoHash(String pid);
}
