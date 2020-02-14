package threads.server.core.peers;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertUsers(User... users);

    @Query("DELETE FROM User")
    void clear();

    @Query("SELECT * FROM User")
    List<User> getUsers();

    @Query("SELECT * FROM User WHERE blocked = 0 AND lite = 1")
    List<User> getNonBlockedLiteUsers();

    @Query("SELECT * FROM User WHERE blocked = 0 AND lite = 1")
    LiveData<List<User>> getLiveDataNonBlockedLiteUsers();

    @Query("SELECT pid FROM User")
    List<String> getUserPids();

    @Query("UPDATE User SET alias = :alias WHERE pid = :pid")
    void setAlias(String pid, String alias);

    @Query("SELECT publicKey FROM User WHERE pid = :pid ")
    String getPublicKey(String pid);

    @Query("SELECT lite FROM User WHERE pid = :pid ")
    boolean isLite(String pid);

    @Query("SELECT alias FROM User WHERE pid = :pid ")
    String getAlias(String pid);

    @Query("SELECT * FROM User WHERE pid = :pid")
    User getUserByPid(String pid);

    @Query("DELETE FROM User WHERE pid = :pid")
    void removeUserByPid(String pid);

    @Query("SELECT * FROM User")
    LiveData<List<User>> getLiveDataUsers();

    @Query("UPDATE User SET dialing = :dialing WHERE pid = :pid")
    void setUserDialing(String pid, boolean dialing);

    @Query("SELECT COUNT(*) FROM User WHERE pid = :pid")
    long hasUser(String pid);

    @Query("SELECT dialing FROM User WHERE pid = :pid")
    boolean getUserDialing(String pid);

    @Query("UPDATE User SET connected = 1, dialing = 0 WHERE pid = :pid")
    void setConnected(String pid);

    @Query("UPDATE User SET connected = 0 WHERE pid = :pid")
    void setDisconnected(String pid);

    @Query("SELECT connected FROM User WHERE pid = :pid ")
    boolean isConnected(String pid);

    @Query("UPDATE User SET blocked = :blocked WHERE pid = :pid")
    void setBlocked(String pid, boolean blocked);

    @Query("SELECT blocked FROM User WHERE pid = :pid ")
    boolean isBlocked(String pid);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    void updateUser(User user);
}
