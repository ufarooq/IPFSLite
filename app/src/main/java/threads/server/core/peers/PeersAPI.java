package threads.server.core.peers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import threads.ipfs.PID;

import static androidx.core.util.Preconditions.checkArgument;
import static androidx.core.util.Preconditions.checkNotNull;


public class PeersAPI {

    private final PeersDatabase peersDatabase;
    private final UsersDatabase usersDatabase;

    PeersAPI(@NonNull UsersDatabase usersDatabase,
             @NonNull PeersDatabase peersDatabase) {
        checkNotNull(peersDatabase);
        checkNotNull(usersDatabase);
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
        checkNotNull(user);
        return isUserBlocked(user.getPid());
    }

    public boolean isUserBlocked(@NonNull String pid) {
        checkNotNull(pid);
        return getUsersDatabase().userDao().isBlocked(pid);
    }


    public void storeUser(@NonNull User user) {
        checkNotNull(user);
        getUsersDatabase().userDao().insertUsers((User) user);
    }


    @NonNull
    public Peer createPeer(@NonNull PID pid) {
        checkNotNull(pid);
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
        checkNotNull(pid);
        checkNotNull(name);
        checkArgument(!pid.getPid().isEmpty());

        return User.createUser(name, pid);
    }


    public void removeUser(@NonNull String pid) {
        checkNotNull(pid);
        getUsersDatabase().userDao().removeUserByPid(pid);
    }


    public boolean existsUser(@NonNull PID user) {
        checkNotNull(user);
        return existsUser(user.getPid());
    }

    private boolean existsUser(@NonNull String pid) {
        checkNotNull(pid);
        return getUsersDatabase().userDao().hasUser(pid) > 0;
    }


    @Nullable
    public User getUserByPID(@NonNull PID pid) {
        checkNotNull(pid);
        checkArgument(!pid.getPid().isEmpty());
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
        checkNotNull(user);
        getUsersDatabase().userDao().setConnected(user.getPid());
    }

    public void setUserDisconnected(@NonNull PID user) {
        checkNotNull(user);
        getUsersDatabase().userDao().setDisconnected(user.getPid());
    }

    public void setUserDialing(@NonNull String pid, boolean dialing) {
        checkNotNull(pid);
        getUsersDatabase().userDao().setUserDialing(pid, dialing);
    }


    public boolean getUserDialing(@NonNull PID user) {
        checkNotNull(user);
        return getUserDialing(user.getPid());
    }

    private boolean getUserDialing(@NonNull String pid) {
        checkNotNull(pid);
        return getUsersDatabase().userDao().getUserDialing(pid);
    }


    public boolean isUserConnected(@NonNull PID user) {
        checkNotNull(user);
        return isUserConnected(user.getPid());
    }

    private boolean isUserConnected(@NonNull String pid) {
        checkNotNull(pid);
        return getUsersDatabase().userDao().isConnected(pid);
    }


    @Nullable
    public String getUserPublicKey(@NonNull String pid) {
        checkNotNull(pid);
        return getUsersDatabase().userDao().getPublicKey(pid);
    }


    @Nullable
    public String getUserAlias(@NonNull String pid) {
        checkNotNull(pid);
        return getUsersDatabase().userDao().getAlias(pid);
    }

    public void setUserAlias(@NonNull String pid, @NonNull String alias) {
        checkNotNull(pid);
        checkNotNull(alias);
        getUsersDatabase().userDao().setAlias(pid, alias);
    }


    public void blockUser(@NonNull PID user) {
        checkNotNull(user);
        getUsersDatabase().userDao().setBlocked(user.getPid(), true);
    }

    public void unblockUser(@NonNull PID user) {
        checkNotNull(user);
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
        checkNotNull(pid);
        return getUsersDatabase().userDao().isLite(pid);
    }

    public void updateUser(@NonNull User user) {
        checkNotNull(user);
        getUsersDatabase().userDao().updateUser(user);
    }
}
