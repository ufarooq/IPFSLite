package threads.core.contents;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Room;

import threads.ipfs.api.CID;
import threads.ipfs.api.PID;

import static androidx.core.util.Preconditions.checkNotNull;

public class CDS {
    private static CDS INSTANCE = null;
    @NonNull
    private final ContentDatabase contentDatabase;

    private CDS(@NonNull Context context) {
        checkNotNull(context);

        contentDatabase = Room.databaseBuilder(context,
                ContentDatabase.class,
                ContentDatabase.class.getSimpleName()).fallbackToDestructiveMigration().build();
    }

    @NonNull
    public static CDS getInstance(@NonNull Context context) {
        checkNotNull(context);
        if (INSTANCE == null) {
            synchronized (CDS.class) {
                if (INSTANCE == null) {
                    INSTANCE = new CDS(context);
                }
            }
        }
        return INSTANCE;
    }

    @NonNull
    public ContentDatabase getContentDatabase() {
        return contentDatabase;
    }


    @Nullable
    public Content getContent(@NonNull CID cid) {
        checkNotNull(cid);
        return getContentDatabase().contentDao().getContent(cid.getCid());
    }


    public void insertContent(@NonNull PID pid, @NonNull CID cid, boolean finished) {
        checkNotNull(pid);
        checkNotNull(cid);
        Content content = Content.create(pid, cid, finished);
        getContentDatabase().contentDao().insertContent(content);
    }

    public void finishContent(CID cid) {
        checkNotNull(cid);
        getContentDatabase().contentDao().setFinished(cid.getCid(), true);
    }
}
