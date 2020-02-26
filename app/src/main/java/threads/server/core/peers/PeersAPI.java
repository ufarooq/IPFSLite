package threads.server.core.peers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import threads.ipfs.PID;


public class PeersAPI {

    private final PeersDatabase peersDatabase;
    private final UsersDatabase usersDatabase;

    PeersAPI(@NonNull UsersDatabase usersDatabase,
             @NonNull PeersDatabase peersDatabase) {

        this.usersDatabase = usersDatabase;
        this.peersDatabase = peersDatabase;

    }

    @NonNull
    public PeersDatabase getPeersDatabase() {
        return peersDatabase;
    }

    @NonNull
    public UsersDatabase getUsersDatabase() {
        return usersDatabase;
    }


    public boolean isUserBlocked(@NonNull PID user) {

        return isUserBlocked(user.getPid());
    }

    public boolean isUserBlocked(@NonNull String pid) {

        return getUsersDatabase().userDao().isBlocked(pid);
    }


    public void storeUser(@NonNull User user) {

        getUsersDatabase().userDao().insertUsers(user);
    }


    @NonNull
    public Peer createPeer(@NonNull PID pid) {

        return Peer.createPeer(pid);
    }


    @NonNull
    public List<Peer> getPeers() {
        return getPeersDatabase().peersDao().getPeers();
    }


    public void storePeers(@NonNull List<Peer> peers) {
        getPeersDatabase().peersDao().insertPeers(peers);
    }


    @NonNull
    public User createUser(@NonNull PID pid, @NonNull String name) {

        return User.createUser(name, pid);
    }


    public void removeUser(@NonNull String pid) {

        getUsersDatabase().userDao().removeUserByPid(pid);
    }


    public boolean existsUser(@NonNull PID user) {

        return existsUser(user.getPid());
    }

    private boolean existsUser(@NonNull String pid) {

        return getUsersDatabase().userDao().hasUser(pid) > 0;
    }


    @Nullable
    public User getUserByPID(@NonNull PID pid) {

        return getUsersDatabase().userDao().getUserByPid(pid.getPid());
    }


    @NonNull
    public List<User> getUsers() {
        return getUsersDatabase().userDao().getUsers();
    }

    @NonNull
    public List<User> getNonBlockedLiteUsers() {
        return getUsersDatabase().userDao().getNonBlockedLiteUsers();
    }

    public void setUserConnected(@NonNull PID user) {

        getUsersDatabase().userDao().setConnected(user.getPid());
    }

    public void setUserDisconnected(@NonNull PID user) {

        getUsersDatabase().userDao().setDisconnected(user.getPid());
    }

    public void setUserDialing(@NonNull String pid, boolean dialing) {

        getUsersDatabase().userDao().setUserDialing(pid, dialing);
    }


    public boolean getUserDialing(@NonNull PID user) {

        return getUserDialing(user.getPid());
    }

    private boolean getUserDialing(@NonNull String pid) {

        return getUsersDatabase().userDao().getUserDialing(pid);
    }


    public boolean isUserConnected(@NonNull PID user) {

        return isUserConnected(user.getPid());
    }

    private boolean isUserConnected(@NonNull String pid) {

        return getUsersDatabase().userDao().isConnected(pid);
    }


    @Nullable
    public String getUserPublicKey(@NonNull String pid) {

        return getUsersDatabase().userDao().getPublicKey(pid);
    }


    @Nullable
    public String getUserAlias(@NonNull String pid) {

        return getUsersDatabase().userDao().getAlias(pid);
    }

    public void setUserAlias(@NonNull String pid, @NonNull String alias) {

        getUsersDatabase().userDao().setAlias(pid, alias);
    }


    public void blockUser(@NonNull PID user) {

        getUsersDatabase().userDao().setBlocked(user.getPid(), true);
    }

    public void unblockUser(@NonNull PID user) {

        getUsersDatabase().userDao().setBlocked(user.getPid(), false);
    }

    @NonNull
    public List<PID> getUsersPIDs() {
        List<PID> result = new ArrayList<>();
        List<String> pids = getUsersDatabase().userDao().getUserPids();
        for (String pid : pids) {
            result.add(PID.create(pid));
        }
        return result;
    }


    public void clearPeers() {
        getPeersDatabase().peersDao().clear();
    }

    public boolean getUserIsLite(@NonNull String pid) {

        return getUsersDatabase().userDao().isLite(pid);
    }

    public void updateUser(@NonNull User user) {

        getUsersDatabase().userDao().updateUser(user);
    }
}
