package threads.server;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Room;

import threads.ipfs.api.CID;
import threads.ipfs.api.PID;

import static androidx.core.util.Preconditions.checkNotNull;

public class ContentService {
    private static ContentService SINGLETON = null;
    @NonNull
    private final ContentDatabase contentDatabase;

    private ContentService(@NonNull Context context) {
        checkNotNull(context);

        contentDatabase = Room.databaseBuilder(context,
                ContentDatabase.class,
                ContentDatabase.class.getSimpleName()).fallbackToDestructiveMigration().build();
    }

    @NonNull
    public static ContentService getInstance(@NonNull Context context) {
        checkNotNull(context);
        if (SINGLETON == null) {
            SINGLETON = new ContentService(context);
        }
        return SINGLETON;
    }

    @NonNull
    public ContentDatabase getContentDatabase() {
        return contentDatabase;
    }


    @Nullable
    Content getContent(@NonNull CID cid) {
        checkNotNull(cid);
        return getContentDatabase().contentDao().getContent(cid.getCid());
    }


    void insertContent(@NonNull PID pid, @NonNull CID cid, boolean finished) {
        checkNotNull(pid);
        checkNotNull(cid);
        Content content = Content.create(pid, cid, finished);
        getContentDatabase().contentDao().insertContent(content);
    }

    void finishContent(CID cid) {
        checkNotNull(cid);
        getContentDatabase().contentDao().setFinished(cid.getCid(), true);
    }
}
