package threads.server.core.contents;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Room;

import threads.ipfs.CID;

public class CDS {
    private static CDS INSTANCE = null;
    @NonNull
    private final ContentDatabase contentDatabase;

    private CDS(@NonNull Context context) {

        contentDatabase = Room.databaseBuilder(context,
                ContentDatabase.class,
                ContentDatabase.class.getSimpleName()).fallbackToDestructiveMigration().build();
    }

    @NonNull
    public static CDS getInstance(@NonNull Context context) {

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
    public Content getContent(@NonNull String cid) {

        return getContentDatabase().contentDao().getContent(cid);
    }

    public void updateTimestamp(@NonNull String cid) {

        getContentDatabase().contentDao().setTimestamp(cid, System.currentTimeMillis());
    }

    public void insertContent(@NonNull String pid, @NonNull String cid, boolean finished) {

        Content content = Content.create(pid, cid, finished);
        getContentDatabase().contentDao().insertContent(content);
    }

    public void finishContent(@NonNull CID cid) {

        getContentDatabase().contentDao().setFinished(cid.getCid(), true);
    }
}
