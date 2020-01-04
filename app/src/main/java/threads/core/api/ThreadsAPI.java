package threads.core.api;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.gson.Gson;

import org.iota.jota.utils.Constants;
import org.iota.jota.utils.TrytesConverter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import threads.core.MimeType;
import threads.core.Preferences;
import threads.core.THREADS;
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
    private final EntityService entityService;

    public ThreadsAPI(@NonNull ThreadsDatabase threadsDatabase,
                      @NonNull EventsDatabase eventsDatabase,
                      @NonNull PeersInfoDatabase peersInfoDatabase,
                      @NonNull PeersDatabase peersDatabase,
                      @NonNull EntityService entityService) {
        checkNotNull(threadsDatabase);
        checkNotNull(eventsDatabase);
        checkNotNull(peersInfoDatabase);
        checkNotNull(peersDatabase);
        checkNotNull(entityService);
        this.threadsDatabase = threadsDatabase;
        this.eventsDatabase = eventsDatabase;
        this.peersInfoDatabase = peersInfoDatabase;
        this.peersDatabase = peersDatabase;
        this.entityService = entityService;
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


    /**
     * Generates a random address
     *
     * @return a random address for a specific address usage
     */
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


    public void setNoteLeaching(long idx, boolean leaching) {
        getThreadsDatabase().noteDao().setLeaching(idx, leaching);
    }

    public void setNotePublishing(long idx, boolean publish) {
        getThreadsDatabase().noteDao().setPublishing(idx, publish);
    }

    public void setNotesLeaching(boolean publish, long... idxs) {
        getThreadsDatabase().noteDao().setNotesLeaching(publish, idxs);
    }

    public void setNotesPublishing(boolean publish, long... idxs) {
        getThreadsDatabase().noteDao().setNotesPublishing(publish, idxs);
    }

    public void setThreadStatus(@NonNull Status oldStatus, @NonNull Status newStatus) {
        checkNotNull(oldStatus);
        getThreadsDatabase().threadDao().setStatus(oldStatus, newStatus);
    }

    public void setNoteStatus(@NonNull Status oldStatus, @NonNull Status newStatus) {
        checkNotNull(oldStatus);
        getThreadsDatabase().noteDao().setStatus(oldStatus, newStatus);
    }

    public void setNoteStatus(long idx, @NonNull Status status) {
        checkNotNull(status);
        getThreadsDatabase().noteDao().setStatus(idx, status);
    }

    public void setNotesStatus(@NonNull Status status, long... idxs) {
        checkNotNull(status);
        getThreadsDatabase().noteDao().setNotesStatus(status, idxs);
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

    public void setImage(@NonNull Note note, @NonNull CID image) {
        checkNotNull(note);
        checkNotNull(image);
        getThreadsDatabase().noteDao().setImage(note.getIdx(), image);
    }

    @NonNull
    public String getMimeType(@NonNull Thread thread) {
        checkNotNull(thread);
        return getThreadsDatabase().threadDao().getMimeType(thread.getIdx());
    }

    @NonNull
    public String getMimeType(@NonNull Note note) {
        checkNotNull(note);
        return getThreadsDatabase().noteDao().getMimeType(note.getIdx());
    }


    @Nullable
    public String getThreadMimeType(long idx) {
        return getThreadsDatabase().threadDao().getMimeType(idx);
    }

    @Nullable
    public String getNoteMimeType(long idx) {
        return getThreadsDatabase().noteDao().getMimeType(idx);
    }

    public void setThreadSenderAlias(@NonNull PID pid, @NonNull String alias) {
        checkNotNull(pid);
        checkNotNull(alias);
        getThreadsDatabase().threadDao().setSenderAlias(pid, alias);
    }

    public void setNoteSenderAlias(@NonNull PID pid, @NonNull String alias) {
        checkNotNull(pid);
        checkNotNull(alias);
        getThreadsDatabase().noteDao().setSenderAlias(pid, alias);
    }

    public void setMimeType(@NonNull Thread thread, @NonNull String mimeType) {
        checkNotNull(thread);
        checkNotNull(mimeType);
        getThreadsDatabase().threadDao().setMimeType(thread.getIdx(), mimeType);
    }

    public void setMimeType(@NonNull Note note, @NonNull String mimeType) {
        checkNotNull(note);
        checkNotNull(mimeType);
        getThreadsDatabase().noteDao().setMimeType(note.getIdx(), mimeType);
    }


    @NonNull
    public List<Note> getNotes(@NonNull Thread thread) {
        checkNotNull(thread);
        return getNotesByThread(thread.getIdx());
    }

    @NonNull
    private List<Note> getNotesByThread(long thread) {
        return getThreadsDatabase().noteDao().getNotesByThread(thread);
    }

    @NonNull
    public List<Note> loadNotes(@NonNull Context context,
                                @NonNull User user,
                                @NonNull Status status) {
        checkNotNull(context);
        checkNotNull(user);
        checkNotNull(status);

        List<Note> notes = new ArrayList<>();

        List<Thread> threads = getThreadsByThreadStatus(status);

        for (Thread thread : threads) {
            notes.addAll(loadNotes(context, thread));
        }
        return notes;
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

    public List<Thread> loadPublishedThreads(@NonNull Context context,
                                             @NonNull String aesKey,
                                             @NonNull String address) {
        checkNotNull(context);
        checkNotNull(address);
        checkNotNull(aesKey);

        List<Thread> threads = new ArrayList<>();
        try {
            List<Entity> entities = getEntityService().loadEntities(context, address);
            for (Entity entity : entities) {
                Thread thread = ThreadDecoder.convert(entity, aesKey);
                if (thread != null) {
                    if (isUserBlocked(thread.getSenderPid())) {
                        continue;
                    }

                    if (existsSameThread(thread)) { // maybe not yet necessary
                        continue;
                    }

                    long today = THREADS.getToday().getTime();
                    if (thread.getExpireDate() > today) {
                        threads.add(thread);
                    }
                }
            }

        } catch (Throwable e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }

        return threads;

    }

    public int getThreadRequestNoteTransactionSize(
            @NonNull User user, @NonNull Note note, boolean addImage, boolean addTitle) {
        try {
            Thread thread = getThread(note);
            checkNotNull(thread);
            Content content = NoteRequestEncoder.convert(thread, note, user.getPublicKey(),
                    addImage, addTitle);
            checkNotNull(content);
            String data = gson.toJson(content);
            int length = 1;
            String trytes = TrytesConverter.asciiToTrytes(data);
            length += Math.floor(trytes.length() / Constants.MESSAGE_LENGTH);
            return length;

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
        return Integer.MAX_VALUE;
    }

    public boolean insertThreadRequestNote(@NonNull Context context,
                                           @NonNull User user,
                                           @NonNull Note note,
                                           boolean addImage,
                                           boolean addTitle) {
        checkNotNull(context);
        checkNotNull(user);
        checkNotNull(note);

        checkArgument(note.getKind() == Kind.OUT);
        checkArgument(note.getNoteType() == NoteType.THREAD_REQUEST);
        checkArgument(!user.getPid().isEmpty());

        try {
            Thread thread = getThread(note);
            checkNotNull(thread);
            Content content = NoteRequestEncoder.convert(thread, note, user.getPublicKey(),
                    addImage, addTitle);
            checkNotNull(content);
            String address = AddressType.getAddress(user.getPID(), AddressType.INBOX);
            checkNotNull(address);
            Entity entity = getEntityService().insertData(context, address, gson.toJson(content));
            checkNotNull(entity);

            String hash = entity.getHash();
            setHash(note, hash);

            return true;
        } catch (Throwable e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
            return false;
        }
    }

    public boolean insertThread(@NonNull Context context,
                                @NonNull Thread thread,
                                @NonNull String address,
                                @NonNull String aesKey) {
        checkNotNull(context);
        checkNotNull(thread);
        checkNotNull(address);
        checkNotNull(aesKey);


        try {

            String dataTransaction = ThreadEncoder.convert(thread, aesKey);

            Entity entity = getEntityService().insertData(context, address, dataTransaction);
            checkNotNull(entity);

            String hash = entity.getHash();
            setHash(thread, hash);
            return true;
        } catch (Throwable e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
            return false;
        }
    }


    public boolean insertThreadPublishNote(@NonNull Context context,
                                           @NonNull Note note,
                                           @NonNull String address,
                                           @NonNull String aesKey) {
        checkNotNull(context);
        checkNotNull(note);
        checkNotNull(address);
        checkNotNull(aesKey);


        checkArgument(note.getKind() == Kind.OUT);
        checkArgument(note.getNoteType() == NoteType.THREAD_PUBLISH);

        try {
            Thread thread = getThread(note);
            checkNotNull(thread);

            String dataTransaction = ThreadEncoder.convert(thread, aesKey);

            Entity entity = getEntityService().insertData(context, address, dataTransaction);
            checkNotNull(entity);

            String hash = entity.getHash();
            setHash(note, hash);
            return true;
        } catch (Throwable e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
            return false;
        }
    }


    public boolean insertNote(@NonNull Context context,
                              @NonNull Note note) {
        checkNotNull(context);
        checkNotNull(note);

        checkArgument(note.getKind() == Kind.OUT);
        checkArgument(note.getNoteType() != NoteType.THREAD_REQUEST);
        checkArgument(note.getNoteType() != NoteType.THREAD_PUBLISH);
        checkArgument(note.getNoteType() != NoteType.INFO);

        try {

            Thread thread = getThread(note);
            checkNotNull(thread);
            CID cid = thread.getCid();
            checkNotNull(cid);
            String address = THREADS.getAddress(cid);

            Content content = NoteEncoder.convert(thread, note);

            Entity entity = getEntityService().insertData(context, address, gson.toJson(content));
            checkNotNull(entity);

            String hash = entity.getHash();
            setHash(note, hash);
            return true;
        } catch (Throwable e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
            return false;
        }
    }

    public boolean publishNote(@NonNull IPFS ipfs, @NonNull String topic, @NonNull Note note) {
        checkNotNull(ipfs);
        checkNotNull(topic);
        checkNotNull(note);

        checkArgument(note.getKind() == Kind.OUT);
        checkArgument(note.getNoteType() != NoteType.THREAD_REQUEST);
        checkArgument(note.getNoteType() != NoteType.THREAD_PUBLISH);
        checkArgument(note.getNoteType() != NoteType.INFO);

        try {
            Thread thread = getThread(note);
            checkNotNull(thread);
            Content content = NoteEncoder.convert(thread, note);
            String message = gson.toJson(content);
            ipfs.pubsubPub(topic, message, 50);
            return true;
        } catch (Throwable e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
            return false;
        }
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


    /**
     * Thread request note to the given sender
     *
     * @param sender Sender user which creates the thread
     * @param thread Thread the message belongs too
     * @return a note to the sender which is not yet added to the database and
     * not insert into the tangle
     */
    @NonNull
    public Note createThreadRequestNote(@NonNull User sender, @NonNull Thread thread) {
        checkNotNull(sender);
        checkNotNull(thread);

        return createNote(
                thread.getIdx(),
                sender.getPID(),
                sender.getAlias(),
                sender.getPublicKey(),
                thread.getSesKey(),
                NoteType.THREAD_REQUEST,
                null,
                MimeType.PLAIN_MIME_TYPE,
                new Date());
    }


    @NonNull
    public Note createThreadRejectNote(@NonNull User sender,
                                       @NonNull Thread thread) {
        checkNotNull(sender);
        checkNotNull(thread);

        return createNote(
                thread.getIdx(),
                sender.getPID(),
                sender.getAlias(),
                sender.getPublicKey(),
                thread.getSesKey(),
                NoteType.THREAD_REJECT,
                null,
                MimeType.PLAIN_MIME_TYPE,
                new Date());

    }


    @NonNull
    public Note createThreadJoinNote(@NonNull User sender,
                                     @NonNull Thread thread) {
        checkNotNull(sender);
        checkNotNull(thread);

        return createNote(
                thread.getIdx(),
                sender.getPID(),
                sender.getAlias(),
                sender.getPublicKey(),
                thread.getSesKey(),
                NoteType.THREAD_JOIN,
                null,
                MimeType.PLAIN_MIME_TYPE,
                new Date());

    }


    @NonNull
    public Note createThreadPublishNote(@NonNull User sender, @NonNull Thread thread) {
        checkNotNull(sender);
        checkNotNull(thread);

        return createNote(
                thread.getIdx(),
                sender.getPID(),
                sender.getAlias(),
                sender.getPublicKey(),
                thread.getSesKey(),
                NoteType.THREAD_PUBLISH,
                thread.getCid(),
                MimeType.PLAIN_MIME_TYPE,
                new Date());

    }


    @NonNull
    public Note createThreadLeaveNote(@NonNull User sender, @NonNull Thread thread) {
        checkNotNull(sender);
        checkNotNull(thread);


        return createNote(
                thread.getIdx(),
                sender.getPID(),
                sender.getAlias(),
                sender.getPublicKey(),
                thread.getSesKey(),
                NoteType.THREAD_LEAVE,
                null,
                MimeType.PLAIN_MIME_TYPE,
                new Date());

    }


    @NonNull
    public Note createHtmlNote(@NonNull User sender, @NonNull Thread thread, @NonNull CID html) {
        checkNotNull(sender);
        checkNotNull(thread);
        checkNotNull(html);

        return createNote(
                thread.getIdx(),
                sender.getPID(),
                sender.getAlias(),
                sender.getPublicKey(),
                thread.getSesKey(),
                NoteType.HTML,
                html,
                MimeType.HTML_MIME_TYPE,
                new Date());

    }


    @NonNull
    public Note createMessageNote(@NonNull User sender,
                                  @NonNull Thread thread,
                                  @NonNull CID cid) {
        checkNotNull(sender);
        checkNotNull(thread);


        return createNote(
                thread.getIdx(),
                sender.getPID(),
                sender.getAlias(),
                sender.getPublicKey(),
                thread.getSesKey(),
                NoteType.MESSAGE,
                cid,
                MimeType.PLAIN_MIME_TYPE,
                new Date());

    }

    @NonNull
    public Note createVideoCallNote(@NonNull User sender, @NonNull Thread thread) {
        checkNotNull(sender);
        checkNotNull(thread);


        return createNote(
                thread.getIdx(),
                sender.getPID(),
                sender.getAlias(),
                sender.getPublicKey(),
                thread.getSesKey(),
                NoteType.VIDEO_CALL,
                null,
                MimeType.PLAIN_MIME_TYPE,
                new Date());

    }

    @NonNull
    public Note createCallNote(@NonNull User sender, @NonNull Thread thread) {
        checkNotNull(sender);
        checkNotNull(thread);


        return createNote(
                thread.getIdx(),
                sender.getPID(),
                sender.getAlias(),
                sender.getPublicKey(),
                thread.getSesKey(),
                NoteType.CALL,
                null,
                MimeType.PLAIN_MIME_TYPE,
                new Date());

    }

    @NonNull
    public Note createLocationNote(@NonNull User sender,
                                   @NonNull Thread thread,
                                   double latitude,
                                   double longitude,
                                   double zoom) {
        checkNotNull(sender);
        checkNotNull(thread);

        Note note = createNote(
                thread.getIdx(),
                sender.getPID(),
                sender.getAlias(),
                sender.getPublicKey(),
                thread.getSesKey(),
                NoteType.LOCATION,
                null,
                MimeType.GEO_MIME_TYPE,
                new Date());

        note.addAdditional(Preferences.LATITUDE, String.valueOf(latitude), false);
        note.addAdditional(Preferences.LONGITUDE, String.valueOf(longitude), false);
        note.addAdditional(Preferences.ZOOM, String.valueOf(zoom), false);

        return note;
    }

    @NonNull
    public Note createLinkNote(@NonNull User sender,
                               @NonNull Thread thread,
                               @NonNull LinkType linkType,
                               @Nullable CID cid,
                               @NonNull String fileName,
                               @NonNull String mimeType,
                               long fileSize) {
        checkNotNull(sender);
        checkNotNull(thread);
        checkNotNull(linkType);
        checkNotNull(fileName);
        checkNotNull(mimeType);

        Note note = createNote(
                thread.getIdx(),
                sender.getPID(),
                sender.getAlias(),
                sender.getPublicKey(),
                thread.getSesKey(),
                NoteType.LINK,
                cid,
                mimeType,
                new Date());
        note.addAdditional(LinkType.class.getSimpleName(), linkType.name(), false);
        note.addAdditional(Content.FILENAME, fileName, false);
        note.addAdditional(Content.FILESIZE, String.valueOf(fileSize), false);

        return note;
    }


    @NonNull
    public Note createDataNote(@NonNull User sender,
                               @NonNull Thread thread,
                               @NonNull String mimeType,
                               @Nullable CID cid) {
        checkNotNull(sender);
        checkNotNull(thread);
        checkNotNull(mimeType);

        return createNote(thread.getIdx(),
                sender.getPID(),
                sender.getAlias(),
                sender.getPublicKey(),
                thread.getSesKey(),
                NoteType.DATA,
                cid,
                mimeType,
                new Date());

    }


    @NonNull
    public Note createAudioNote(@NonNull User sender,
                                @NonNull Thread thread,
                                @NonNull String mimeType,
                                @Nullable CID cid) {
        checkNotNull(sender);
        checkNotNull(thread);
        checkNotNull(mimeType);

        return createNote(thread.getIdx(),
                sender.getPID(),
                sender.getAlias(),
                sender.getPublicKey(),
                thread.getSesKey(),
                NoteType.AUDIO,
                cid,
                mimeType,
                new Date());

    }


    @NonNull
    public Note createInfoNote(@NonNull Thread thread,
                               @NonNull String info,
                               @NonNull Date date) {
        checkNotNull(thread);

        Note note = createNote(
                thread.getIdx(),
                thread.getSenderPid(),
                thread.getSenderAlias(),
                thread.getSenderKey(),
                thread.getSesKey(),
                NoteType.INFO,
                null,
                MimeType.PLAIN_MIME_TYPE,
                date);
        note.addAdditional(Content.TEXT, info, true);
        return note;
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

    public void setAdditional(@NonNull Note note,
                              @NonNull String key,
                              @NonNull String value,
                              boolean internal) {
        checkNotNull(note);
        checkNotNull(key);
        checkNotNull(value);
        Note update = getNoteByIdx(note.getIdx());
        checkNotNull(update);
        update.addAdditional(key, value, internal);
        updateNote(update);
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
        counter += getThreadsDatabase().noteDao().references(cid);
        counter += getPeersDatabase().peersDao().references(cid);
        return counter > 0;
    }

    public void removeThread(@NonNull IPFS ipfs, @NonNull Thread thread) {
        checkNotNull(ipfs);
        checkNotNull(thread);


        // delete thread notes children
        removeThreadNotes(ipfs, thread);

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

    /**
     * Utility function to remove a note from DB and from local RELAY
     *
     * @param ipfs RELAY client
     * @param note Note object
     */
    public void removeNote(@NonNull IPFS ipfs, @NonNull Note note) {
        checkNotNull(ipfs);
        checkNotNull(note);

        getThreadsDatabase().noteDao().removeNote(note);

        unpin(ipfs, note.getCid());
        unpin(ipfs, note.getImage());

    }

    public void insertHash(@NonNull Hash hash) {
        checkNotNull(hash);
        entityService.getHashDatabase().hashDao().insertHash(hash);
    }

    public List<Thread> getExpiredThreads() {
        return getThreadsDatabase().threadDao().getExpiredThreads(System.currentTimeMillis());
    }


    public List<Note> getExpiredNotes() {
        return getThreadsDatabase().noteDao().getExpiredNotes(System.currentTimeMillis());
    }

    @NonNull
    public List<Thread> loadThreadRequests(@NonNull Context context,
                                           @NonNull User user,
                                           @NonNull String privateKey) {

        checkNotNull(context);
        checkNotNull(user);
        checkNotNull(privateKey);

        List<Thread> threads = new ArrayList<>();
        String address = AddressType.getAddress(user.getPID(), AddressType.INBOX);
        checkNotNull(address);
        try {
            List<Entity> entities = getEntityService().loadEntities(context, address);
            for (Entity entity : entities) {
                Thread thread = NoteRequestDecoder.convert(entity, privateKey);
                if (thread != null) {
                    if (isUserBlocked(thread.getSenderPid())) {
                        continue;
                    }

                    if (existsSameThread(thread)) {
                        continue;
                    }
                    threads.add(thread); // handle later
                }
            }

        } catch (Throwable e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }

        return threads;
    }

    @Nullable
    public PeerInfo getPeer(@NonNull Context context, @NonNull PID pid) {
        checkNotNull(context);
        checkNotNull(pid);


        PeerInfo peer = loadPeer(context, pid);
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
                             @NonNull PID pid) {
        checkNotNull(context);
        checkNotNull(pid);

        String address = AddressType.getAddress(pid, AddressType.PEER);

        AtomicReference<PeerInfo> reference = new AtomicReference<>(null);

        try {
            List<Entity> entities = getEntityService().loadEntities(context, address);
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


    @NonNull
    public List<Note> loadNotes(@NonNull Context context, @NonNull Thread thread) {

        checkNotNull(context);
        checkNotNull(thread);

        List<Note> notes = new ArrayList<>();
        CID cid = thread.getCid();
        checkNotNull(cid);
        String address = THREADS.getAddress(cid);

        try {
            List<Entity> entities = getEntityService().loadEntities(context, address);
            for (Entity entity : entities) {
                Note note = NoteDecoder.convert(thread, entity);
                if (note != null) {
                    if (isUserBlocked(note.getSenderPid())) {
                        continue;
                    }

                    if (existsSameNote(note)) {
                        continue;
                    }
                    notes.add(note); // handle later
                }
            }

        } catch (Throwable e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
        return notes;
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

        for (Thread thread : threads) {
            removeThreadNotes(ipfs, thread);
        }
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


    public List<PID> getMembers(@NonNull Thread thread) {
        checkNotNull(thread);
        return Lists.newArrayList(thread.getMembers());
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

    public void incrementNoteNumber(@NonNull Note note) {
        checkNotNull(note);
        incrementNotesNumber(note.getIdx());
    }

    public void incrementNotesNumber(long... idxs) {
        getThreadsDatabase().noteDao().incrementNumber(idxs);
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

    public void resetNotesNumber() {
        getThreadsDatabase().noteDao().resetNotesNumber();
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

    @NonNull
    public List<User> getUsers(@NonNull Thread thread) {
        checkNotNull(thread);

        List<User> users = new ArrayList<>();
        for (PID pid : thread.getMembers()) {
            User user = getUserByPID(pid);
            if (user != null) {
                users.add(user);
            }
        }
        return users;
    }

    @NonNull
    public String getFileName(@NonNull Note note) {
        checkNotNull(note);
        return note.getAdditionalValue(Content.FILENAME);
    }


    public long getFileSize(@NonNull Note note) {
        checkNotNull(note);
        return Long.valueOf(note.getAdditionalValue(Content.FILESIZE));
    }

    @NonNull
    public LinkType getLinkNoteLinkType(@NonNull Note note) {
        checkNotNull(note);
        checkArgument(note.getNoteType() == NoteType.LINK);
        return LinkType.valueOf(note.getAdditionalValue(LinkType.class.getSimpleName()));
    }


    private void removeThreadNotes(@NonNull IPFS ipfs, @NonNull Thread thread) {
        checkNotNull(thread);
        checkNotNull(ipfs);

        List<Note> notes = getNotes(thread);
        for (Note note : notes) {
            removeNote(ipfs, note);
        }
    }


    @NonNull
    public List<Note> getNotesByNoteType(@NonNull NoteType type) {
        checkNotNull(type);
        return getThreadsDatabase().noteDao().getNotesByType(type);
    }


    @NonNull
    public List<Note> getNotesByKindAndStatus(@NonNull Kind kind, @NonNull Status status) {
        checkNotNull(kind);
        checkNotNull(status);
        return getThreadsDatabase().noteDao().getNotesByKindAndStatus(kind, status);
    }


    public boolean insertUser(@NonNull Context context,
                              @NonNull User user,
                              @NonNull String address,
                              @NonNull String aesKey) {
        checkNotNull(context);
        checkNotNull(user);
        checkNotNull(aesKey);

        try {
            String dataTransaction = UserEncoder.convert(user, aesKey);


            Entity entity = getEntityService().insertData(context, address, dataTransaction);
            checkNotNull(entity);

            String hash = entity.getHash();
            setHash(user, hash);

            return true;
        } catch (Throwable e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
            return false;
        }
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
    public String getHash(@NonNull Note note) {
        checkNotNull(note);
        return getThreadsDatabase().noteDao().getHash(note.getIdx());
    }

    @Nullable
    public Note getNoteByHash(@NonNull String hash) {
        checkNotNull(hash);
        return getThreadsDatabase().noteDao().getNoteByHash(hash);
    }

    @Nullable
    public Thread getThreadByHash(@NonNull String hash) {
        checkNotNull(hash);
        return getThreadsDatabase().threadDao().getThreadByHash(hash);
    }

    public void setHash(@NonNull Note note, @Nullable String hash) {
        checkNotNull(note);
        note.setHash(hash);
        getThreadsDatabase().noteDao().setHash(note.getIdx(), hash);
    }

    public void updateSettings(@NonNull Settings settings) {
        checkNotNull(settings);
        getThreadsDatabase().settingsDao().updateSettings(settings);
    }

    public boolean hasHash(@NonNull String hash) {
        return entityService.getHashDatabase().hashDao().hasHash(hash) > 0;
    }


    public void removeHash(@NonNull String hash) {
        entityService.getHashDatabase().hashDao().removeHash(hash);
    }

    public void updateUser(@NonNull User user) {
        getThreadsDatabase().userDao().updateUser(user);
    }


    @Nullable
    public PeerInfo getPeerInfoByHash(@NonNull Context context,
                                      @NonNull PID pid,
                                      @NonNull String hash) {
        checkNotNull(context);
        checkNotNull(hash);
        checkNotNull(pid);
        PeerInfo peer = getPeerInfoByHash(hash);

        if (peer != null) {
            return peer;
        } // already loaded

        peer = loadPeerInfoByHash(context, pid, hash);
        if (peer != null) {

            // now we have to check if this peer is newer
            // then the latest store peer
            boolean store = true;
            PeerInfo storedPeer = getPeerInfoByPID(pid);
            if (storedPeer != null) {
                if (peer.getTimestamp() < storedPeer.getTimestamp()) {
                    store = false;
                }
            }

            if (store) {
                storePeerInfo(peer);
            }
        }
        return peer;
    }


    @Nullable
    public PeerInfo loadPeerInfoByHash(@NonNull Context context,
                                       @NonNull PID pid,
                                       @NonNull String hash) {

        checkNotNull(context);
        checkNotNull(pid);
        checkNotNull(hash);
        try {
            Entity entity = getEntityService().loadEntityByHash(context, hash);
            if (entity != null) {
                PeerInfo peer = PeerInfoDecoder.convert(pid, entity);
                if (peer != null) {
                    return peer;
                }
            }
        } catch (Throwable e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
        return null;
    }

    @Nullable
    public User loadUserByHash(@NonNull Context context,
                               @NonNull String hash,
                               @NonNull String aesKey) {

        checkNotNull(context);
        checkNotNull(hash);
        checkNotNull(aesKey);

        try {
            Entity entity = getEntityService().loadEntityByHash(context, hash);
            if (entity != null) {
                User user = UserDecoder.convert(entity, aesKey);
                if (user != null) {
                    return user;
                }
            }
        } catch (Throwable e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
        return null;
    }


    @Nullable
    public User loadUserByAddress(@NonNull Context context,
                                  @NonNull String address,
                                  @NonNull String aesKey) {
        checkNotNull(context);
        checkNotNull(address);
        checkNotNull(aesKey);

        List<User> users = new ArrayList<>();

        try {
            List<Entity> entities = getEntityService().loadEntities(context, address);
            for (Entity entity : entities) {
                User user = UserDecoder.convert(entity, aesKey);
                if (user != null) {
                    users.add(user);
                }
            }
            if (users.isEmpty()) {
                return null;
            }
        } catch (Throwable e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
        return users.get(0);

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

    public void setSenderBlocked(PID sender, boolean blocked) {
        getThreadsDatabase().noteDao().setSenderBlocked(sender, blocked);
        getThreadsDatabase().threadDao().setSenderBlocked(sender, blocked);
    }

    public long storeThread(@NonNull Thread thread) {
        checkNotNull(thread);
        return getThreadsDatabase().threadDao().insertThread(thread);
    }


    public long storeNote(@NonNull Note note) {
        checkNotNull(note);
        return getThreadsDatabase().noteDao().insertNote(note);
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

    public void setNoteCID(long idx, @NonNull CID cid) {
        checkNotNull(cid);
        getThreadsDatabase().noteDao().setCid(idx, cid);

    }

    public void setCID(@NonNull Note note, @NonNull CID cid) {
        checkNotNull(note);
        checkNotNull(cid);
        getThreadsDatabase().noteDao().setCid(note.getIdx(), cid);

    }


    public void setStatus(@NonNull Note note, @NonNull Status status) {
        checkNotNull(note);
        checkNotNull(status);
        getThreadsDatabase().noteDao().setStatus(note.getIdx(), status);
    }


    public void setNoteStatus(@NonNull Status status, long idx) {
        checkNotNull(status);
        getThreadsDatabase().noteDao().setStatus(idx, status);
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

    public void resetNotesPublishing() {
        getThreadsDatabase().noteDao().resetNotesPublishing();
    }

    public void resetNotesLeaching() {
        getThreadsDatabase().noteDao().resetNotesLeaching();
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
    public List<Note> getNotesByDate(long date) {
        return getThreadsDatabase().noteDao().getNotesByDate(date);
    }

    @NonNull
    public List<Thread> getPinnedThreads() {
        return getThreadsDatabase().threadDao().getThreadsByPinned(true);
    }

    @NonNull
    public List<Thread> getThreadsByDate(long date) {
        return getThreadsDatabase().threadDao().getThreadsByDate(date);
    }

    private boolean existsSameNote(@NonNull Note note) {
        checkNotNull(note);
        boolean result = false;
        List<Note> notes = getNotesByDate(note.getDate());
        for (Note cmp : notes) {
            if (note.sameNote(cmp)) {
                result = true;
                break;
            }
        }
        return result;
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

    @NonNull
    public Status getStatus(@NonNull Note note) {
        checkNotNull(note);
        return getThreadsDatabase().noteDao().getStatus(note.getIdx());
    }

    @Nullable
    public Status getNoteStatus(long idx) {
        return getThreadsDatabase().noteDao().getStatus(idx);
    }

    @Nullable
    public Status getThreadStatus(long idx) {
        return getThreadsDatabase().threadDao().getStatus(idx);
    }

    @Nullable
    public NoteType getNoteType(long idx) {
        return getThreadsDatabase().noteDao().getNoteType(idx);
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


    @NonNull
    public Settings createSettings(@NonNull String id) {
        checkNotNull(id);
        return Settings.createSettings(id);
    }


    public void storeNotes(@NonNull List<Note> notes) {
        checkNotNull(notes);
        getThreadsDatabase().noteDao().insertNotes(Iterables.toArray(notes, Note.class));
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


    @Nullable
    public CID getNoteCID(long idx) {
        // TODO bug in generation of database
        Note note = getThreadsDatabase().noteDao().getNoteByIdx(idx);
        if (note != null) {
            return note.getCid();
        }
        return null;
    }

    @Nullable
    public Thread getThread(@NonNull Note note) {
        checkNotNull(note);
        long tidx = note.getThread();

        return getThreadByIdx(tidx);
    }

    @Nullable
    public Note getNoteByIdx(long idx) {
        return getThreadsDatabase().noteDao().getNoteByIdx(idx);
    }


    private void updateNote(@NonNull Note note) {
        checkNotNull(note);
        getThreadsDatabase().noteDao().updateNote(note);
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


    @Nullable

    public Settings getSettings(@NonNull String id) {
        checkNotNull(id);
        return getThreadsDatabase().settingsDao().getSettings(id);
    }


    public void storeSettings(@NonNull Settings settings) {
        checkNotNull(settings);
        getThreadsDatabase().settingsDao().insertSettings(settings);
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


    public void addMember(@NonNull Thread thread, @NonNull PID user) {
        checkNotNull(thread);
        checkNotNull(user);
        thread.addMember(user);
        Thread update = getThreadByIdx(thread.getIdx());
        checkNotNull(update);
        update.addMember(user);
        updateThread(update);
    }

    public void removeMember(@NonNull Thread thread, @NonNull PID user) {
        checkNotNull(thread);
        checkNotNull(user);
        thread.removeMember(user);
        Thread update = getThreadByIdx(thread.getIdx());
        checkNotNull(update);
        update.removeMember(user);
        updateThread(update);
    }

    @NonNull
    public Hash createHash(@NonNull String hash) {
        checkNotNull(hash);
        return Hash.create(hash, System.currentTimeMillis());
    }


    private Note createNote(long thread,
                            @NonNull PID senderPid,
                            @NonNull String senderAuthor,
                            @NonNull String senderKey,
                            @NonNull String sesKey,
                            @NonNull NoteType noteType,
                            @Nullable CID cid,
                            @NonNull String mimeType,
                            @NonNull Date date) {
        Note note = Note.createNote(thread, senderPid, senderAuthor, senderKey,
                sesKey, Status.INIT, Kind.OUT, noteType, mimeType, date.getTime());
        note.setCid(cid);

        return note;
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
    public List<Note> getNotesByCID(@NonNull CID cid) {
        checkNotNull(cid);
        return getThreadsDatabase().noteDao().getNotesByCID(cid);
    }

    @NonNull
    public List<Note> getNotesBySenderPIDAndStatus(@NonNull PID pid, @NonNull Status status) {
        checkNotNull(pid);
        checkNotNull(status);
        return getThreadsDatabase().noteDao().getNotesBySenderPIDAndStatus(pid, status);
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
                                  @NonNull threads.core.api.PeerInfo peer) {
        checkNotNull(context);
        checkNotNull(peer);
        try {

            String address = AddressType.getAddress(peer.getPID(), AddressType.PEER);

            String data = PeerInfoEncoder.convert(peer);

            EntityService entityService = getEntityService();

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
    private EntityService getEntityService() {
        return entityService;
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
