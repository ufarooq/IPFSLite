package threads.server.core.threads;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.collect.Iterables;

import java.util.Date;
import java.util.List;

import threads.ipfs.CID;
import threads.ipfs.IPFS;
import threads.ipfs.Multihash;
import threads.ipfs.PID;

import static androidx.core.util.Preconditions.checkArgument;
import static androidx.core.util.Preconditions.checkNotNull;


public class ThreadsAPI {
    private final static String TAG = ThreadsAPI.class.getSimpleName();

    private final ThreadsDatabase threadsDatabase;

    ThreadsAPI(@NonNull ThreadsDatabase threadsDatabase) {
        checkNotNull(threadsDatabase);
        this.threadsDatabase = threadsDatabase;
    }


    @NonNull
    public ThreadsDatabase getThreadsDatabase() {
        return threadsDatabase;
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
        getThreadsDatabase().threadDao().setThumbnail(thread.getIdx(), image);
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


    @NonNull
    public Thread createThread(@NonNull PID creatorPid,
                               @NonNull String alias,
                               @NonNull Status status,
                               @NonNull Kind kind,
                               long thread) {

        checkNotNull(creatorPid);
        checkNotNull(alias);
        checkNotNull(status);
        checkNotNull(kind);
        checkArgument(thread >= 0);
        return createThread(
                status,
                kind,
                creatorPid,
                alias,
                thread);

    }


    private boolean isReferenced(@NonNull CID cid) {
        checkNotNull(cid);
        return getThreadsDatabase().threadDao().references(cid) > 0;
    }

    private void removeThread(@NonNull IPFS ipfs, @NonNull Thread thread) {
        checkNotNull(ipfs);
        checkNotNull(thread);


        getThreadsDatabase().threadDao().removeThreads(thread);


        unpin(ipfs, thread.getContent());
        unpin(ipfs, thread.getThumbnail());

        // delete all children
        List<Thread> entries = getChildren(thread.getIdx());
        for (Thread entry : entries) {
            removeThread(ipfs, entry);
        }
    }

    private void unpin(@NonNull IPFS ipfs, @Nullable CID cid) {
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


    public List<Thread> getNewestThreadsByStatus(@NonNull Status status, int limit) {
        return getThreadsDatabase().threadDao().getNewestThreadsByStatus(status, limit);
    }

    public List<Thread> getExpiredThreads() {
        return getThreadsDatabase().threadDao().getExpiredThreads(System.currentTimeMillis());
    }


    public void removeThreads(@NonNull IPFS ipfs, long... idxs) {
        checkNotNull(ipfs);
        List<Thread> threads = getThreadsByIdx(idxs);
        for (Thread thread : threads) {
            removeThread(ipfs, thread);
        }
    }

    private void rm(@NonNull IPFS ipfs, @NonNull CID cid) {
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


    public void setDate(@NonNull Thread thread, @NonNull Date date) {
        checkNotNull(thread);
        checkNotNull(date);
        getThreadsDatabase().threadDao().setThreadLastModified(thread.getIdx(), date.getTime());
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

    public void setThreadName(long idx, @NonNull String name) {
        checkNotNull(name);
        getThreadsDatabase().threadDao().setName(idx, name);
    }

    public void setThreadContent(long idx, @NonNull CID cid) {
        checkNotNull(cid);
        Multihash.fromBase58(cid.getCid());
        getThreadsDatabase().threadDao().setContent(idx, cid);
    }

    public void setThreadThumbnail(long idx, @NonNull CID image) {
        checkNotNull(image);
        getThreadsDatabase().threadDao().setThumbnail(idx, image);
    }

    public void resetThreadsPublishing() {
        getThreadsDatabase().threadDao().resetThreadsPublishing();
    }


    public void resetThreadsLeaching() {
        getThreadsDatabase().threadDao().resetThreadsLeaching();
    }


    @NonNull
    public Status getStatus(@NonNull Thread thread) {
        checkNotNull(thread);
        return getThreadsDatabase().threadDao().getStatus(thread.getIdx());
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
        return getThreadsDatabase().threadDao().getThreadsByLastModified(date);
    }


    @Nullable
    public Status getThreadStatus(long idx) {
        return getThreadsDatabase().threadDao().getStatus(idx);
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
    private List<Thread> getChildren(long thread) {
        return getThreadsDatabase().threadDao().getChildren(thread);
    }

    @NonNull
    public List<Thread> getChildrenByStatus(long thread, @NonNull Status status) {
        checkNotNull(status);
        return getThreadsDatabase().threadDao().getChildrenByStatus(thread, status);
    }

    public int getThreadReferences(long thread) {
        return getThreadsDatabase().threadDao().getThreadReferences(thread);
    }

    @Nullable
    public Thread getThreadByIdx(long idx) {
        return getThreadsDatabase().threadDao().getThreadByIdx(idx);
    }


    public List<Thread> getThreadsByIdx(long... idx) {
        return getThreadsDatabase().threadDao().getThreadsByIdx(idx);
    }


    @Nullable
    public CID getThreadContent(long idx) {
        return getThreadsDatabase().threadDao().getContent(idx);
    }

    @Nullable
    public CID getThreadThumbnail(long idx) {
        return getThreadsDatabase().threadDao().getThumbnail(idx);
    }

    @NonNull
    public List<Thread> getThreadsByStatus(@NonNull Status status) {
        checkNotNull(status);
        return getThreadsDatabase().threadDao().getThreadsByStatus(status);
    }


    @NonNull
    public List<Thread> getThreadsByKindAndStatus(@NonNull Kind kind, @NonNull Status status) {
        checkNotNull(kind);
        checkNotNull(status);
        return getThreadsDatabase().threadDao().getThreadsByKindAndStatus(kind, status);
    }

    @NonNull
    private Thread createThread(@NonNull Status status,
                                @NonNull Kind kind,
                                @NonNull PID senderPid,
                                @NonNull String senderAlias,
                                long thread) {
        return Thread.createThread(status, senderPid, senderAlias, kind, thread);
    }


    @NonNull
    public List<Thread> getThreadsByCIDAndParent(@NonNull CID cid, long thread) {
        checkNotNull(cid);
        return getThreadsDatabase().threadDao().getThreadsByCidAndParent(cid, thread);
    }

    @NonNull
    public List<Thread> getThreadsByCID(@NonNull CID cid) {
        checkNotNull(cid);
        return getThreadsDatabase().threadDao().getThreadsByCid(cid);
    }

    public List<Thread> getThreadsByQuery(Status status, String query) {
        checkNotNull(status);
        checkNotNull(query);
        String searchQuery = query.trim();
        if (!searchQuery.startsWith("%")) {
            searchQuery = "%" + searchQuery;
        }
        if (!searchQuery.endsWith("%")) {
            searchQuery = searchQuery + "%";
        }
        return getThreadsDatabase().threadDao().getThreadsByQuery(status, searchQuery);
    }

    public void setThreadProgress(long idx, int progress) {
        checkArgument(progress >= 0 && progress <= 100);
        getThreadsDatabase().threadDao().setProgress(idx, progress);
    }
}
