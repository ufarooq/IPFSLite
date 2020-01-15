package threads.core.peers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import threads.iota.Entity;
import threads.iota.EntityService;
import threads.iota.Hash;
import threads.iota.IOTA;
import threads.ipfs.CID;
import threads.ipfs.IPFS;
import threads.ipfs.PID;

import static androidx.core.util.Preconditions.checkArgument;
import static androidx.core.util.Preconditions.checkNotNull;


public class PeersAPI {
    private final static String TAG = PeersAPI.class.getSimpleName();

    private final PeersInfoDatabase peersInfoDatabase;
    private final PeersDatabase peersDatabase;

    public PeersAPI(@NonNull PeersInfoDatabase peersInfoDatabase,
                    @NonNull PeersDatabase peersDatabase) {
        checkNotNull(peersInfoDatabase);
        checkNotNull(peersDatabase);

        this.peersInfoDatabase = peersInfoDatabase;
        this.peersDatabase = peersDatabase;

    }

    @NonNull
    public PeersDatabase getPeersDatabase() {
        return peersDatabase;
    }

    @NonNull
    private PeersInfoDatabase getPeersInfoDatabase() {
        return peersInfoDatabase;
    }


    @NonNull
    public String getRandomAddress() {
        return IOTA.generateAddress();
    }


    public void setImage(@NonNull User user, @NonNull CID image) {
        checkNotNull(user);
        checkNotNull(image);
        getPeersDatabase().userDao().setImage(user.getPid(), image);
    }


    public boolean isUserBlocked(@NonNull PID user) {
        checkNotNull(user);
        return isUserBlocked(user.getPid());
    }

    public boolean isUserBlocked(@NonNull String pid) {
        checkNotNull(pid);
        return getPeersDatabase().userDao().isBlocked(pid);
    }


    @Nullable
    public PeerInfo getPeerInfoByHash(@NonNull String hash) {
        checkNotNull(hash);
        return getPeersInfoDatabase().peersInfoDao().getPeerInfoByHash(hash);
    }

    public void storeUser(@NonNull User user) {
        checkNotNull(user);
        getPeersDatabase().userDao().insertUsers((User) user);
    }


    @NonNull
    public PeerInfo createPeerInfo(@NonNull PID owner) {
        checkNotNull(owner);

        return PeerInfo.createPeerInfo(owner);
    }

    @NonNull
    public Peer createPeer(@NonNull PID pid, @NonNull String multiAddress) {
        checkNotNull(pid);
        checkNotNull(multiAddress);

        return Peer.createPeer(pid, multiAddress);
    }

    @Nullable
    public Peer getPeerByPID(@NonNull PID pid) {
        checkNotNull(pid);
        return getPeersDatabase().peersDao().getPeerByPid(pid.getPid());
    }

    @NonNull
    public List<Peer> getRelayPeers() {
        return getPeersDatabase().peersDao().getRelayPeers();
    }

    @NonNull
    public List<Peer> getAutonatPeers() {
        return getPeersDatabase().peersDao().getAutonatPeers();
    }

    @NonNull
    public List<Peer> getPeers() {
        return getPeersDatabase().peersDao().getPeers();
    }

    @NonNull
    public List<Peer> getPubsubPeers() {
        return getPeersDatabase().peersDao().getPubsubPeers();
    }


    @Nullable
    public PeerInfo getPeerInfoByPID(@NonNull PID pid) {
        checkNotNull(pid);
        return getPeersInfoDatabase().peersInfoDao().getPeerInfoByPid(pid.getPid());
    }

    public void storePeer(@NonNull Peer peer) {
        checkNotNull(peer);
        getPeersDatabase().peersDao().insertPeer(peer);
    }

    public void storePeerInfo(@NonNull PeerInfo peer) {
        checkNotNull(peer);
        getPeersInfoDatabase().peersInfoDao().insertPeerInfo(peer);
    }

    public void updatePeerInfo(@NonNull PeerInfo peer) {
        checkNotNull(peer);
        getPeersInfoDatabase().peersInfoDao().updatePeerInfo(peer);
    }

    public void updatePeer(@NonNull Peer peer) {
        checkNotNull(peer);
        getPeersDatabase().peersDao().updatePeer(peer);
    }


    @NonNull
    public User createUser(@NonNull PID pid,
                           @NonNull String publicKey,
                           @NonNull String name,
                           @NonNull UserType type,
                           @Nullable CID image) {
        checkNotNull(pid);
        checkNotNull(publicKey);
        checkNotNull(name);
        checkNotNull(type);
        checkArgument(!pid.getPid().isEmpty());

        return User.createUser(type, name, publicKey, pid, image);
    }


    public void setAdditional(@NonNull User user,
                              @NonNull String key,
                              @NonNull String value,
                              boolean internal) {
        checkNotNull(user);
        checkNotNull(key);
        checkNotNull(value);
        User update = getUserByPID(user.getPID());
        checkNotNull(update);
        update.addAdditional(key, value, internal);
        updateUser(update);
    }


    public boolean isReferenced(@NonNull CID cid) {
        checkNotNull(cid);
        int counter = getPeersDatabase().peersDao().references(cid);
        return counter > 0;
    }


    public void unpin(@NonNull IPFS ipfs, @Nullable CID cid) {
        try {
            if (cid != null) {
                if (!isReferenced(cid)) {
                    rm(ipfs, cid);
                }
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
    }


    @Nullable
    public PeerInfo getPeer(@NonNull Context context,
                            @NonNull EntityService entityService,
                            @NonNull PID pid) {
        checkNotNull(context);
        checkNotNull(pid);


        PeerInfo peer = loadPeer(context, entityService, pid);
        if (peer != null) {

            PeerInfo storePeer = getPeerInfoByPID(pid);
            if (storePeer != null) {
                if (storePeer.getTimestamp() > peer.getTimestamp()) {
                    // store peer is newer
                    mergePeerInfo(storePeer, peer, true);
                    return storePeer;
                } else {
                    mergePeerInfo(peer, storePeer, false);
                    storePeerInfo(peer);
                    return peer;
                }
            } else {
                storePeerInfo(peer);
                return peer;
            }
        }
        return getPeerInfoByPID(pid);
    }

    @Nullable
    public PeerInfo loadPeer(@NonNull Context context,
                             @NonNull EntityService entityService,
                             @NonNull PID pid) {
        checkNotNull(context);
        checkNotNull(entityService);
        checkNotNull(pid);

        String address = AddressType.getAddress(pid, AddressType.PEER);

        AtomicReference<PeerInfo> reference = new AtomicReference<>(null);

        try {
            List<Entity> entities = entityService.loadEntities(context, address);
            for (Entity entity : entities) {
                PeerInfo peer = PeerInfoDecoder.convert(pid, entity);

                if (peer != null) {

                    PeerInfo inserted = reference.get();
                    if (inserted != null) {

                        if (inserted.getTimestamp() < peer.getTimestamp()) {
                            // replace items (means peer is newer)
                            mergePeerInfo(peer, inserted, false);
                            reference.set(peer);

                        } else {

                            mergePeerInfo(inserted, peer, false);
                        }
                    } else {
                        reference.set(peer);
                    }
                }
            }

        } catch (Throwable e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }

        return reference.get();
    }

    private void mergePeerInfo(@NonNull PeerInfo newer, @NonNull PeerInfo older, boolean update) {
        checkNotNull(newer);
        checkNotNull(older);
        boolean doUpdate = false;
        Additionals additionals = older.getAdditionals();
        for (String key : additionals.keySet()) {
            if (!newer.hasAdditional(key)) {
                Additional additional = older.getAdditional(key);
                checkNotNull(additional);
                newer.addAdditional(key, additional.getValue(), additional.getInternal());
                doUpdate = true;
            }
        }
        if (update && doUpdate) {
            updatePeerInfo(newer);
        }
    }


    public void removeUser(@NonNull IPFS ipfs, @NonNull User user) {
        checkNotNull(user);
        getPeersDatabase().userDao().removeUsers(user);

        unpin(ipfs, user.getImage());

    }


    public boolean existsUser(@NonNull PID user) {
        checkNotNull(user);
        return existsUser(user.getPid());
    }

    public boolean existsUser(@NonNull String pid) {
        checkNotNull(pid);
        return getPeersDatabase().userDao().hasUser(pid) > 0;
    }

    public void rm(@NonNull IPFS ipfs, @NonNull CID cid) {
        checkNotNull(ipfs);
        checkNotNull(cid);
        checkNotNull(cid);
        ipfs.rm(cid);
    }

    @Nullable
    public User getUserByPID(@NonNull PID pid) {
        checkNotNull(pid);
        checkArgument(!pid.getPid().isEmpty());
        return getPeersDatabase().userDao().getUserByPid(pid.getPid());
    }


    @NonNull
    public List<User> getUsersByPID(@NonNull String... pids) {
        checkNotNull(pids);
        return getPeersDatabase().userDao().getUsersByPid(pids);
    }

    @NonNull
    public List<User> getUsers() {
        return getPeersDatabase().userDao().getUsers();
    }

    @NonNull
    public List<User> getBlockedUsers(boolean blocked) {
        return getPeersDatabase().userDao().getBlockedUsers(blocked);
    }

    @Nullable
    public String getPeerInfoHash(@NonNull PID pid) {
        checkNotNull(pid);
        return getPeersInfoDatabase().peersInfoDao().getPeerInfoHash(pid.getPid());
    }

    public void setHash(@NonNull PeerInfo peer, @Nullable String hash) {
        checkNotNull(peer);
        peer.setHash(hash);
        getPeersInfoDatabase().peersInfoDao().setHash(peer.getPid(), hash);

    }

    public void setHash(@NonNull User user, @Nullable String hash) {
        checkNotNull(user);
        user.setHash(hash);
        getPeersDatabase().userDao().setHash(user.getPid(), hash);
    }


    public void updateUser(@NonNull User user) {
        getPeersDatabase().userDao().updateUser(user);
    }


    public void setConnected(@NonNull User user, boolean connected) {
        checkNotNull(user);
        setUserConnected(user.getPID(), connected);
    }

    public void setUserConnected(@NonNull PID user, boolean connected) {
        checkNotNull(user);
        getPeersDatabase().userDao().setConnected(user.getPid(), connected);
    }

    public void setUserAutoConnect(@NonNull PID user, boolean autoConnect) {
        checkNotNull(user);
        getPeersDatabase().userDao().setAutoConnect(user.getPid(), autoConnect);
    }

    public void setUserDialing(@NonNull PID user, boolean dialing) {
        checkNotNull(user);
        getPeersDatabase().userDao().setUserDialing(user.getPid(), dialing);
    }

    public List<User> getAutoConnectUsers() {
        return getPeersDatabase().userDao().getAutoConnectUsers(true);
    }


    public boolean isUserConnected(@NonNull User user) {
        checkNotNull(user);
        return isUserConnected(user.getPid());
    }


    public boolean getUserDialing(@NonNull PID user) {
        checkNotNull(user);
        return getUserDialing(user.getPid());
    }

    public boolean getUserDialing(@NonNull String pid) {
        checkNotNull(pid);
        return getPeersDatabase().userDao().getUserDialing(pid);
    }


    public boolean isUserConnected(@NonNull PID user) {
        checkNotNull(user);
        return isUserConnected(user.getPid());
    }

    public boolean isUserConnected(@NonNull String pid) {
        checkNotNull(pid);
        return getPeersDatabase().userDao().isConnected(pid);
    }

    public boolean isUserAutoConnect(@NonNull PID user) {
        checkNotNull(user);
        return isUserAutoConnect(user.getPid());
    }

    public boolean isUserAutoConnect(@NonNull String pid) {
        checkNotNull(pid);
        return getPeersDatabase().userDao().isAutoConnect(pid);
    }

    public void resetUsersDialing() {
        getPeersDatabase().userDao().resetUsersDialing();
    }

    public void resetUsersConnected() {
        getPeersDatabase().userDao().resetUsersConnected();
    }

    public void resetPeersConnected() {
        getPeersDatabase().peersDao().resetPeersConnected();
    }


    public void setTimestamp(@NonNull Peer peer, long timestamp) {
        checkNotNull(peer);
        getPeersDatabase().peersDao().setTimestamp(peer.getPid(), timestamp);
    }


    public void setUserPublicKey(@NonNull User user, @NonNull String publicKey) {
        checkNotNull(user);
        checkNotNull(publicKey);
        setUserPublicKey(user.getPid(), publicKey);
    }

    public void setUserPublicKey(@NonNull PID user, @NonNull String publicKey) {
        checkNotNull(user);
        checkNotNull(publicKey);
        setUserPublicKey(user.getPid(), publicKey);
    }

    public void setUserPublicKey(@NonNull String pid, @NonNull String publicKey) {
        checkNotNull(pid);
        checkNotNull(publicKey);
        getPeersDatabase().userDao().setPublicKey(pid, publicKey);
    }

    public String getUserPublicKey(@NonNull User user) {
        checkNotNull(user);
        return getUserPublicKey(user.getPid());
    }

    public String getUserPublicKey(@NonNull PID user) {
        checkNotNull(user);
        return getUserPublicKey(user.getPid());
    }

    public String getUserPublicKey(@NonNull String pid) {
        checkNotNull(pid);
        return getPeersDatabase().userDao().getPublicKey(pid);
    }

    public String getUserAlias(@NonNull User user) {
        checkNotNull(user);
        return getUserAlias(user.getPid());
    }

    public String getUserAlias(@NonNull PID user) {
        checkNotNull(user);
        return getUserAlias(user.getPid());
    }

    public String getUserAlias(@NonNull String pid) {
        checkNotNull(pid);
        return getPeersDatabase().userDao().getAlias(pid);
    }


    public void setUserAlias(@NonNull User user, @NonNull String alias) {
        checkNotNull(user);
        checkNotNull(alias);
        setUserAlias(user.getPid(), alias);
    }

    public void setUserAlias(@NonNull PID user, @NonNull String alias) {
        checkNotNull(user);
        checkNotNull(alias);
        setUserAlias(user.getPid(), alias);
    }

    public void setUserAlias(@NonNull String pid, @NonNull String alias) {
        checkNotNull(pid);
        checkNotNull(alias);
        getPeersDatabase().userDao().setAlias(pid, alias);
    }


    public void setUserImage(@NonNull PID user, @NonNull CID image) {
        checkNotNull(user);
        checkNotNull(image);
        setUserImage(user.getPid(), image);
    }

    public void setUserImage(@NonNull String pid, @NonNull CID image) {
        checkNotNull(pid);
        checkNotNull(image);
        getPeersDatabase().userDao().setImage(pid, image);
    }


    public void setUserType(@NonNull User user, @NonNull UserType type) {
        checkNotNull(user);
        checkNotNull(type);
        setUserType(user.getPid(), type);
    }

    public void setUserType(@NonNull PID user, @NonNull UserType type) {
        checkNotNull(user);
        checkNotNull(type);
        setUserType(user.getPid(), type);
    }

    public void setUserType(@NonNull String pid, @NonNull UserType type) {
        checkNotNull(pid);
        checkNotNull(type);
        getPeersDatabase().userDao().setUserType(pid, type);
    }


    public void blockUser(@NonNull PID user) {
        checkNotNull(user);
        getPeersDatabase().userDao().setBlocked(user.getPid(), true);
    }


    public void blockUser(@NonNull User user) {
        checkNotNull(user);
        blockUser(user.getPID());
    }


    public void unblockUser(@NonNull User user) {
        checkNotNull(user);
        unblockUser(user.getPID());
    }

    public void unblockUser(@NonNull PID user) {
        checkNotNull(user);
        getPeersDatabase().userDao().setBlocked(user.getPid(), false);
    }


    @NonNull
    public Hash createHash(@NonNull String hash) {
        checkNotNull(hash);
        return Hash.create(hash, System.currentTimeMillis());
    }


    @NonNull
    public List<PID> getUsersPIDs() {
        List<PID> result = new ArrayList<>();
        List<String> pids = getPeersDatabase().userDao().getUserPids();
        for (String pid : pids) {
            result.add(PID.create(pid));
        }
        return result;
    }

    public boolean insertPeerInfo(@NonNull Context context,
                                  @NonNull EntityService entityService,
                                  @NonNull PeerInfo peer) {
        checkNotNull(context);
        checkNotNull(entityService);
        checkNotNull(peer);
        try {

            String address = AddressType.getAddress(peer.getPID(), AddressType.PEER);

            String data = PeerInfoEncoder.convert(peer);


            Entity entity = entityService.insertData(context, address, data);
            checkNotNull(entity);

            String hash = entity.getHash();
            setHash(peer, hash);
            return true;
        } catch (Throwable e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
            return false;
        }
    }


    public void removePeerInfo(@NonNull PeerInfo peer) {
        checkNotNull(peer);
        getPeersInfoDatabase().peersInfoDao().deletePeerInfo(peer);
    }

    public void removePeer(@NonNull IPFS ipfs, @NonNull Peer peer) {
        checkNotNull(peer);
        getPeersDatabase().peersDao().deletePeer(peer);
        unpin(ipfs, peer.getImage());
    }


}
