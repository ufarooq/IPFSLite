package threads.server.core.threads;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.common.collect.Iterables;

import java.util.List;

import threads.ipfs.CID;
import threads.ipfs.IPFS;
import threads.ipfs.Multihash;


public class ThreadsAPI {
    private final static String TAG = ThreadsAPI.class.getSimpleName();

    private final ThreadsDatabase threadsDatabase;

    ThreadsAPI(@NonNull ThreadsDatabase threadsDatabase) {

        this.threadsDatabase = threadsDatabase;
    }


    @NonNull
    public ThreadsDatabase getThreadsDatabase() {
        return threadsDatabase;
    }


    public void setThreadsDeleting(long... idxs) {
        getThreadsDatabase().threadDao().setThreadsDeleting(idxs);
    }

    public void setThreadsUnpin(long... idxs) {
        getThreadsDatabase().threadDao().setThreadsUnpin(idxs);
    }

    public void resetThreadsPublishing(long... idxs) {
        getThreadsDatabase().threadDao().resetThreadsPublishing(idxs);
    }


    public void resetThreadsStatus(long... idxs) {
        getThreadsDatabase().threadDao().setThreadsStatus(Status.UNKNOWN, idxs);
    }

    public void setThreadLeaching(long idx) {
        getThreadsDatabase().threadDao().setLeaching(idx);
    }

    public void resetThreadLeaching(long idx) {
        getThreadsDatabase().threadDao().resetLeaching(idx);
    }

    public void setThreadStatus(long idx, Status status) {
        getThreadsDatabase().threadDao().setStatus(idx, status);
    }

    public void resetThreadStatus(long idx) {
        getThreadsDatabase().threadDao().setStatus(idx, Status.UNKNOWN);
    }

    public void setThreadSeeding(long idx) {
        getThreadsDatabase().threadDao().setSeeding(idx);
    }


    @Nullable
    public String getMimeType(@NonNull Thread thread) {

        return getThreadsDatabase().threadDao().getMimeType(thread.getIdx());
    }


    @Nullable
    public String getThreadMimeType(long idx) {
        return getThreadsDatabase().threadDao().getMimeType(idx);
    }


    public void setMimeType(@NonNull Thread thread, @NonNull String mimeType) {

        getThreadsDatabase().threadDao().setMimeType(thread.getIdx(), mimeType);
    }

    public void setThreadMimeType(long idx, @NonNull String mimeType) {

        getThreadsDatabase().threadDao().setMimeType(idx, mimeType);
    }

    @NonNull
    public Thread createThread(long parent) {

        return Thread.createThread(parent);

    }


    private boolean isReferenced(@NonNull CID cid) {

        return getThreadsDatabase().threadDao().references(cid) > 0;
    }

    private void removeThread(@NonNull IPFS ipfs, @NonNull Thread thread) {

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

        List<Thread> threads = getThreadsByIdx(idxs);
        for (Thread thread : threads) {
            removeThread(ipfs, thread);
        }
    }

    private void rm(@NonNull IPFS ipfs, @NonNull CID cid) {

        ipfs.rm(cid);
    }


    public void removeThreads(@NonNull IPFS ipfs, @NonNull List<Thread> threads) {

        getThreadsDatabase().threadDao().removeThreads(
                Iterables.toArray(threads, Thread.class));
    }


    public List<Thread> getThreads() {
        return getThreadsDatabase().threadDao().getThreads();
    }

    public long storeThread(@NonNull Thread thread) {
        return getThreadsDatabase().threadDao().insertThread(thread);
    }

    public void setThreadPin(long idx) {
        getThreadsDatabase().threadDao().setPinned(idx, true);

    }

    public void setThreadUnpin(long idx) {
        getThreadsDatabase().threadDao().setPinned(idx, false);

    }

    public void setThreadName(long idx, @NonNull String name) {

        getThreadsDatabase().threadDao().setName(idx, name);
    }

    public void setThreadContent(long idx, @NonNull CID cid) {

        Multihash.fromBase58(cid.getCid());
        getThreadsDatabase().threadDao().setContent(idx, cid);
    }

    public void setThreadThumbnail(long idx, @NonNull CID image) {

        getThreadsDatabase().threadDao().setThumbnail(idx, image);
    }

    public void setThreadPublishing(long idx) {
        getThreadsDatabase().threadDao().setThreadPublishing(idx);
    }

    public void resetThreadPublishing(long idx) {
        getThreadsDatabase().threadDao().resetThreadPublishing(idx);
    }


    @NonNull
    public List<Thread> getPinnedThreads() {
        return getThreadsDatabase().threadDao().getThreadsByPinned(true);
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

        return getThreadsDatabase().threadDao().getThreadsByContentAndParent(cid, thread);
    }

    @NonNull
    public List<Thread> getThreadsByContent(@NonNull CID cid) {

        return getThreadsDatabase().threadDao().getThreadsByContent(cid);
    }

    public List<Thread> getSeedingThreadsByQuery(String query) {

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

        getThreadsDatabase().threadDao().setProgress(idx, progress);
    }

    public void setThreadSize(long idx, long size) {
        getThreadsDatabase().threadDao().setSize(idx, size);
    }
}
