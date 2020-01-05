package threads.core.api;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.collect.Iterables;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import threads.iota.Entity;
import threads.iota.EntityService;
import threads.iota.Hash;
import threads.iota.IOTA;
import threads.ipfs.IPFS;
import threads.ipfs.api.CID;
import threads.ipfs.api.Multihash;
import threads.ipfs.api.PID;

import static androidx.core.util.Preconditions.checkArgument;
import static androidx.core.util.Preconditions.checkNotNull;


public class ThreadsAPI {
    private final static String TAG = ThreadsAPI.class.getSimpleName();
    private final Gson gson = new Gson();
    private final ThreadsDatabase threadsDatabase;
    private final EventsDatabase eventsDatabase;
    private final PeersInfoDatabase peersInfoDatabase;
    private final PeersDatabase peersDatabase;

    public ThreadsAPI(@NonNull ThreadsDatabase threadsDatabase,
                      @NonNull EventsDatabase eventsDatabase,
                      @NonNull PeersInfoDatabase peersInfoDatabase,
                      @NonNull PeersDatabase peersDatabase) {
        checkNotNull(threadsDatabase);
        checkNotNull(eventsDatabase);
        checkNotNull(peersInfoDatabase);
        checkNotNull(peersDatabase);

        this.threadsDatabase = threadsDatabase;
        this.eventsDatabase = eventsDatabase;
        this.peersInfoDatabase = peersInfoDatabase;
        this.peersDatabase = peersDatabase;

    }

    @NonNull
    private PeersDatabase getPeersDatabase() {
        return peersDatabase;
    }

    @NonNull
    private PeersInfoDatabase getPeersInfoDatabase() {
        return peersInfoDatabase;
    }

    @NonNull
    private EventsDatabase getEventsDatabase() {
        return eventsDatabase;
    }

    @NonNull
    private ThreadsDatabase getThreadsDatabase() {
        return threadsDatabase;
    }


    @NonNull
    public String getRandomAddress() {
        return IOTA.generateAddress();
    }


    public int getThreadsNumber() {
        return getThreadsDatabase().threadDao().getThreadsNumber();
    }

    public void setThreadStatus(long idx, @NonNull Status status) {
        checkNotNull(status);
        getThreadsDatabase().threadDao().setStatus(idx, status);
    }

    public void setThreadsStatus(@NonNull Status status, long... idxs) {
        checkNotNull(status);
        getThreadsDatabase().threadDao().setThreadsStatus(status, idxs);
    }

    public void setThreadLeaching(long idx, boolean leaching) {
        getThreadsDatabase().threadDao().setLeaching(idx, leaching);
    }

    public void setThreadPublishing(long idx, boolean publish) {
        getThreadsDatabase().threadDao().setPublishing(idx, publish);
    }

    public void setThreadsLeaching(boolean leaching, long... idxs) {
        getThreadsDatabase().threadDao().setThreadsLeaching(leaching, idxs);
    }

    public void setThreadsPublishing(boolean publish, long... idxs) {
        getThreadsDatabase().threadDao().setThreadsPublishing(publish, idxs);
    }


    public void setThreadStatus(@NonNull Status oldStatus, @NonNull Status newStatus) {
        checkNotNull(oldStatus);
        getThreadsDatabase().threadDao().setStatus(oldStatus, newStatus);
    }


    public void setImage(@NonNull Thread thread, @NonNull CID image) {
        checkNotNull(thread);
        checkNotNull(image);
        getThreadsDatabase().threadDao().setImage(thread.getIdx(), image);
    }


    public void setImage(@NonNull User user, @NonNull CID image) {
        checkNotNull(user);
        checkNotNull(image);
        getThreadsDatabase().userDao().setImage(user.getPid(), image);
    }


    @NonNull
    public String getMimeType(@NonNull Thread thread) {
        checkNotNull(thread);
        return getThreadsDatabase().threadDao().getMimeType(thread.getIdx());
    }


    @Nullable
    public String getThreadMimeType(long idx) {
        return getThreadsDatabase().threadDao().getMimeType(idx);
    }


    public void setThreadSenderAlias(@NonNull PID pid, @NonNull String alias) {
        checkNotNull(pid);
        checkNotNull(alias);
        getThreadsDatabase().threadDao().setSenderAlias(pid, alias);
    }


    public void setMimeType(@NonNull Thread thread, @NonNull String mimeType) {
        checkNotNull(thread);
        checkNotNull(mimeType);
        getThreadsDatabase().threadDao().setMimeType(thread.getIdx(), mimeType);
    }


    public boolean isUserBlocked(@NonNull PID user) {
        checkNotNull(user);
        return isUserBlocked(user.getPid());
    }

    public boolean isUserBlocked(@NonNull String pid) {
        checkNotNull(pid);
        return getThreadsDatabase().userDao().isBlocked(pid);
    }


    @NonNull
    public Thread createThread(@NonNull User user,
                               @NonNull Status status,
                               @NonNull Kind kind,
                               @NonNull String sesKey,
                               long thread) {

        checkNotNull(user);
        checkNotNull(status);
        checkNotNull(kind);
        checkNotNull(sesKey);
        checkArgument(thread >= 0);
        return createThread(
                status,
                kind,
                user.getPID(),
                user.getAlias(),
                user.getPublicKey(),
                sesKey,
                new Date(),
                thread);

    }


    @Nullable
    public PeerInfo getPeerInfoByHash(@NonNull String hash) {
        checkNotNull(hash);
        return getPeersInfoDatabase().peersInfoDao().getPeerInfoByHash(hash);
    }

    public void storeUser(@NonNull User user) {
        checkNotNull(user);
        getThreadsDatabase().userDao().insertUsers((User) user);
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

    public void setAdditional(@NonNull Thread thread,
                              @NonNull String key,
                              @NonNull String value,
                              boolean internal) {
        checkNotNull(thread);
        checkNotNull(key);
        checkNotNull(value);
        Thread update = getThreadByIdx(thread.getIdx());
        checkNotNull(update);
        update.addAdditional(key, value, internal);
        updateThread(update);
    }


    public boolean isReferenced(@NonNull CID cid) {
        checkNotNull(cid);
        int counter = getThreadsDatabase().threadDao().references(cid);
        counter += getPeersDatabase().peersDao().references(cid);
        return counter > 0;
    }

    public void removeThread(@NonNull IPFS ipfs, @NonNull Thread thread) {
        checkNotNull(ipfs);
        checkNotNull(thread);


        getThreadsDatabase().threadDao().removeThreads(thread);


        unpin(ipfs, thread.getCid());
        unpin(ipfs, thread.getImage());

        // delete all children
        List<Thread> entries = getThreadsByThread(thread.getIdx());
        for (Thread entry : entries) {
            removeThread(ipfs, entry);
        }
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




    public List<Thread> getExpiredThreads() {
        return getThreadsDatabase().threadDao().getExpiredThreads(System.currentTimeMillis());
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
        getThreadsDatabase().userDao().removeUsers(user);

        unpin(ipfs, user.getImage());

    }


    public boolean existsUser(@NonNull PID user) {
        checkNotNull(user);
        return existsUser(user.getPid());
    }

    public boolean existsUser(@NonNull String pid) {
        checkNotNull(pid);
        return getThreadsDatabase().userDao().hasUser(pid) > 0;
    }

    public void removeThreads(@NonNull IPFS ipfs, long... idxs) {
        checkNotNull(ipfs);
        List<Thread> threads = getThreadByIdxs(idxs);
        for (Thread thread : threads) {
            removeThread(ipfs, thread);

        }
    }

    public void rm(@NonNull IPFS ipfs, @NonNull CID cid) {
        checkNotNull(ipfs);
        checkNotNull(cid);
        checkNotNull(cid);
        ipfs.rm(cid);
    }


    public void removeThreads(@NonNull IPFS ipfs, @NonNull List<Thread> threads) {
        checkNotNull(ipfs);
        checkNotNull(threads);

        getThreadsDatabase().threadDao().removeThreads(
                Iterables.toArray(threads, Thread.class));
    }

    @NonNull
    public Event createEvent(@NonNull String identifier, @NonNull String content) {
        checkNotNull(identifier);
        checkNotNull(content);
        return Event.createEvent(identifier, content);
    }

    public void removeEvent(@NonNull Event event) {
        checkNotNull(event);
        getEventsDatabase().eventDao().deleteEvent(event);
    }

    public void removeEvent(@NonNull String identifier) {
        checkNotNull(identifier);
        getEventsDatabase().eventDao().deleteEvent(identifier);
    }


    public void invokeEvent(@NonNull String identifier, @NonNull String content) {
        checkNotNull(identifier);
        checkNotNull(content);
        storeEvent(createEvent(identifier, content));
    }

    public void storeEvent(@NonNull Event event) {
        checkNotNull(event);
        getEventsDatabase().eventDao().insertEvent(event);
    }


    public Message createMessage(@NonNull MessageKind messageKind, @NonNull String message, long timestamp) {
        checkNotNull(messageKind);
        checkNotNull(message);
        return Message.createMessage(messageKind, message, timestamp);
    }


    public void removeMessage(@NonNull Message message) {
        checkNotNull(message);
        getEventsDatabase().messageDao().deleteMessage(message);
    }


    public void storeMessage(@NonNull Message message) {
        checkNotNull(message);
        getEventsDatabase().messageDao().insertMessages(message);
    }


    public void clearMessages() {
        getEventsDatabase().messageDao().clear();
    }


    public List<Thread> getThreads() {
        return getThreadsDatabase().threadDao().getThreads();
    }


    public void incrementThreadNumber(@NonNull Thread thread) {
        checkNotNull(thread);
        incrementThreadsNumber(thread.getIdx());
    }

    public void incrementThreadsNumber(long... idxs) {
        getThreadsDatabase().threadDao().incrementNumber(idxs);
    }


    public void resetThreadsNumber(long... idxs) {
        getThreadsDatabase().threadDao().resetNumber(idxs);
    }

    public void resetThreadNumber(long thread) {
        getThreadsDatabase().threadDao().resetThreadNumber(thread);
    }

    public void resetThreadsNumber() {
        getThreadsDatabase().threadDao().resetThreadsNumber();
    }


    @Nullable
    public User getUserByPID(@NonNull PID pid) {
        checkNotNull(pid);
        checkArgument(!pid.getPid().isEmpty());
        return getThreadsDatabase().userDao().getUserByPid(pid.getPid());
    }


    @NonNull
    public List<User> getUsersByPID(@NonNull String... pids) {
        checkNotNull(pids);
        return getThreadsDatabase().userDao().getUsersByPid(pids);
    }

    @NonNull
    public List<User> getUsers() {
        return getThreadsDatabase().userDao().getUsers();
    }

    @NonNull
    public List<User> getBlockedUsers(boolean blocked) {
        return getThreadsDatabase().userDao().getBlockedUsers(blocked);
    }

    public void setHash(@NonNull Thread thread, @Nullable String hash) {
        checkNotNull(thread);
        long idx = thread.getIdx();
        thread.setHash(hash);
        getThreadsDatabase().threadDao().setHash(idx, hash);

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
        getThreadsDatabase().userDao().setHash(user.getPid(), hash);
    }


    @Nullable
    public Thread getThreadByHash(@NonNull String hash) {
        checkNotNull(hash);
        return getThreadsDatabase().threadDao().getThreadByHash(hash);
    }


    public void updateUser(@NonNull User user) {
        getThreadsDatabase().userDao().updateUser(user);
    }


    public void setDate(@NonNull Thread thread, @NonNull Date date) {
        checkNotNull(thread);
        checkNotNull(date);
        getThreadsDatabase().threadDao().setThreadDate(thread.getIdx(), date.getTime());
    }

    public void resetThreadNumber(@NonNull Thread thread) {
        checkNotNull(thread);
        getThreadsDatabase().threadDao().resetNumber(thread.getIdx());
    }

    public long storeThread(@NonNull Thread thread) {
        checkNotNull(thread);
        return getThreadsDatabase().threadDao().insertThread(thread);
    }


    public void setStatus(@NonNull Thread thread, @NonNull Status status) {
        checkNotNull(thread);
        checkNotNull(status);
        getThreadsDatabase().threadDao().setStatus(thread.getIdx(), status);
    }

    public void setThreadPinned(long idx, boolean pinned) {
        getThreadsDatabase().threadDao().setPinned(idx, pinned);

    }

    public boolean isThreadPinned(long idx) {
        return getThreadsDatabase().threadDao().isPinned(idx);
    }

    public void setCID(@NonNull Thread thread, @NonNull CID cid) {
        checkNotNull(thread);
        checkNotNull(cid);
        Multihash.fromBase58(cid.getCid()); // check if cid  is valid (otherwise exception)
        getThreadsDatabase().threadDao().setCid(thread.getIdx(), cid);

    }

    public void setThreadCID(long idx, @NonNull CID cid) {
        checkNotNull(cid);
        getThreadsDatabase().threadDao().setCid(idx, cid);

    }


    public void setConnected(@NonNull User user, boolean connected) {
        checkNotNull(user);
        setUserConnected(user.getPID(), connected);
    }

    public void setUserConnected(@NonNull PID user, boolean connected) {
        checkNotNull(user);
        getThreadsDatabase().userDao().setConnected(user.getPid(), connected);
    }

    public void setUserAutoConnect(@NonNull PID user, boolean autoConnect) {
        checkNotNull(user);
        getThreadsDatabase().userDao().setAutoConnect(user.getPid(), autoConnect);
    }

    public void setUserDialing(@NonNull PID user, boolean dialing) {
        checkNotNull(user);
        getThreadsDatabase().userDao().setUserDialing(user.getPid(), dialing);
    }

    public List<User> getAutoConnectUsers() {
        return getThreadsDatabase().userDao().getAutoConnectUsers(true);
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
        return getThreadsDatabase().userDao().getUserDialing(pid);
    }


    public boolean isUserConnected(@NonNull PID user) {
        checkNotNull(user);
        return isUserConnected(user.getPid());
    }

    public boolean isUserConnected(@NonNull String pid) {
        checkNotNull(pid);
        return getThreadsDatabase().userDao().isConnected(pid);
    }

    public boolean isUserAutoConnect(@NonNull PID user) {
        checkNotNull(user);
        return isUserAutoConnect(user.getPid());
    }

    public boolean isUserAutoConnect(@NonNull String pid) {
        checkNotNull(pid);
        return getThreadsDatabase().userDao().isAutoConnect(pid);
    }

    public void resetThreadsPublishing() {
        getThreadsDatabase().threadDao().resetThreadsPublishing();
    }


    public void resetThreadsLeaching() {
        getThreadsDatabase().threadDao().resetThreadsLeaching();
    }

    public void resetUsersDialing() {
        getThreadsDatabase().userDao().resetUsersDialing();
    }

    public void resetUsersConnected() {
        getThreadsDatabase().userDao().resetUsersConnected();
    }

    public void resetPeersConnected() {
        getPeersDatabase().peersDao().resetPeersConnected();
    }


    @NonNull
    public Status getStatus(@NonNull Thread thread) {
        checkNotNull(thread);
        return getThreadsDatabase().threadDao().getStatus(thread.getIdx());
    }

    public void setTimestamp(@NonNull Peer peer, long timestamp) {
        checkNotNull(peer);
        getPeersDatabase().peersDao().setTimestamp(peer.getPid(), timestamp);
    }


    public boolean getThreadMarkedFlag(long idx) {
        return getThreadsDatabase().threadDao().getMarkedFlag(idx);
    }

    public void setThreadMarkedFlag(long idx, boolean flag) {
        getThreadsDatabase().threadDao().setMarkedFlag(idx, flag);
    }

    @NonNull
    public List<Thread> getPinnedThreads() {
        return getThreadsDatabase().threadDao().getThreadsByPinned(true);
    }

    @NonNull
    public List<Thread> getThreadsByDate(long date) {
        return getThreadsDatabase().threadDao().getThreadsByDate(date);
    }


    private boolean existsSameThread(@NonNull Thread thread) {
        checkNotNull(thread);
        boolean result = false;
        List<Thread> notes = getThreadsByDate(thread.getDate());
        for (Thread cmp : notes) {
            if (thread.sameThread(cmp)) {
                result = true;
                break;
            }
        }
        return result;
    }


    @Nullable
    public Status getThreadStatus(long idx) {
        return getThreadsDatabase().threadDao().getStatus(idx);
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
        getThreadsDatabase().userDao().setPublicKey(pid, publicKey);
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
        return getThreadsDatabase().userDao().getPublicKey(pid);
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
        return getThreadsDatabase().userDao().getAlias(pid);
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
        getThreadsDatabase().userDao().setAlias(pid, alias);
    }


    public void setUserImage(@NonNull PID user, @NonNull CID image) {
        checkNotNull(user);
        checkNotNull(image);
        setUserImage(user.getPid(), image);
    }

    public void setUserImage(@NonNull String pid, @NonNull CID image) {
        checkNotNull(pid);
        checkNotNull(image);
        getThreadsDatabase().userDao().setImage(pid, image);
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
        getThreadsDatabase().userDao().setUserType(pid, type);
    }


    public void storeThreads(@NonNull List<Thread> threads) {
        checkNotNull(threads);
        getThreadsDatabase().threadDao().insertThreads(Iterables.toArray(threads, Thread.class));
    }


    public void updateThread(@NonNull Thread thread) {
        checkNotNull(thread);
        getThreadsDatabase().threadDao().updateThreads((Thread) thread);
    }

    @NonNull
    public List<Thread> getThreadsByThread(long thread) {
        return getThreadsDatabase().threadDao().getThreadsByThread(thread);
    }

    public int getThreadReferences(long thread) {
        return getThreadsDatabase().threadDao().getThreadReferences(thread);
    }

    @Nullable
    public Thread getThreadByIdx(long idx) {
        return getThreadsDatabase().threadDao().getThreadByIdx(idx);
    }

    @Nullable
    public Thread getThreadByTimestamp(long timestamp) {
        return getThreadsDatabase().threadDao().getThreadByTimestamp(timestamp);
    }


    public List<Thread> getThreadByIdxs(long... idx) {
        return getThreadsDatabase().threadDao().getThreadByIdxs(idx);
    }


    @Nullable
    public CID getThreadCID(long idx) {
        return getThreadsDatabase().threadDao().getCID(idx);
    }


    public void blockUser(@NonNull PID user) {
        checkNotNull(user);
        getThreadsDatabase().userDao().setBlocked(user.getPid(), true);
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
        getThreadsDatabase().userDao().setBlocked(user.getPid(), false);
    }


    @NonNull
    public List<Thread> getThreadsByThreadStatus(@NonNull Status status) {
        checkNotNull(status);
        return getThreadsDatabase().threadDao().getThreadsByStatus(status);
    }


    @NonNull
    public List<Thread> getThreadsByKindAndThreadStatus(@NonNull Kind kind, @NonNull Status status) {
        checkNotNull(kind);
        checkNotNull(status);
        return getThreadsDatabase().threadDao().getThreadsByKindAndThreadStatus(kind, status);
    }




    @NonNull
    public Hash createHash(@NonNull String hash) {
        checkNotNull(hash);
        return Hash.create(hash, System.currentTimeMillis());
    }


    @NonNull
    private Thread createThread(@NonNull Status status,
                                @NonNull Kind kind,
                                @NonNull PID senderPid,
                                @NonNull String senderAlias,
                                @NonNull String senderKey,
                                @NonNull String sesKey,
                                @NonNull Date date,
                                long thread) {
        return Thread.createThread(status,
                senderPid, senderAlias, senderKey,
                sesKey, kind, date.getTime(), thread);
    }


    @NonNull
    public List<PID> getUsersPIDs() {
        List<PID> result = new ArrayList<>();
        List<String> pids = getThreadsDatabase().userDao().getUserPids();
        for (String pid : pids) {
            result.add(PID.create(pid));
        }
        return result;
    }

    public boolean insertPeerInfo(@NonNull Context context,
                                  @NonNull EntityService entityService,
                                  @NonNull threads.core.api.PeerInfo peer) {
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


    @NonNull
    public List<Thread> getThreadsByCIDAndThread(@NonNull CID cid, long thread) {
        checkNotNull(cid);
        return getThreadsDatabase().threadDao().getThreadsByCidAndThread(cid, thread);
    }

    @NonNull
    public List<Thread> getThreadsByCID(@NonNull CID cid) {
        checkNotNull(cid);
        return getThreadsDatabase().threadDao().getThreadsByCid(cid);
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
