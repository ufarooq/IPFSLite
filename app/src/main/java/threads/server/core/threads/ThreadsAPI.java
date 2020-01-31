package threads.server.core.threads;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.collect.Iterables;

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


    public void setThreadsDeleting(long... idxs) {
        getThreadsDatabase().threadDao().setThreadsDeleting(idxs);
    }

    public void setThreadLeaching(long idx, boolean leaching) {
        getThreadsDatabase().threadDao().setLeaching(idx, leaching);
    }

    public void setThreadSeeding(long idx) {
        getThreadsDatabase().threadDao().setSeeding(idx);
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


    @Nullable
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

    public void setThreadMimeType(long idx, @NonNull String mimeType) {
        checkNotNull(mimeType);
        getThreadsDatabase().threadDao().setMimeType(idx, mimeType);
    }

    @NonNull
    public Thread createThread(@NonNull PID creatorPid,
                               @NonNull String alias,
                               long parent) {

        checkNotNull(creatorPid);
        checkNotNull(alias);
        checkArgument(parent >= 0);
        return Thread.createThread(creatorPid, alias, parent);

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


    public List<Thread> getNewestSeedingThreads(int limit) {
        return getThreadsDatabase().threadDao().getNewestSeedingThreads(limit);
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
        getThreadsDatabase().threadDao().resetThreadsNumber(idxs);
    }

    public void resetParentThreadsNumber(long parent) {
        getThreadsDatabase().threadDao().resetParentThreadsNumber(parent);
    }

    public void resetThreadsNumber() {
        getThreadsDatabase().threadDao().resetThreadsNumber();
    }


    public long storeThread(@NonNull Thread thread) {
        checkNotNull(thread);
        return getThreadsDatabase().threadDao().insertThread(thread);
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
    public List<Thread> getPinnedThreads() {
        return getThreadsDatabase().threadDao().getThreadsByPinned(true);
    }

    @NonNull
    public List<Thread> getThreadsByDate(long date) {
        return getThreadsDatabase().threadDao().getThreadsByLastModified(date);
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
    public List<Thread> getChildren(long thread) {
        return getThreadsDatabase().threadDao().getChildren(thread);
    }

    @NonNull
    public List<Thread> getSeedingChildren(long thread) {
        return getThreadsDatabase().threadDao().getSeedingChildren(thread);
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
    public List<Thread> getThreadsByContentAndParent(@NonNull CID cid, long thread) {
        checkNotNull(cid);
        return getThreadsDatabase().threadDao().getThreadsByContentAndParent(cid, thread);
    }

    @NonNull
    public List<Thread> getThreadsByContent(@NonNull CID cid) {
        checkNotNull(cid);
        return getThreadsDatabase().threadDao().getThreadsByContent(cid);
    }

    public List<Thread> getSeedingThreadsByQuery(String query) {

        checkNotNull(query);
        String searchQuery = query.trim();
        if (!searchQuery.startsWith("%")) {
            searchQuery = "%" + searchQuery;
        }
        if (!searchQuery.endsWith("%")) {
            searchQuery = searchQuery + "%";
        }
        return getThreadsDatabase().threadDao().getSeedingThreadsByQuery(searchQuery);
    }

    public void setThreadProgress(long idx, int progress) {
        checkArgument(progress >= 0 && progress <= 100);
        getThreadsDatabase().threadDao().setProgress(idx, progress);
    }

    public void setThreadSize(long idx, long size) {
        getThreadsDatabase().threadDao().setSize(idx, size);
    }
}
