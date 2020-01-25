package threads.server.core.peers;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.TypeConverters;
import androidx.room.Update;

import java.util.List;

import threads.ipfs.CID;
import threads.server.core.Converter;

@Dao
public interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertUsers(User... users);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    void updateUser(User user);

    @Query("DELETE FROM User")
    void clear();

    @Query("SELECT * FROM User")
    List<User> getUsers();

    @Query("SELECT * FROM User WHERE blocked = :blocked")
    List<User> getBlockedUsers(boolean blocked);

    @Query("SELECT pid FROM User")
    List<String> getUserPids();

    @Query("UPDATE User SET hash = :hash WHERE pid = :pid")
    void setHash(String pid, String hash);

    @Query("UPDATE User SET alias = :alias WHERE pid = :pid")
    void setAlias(String pid, String alias);

    @Query("UPDATE User SET publicKey = :publicKey WHERE pid = :pid")
    void setPublicKey(String pid, String publicKey);

    @Query("UPDATE User SET image = :image WHERE pid = :pid")
    @TypeConverters(Converter.class)
    void setImage(String pid, CID image);

    @Query("SELECT publicKey FROM User WHERE pid = :pid ")
    String getPublicKey(String pid);

    @Query("SELECT alias FROM User WHERE pid = :pid ")
    String getAlias(String pid);

    @Query("SELECT * FROM User WHERE pid = :pid")
    User getUserByPid(String pid);

    @Query("SELECT * FROM User WHERE pid IN (:pids)")
    List<User> getUsersByPid(String... pids);

    @Query("SELECT * FROM User WHERE pid = :pid")
    LiveData<User> getLiveDataUser(String pid);

    @Query("DELETE FROM User WHERE pid = :pid")
    void removeUserByPid(String pid);

    @Query("DELETE FROM User WHERE pid IN (:pids)")
    void removeUsersByPid(String... pids);

    @Query("SELECT * FROM User")
    LiveData<List<User>> getLiveDataUsers();

    @Query("UPDATE User SET dialing = 0")
    void resetUsersDialing();

    @Query("UPDATE User SET dialing = :dialing WHERE pid = :pid")
    void setUserDialing(String pid, boolean dialing);

    @Query("SELECT * FROM User WHERE hash =:hash")
    User getUserByHash(String hash);

    @Delete
    void removeUsers(User... users);

    @Query("SELECT COUNT(*) FROM User WHERE pid = :pid")
    long hasUser(String pid);

    @Query("SELECT dialing FROM User WHERE pid = :pid")
    boolean getUserDialing(String pid);


    @Query("UPDATE User SET connected = 0")
    void resetUsersConnected();

    @Query("UPDATE User SET connected = :connected WHERE pid = :pid")
    void setConnected(String pid, boolean connected);

    @Query("SELECT connected FROM User WHERE pid = :pid ")
    boolean isConnected(String pid);

    @Query("UPDATE User SET blocked = :blocked WHERE pid = :pid")
    void setBlocked(String pid, boolean blocked);

    @Query("SELECT blocked FROM User WHERE pid = :pid ")
    boolean isBlocked(String pid);
}
