package threads.server.core.threads;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Room;

import static androidx.core.util.Preconditions.checkNotNull;

public class THREADS extends ThreadsAPI {
    public static final String TAG = THREADS.class.getSimpleName();
    private static THREADS INSTANCE = null;

    private THREADS(final THREADS.Builder builder) {
        super(builder.threadsDatabase);
    }

    @NonNull
    private static THREADS createThreads(@NonNull ThreadsDatabase threadsDatabase) {
        checkNotNull(threadsDatabase);

        return new THREADS.Builder()
                .threadsDatabase(threadsDatabase)
                .build();
    }

    public static THREADS getInstance(@NonNull Context context) {
        checkNotNull(context);

        if (INSTANCE == null) {
            synchronized (THREADS.class) {
                if (INSTANCE == null) {
                    ThreadsDatabase threadsDatabase = Room.databaseBuilder(context,
                            ThreadsDatabase.class,
                            ThreadsDatabase.class.getSimpleName()).allowMainThreadQueries().fallbackToDestructiveMigration().build();
                    INSTANCE = THREADS.createThreads(threadsDatabase);
                }
            }
        }
        return INSTANCE;
    }


    public static class Builder {

        ThreadsDatabase threadsDatabase = null;

        public THREADS build() {
            checkNotNull(threadsDatabase);


            return new THREADS(this);
        }

        Builder threadsDatabase(@NonNull ThreadsDatabase threadsDatabase) {
            checkNotNull(threadsDatabase);
            this.threadsDatabase = threadsDatabase;
            return this;
        }


    }
}
