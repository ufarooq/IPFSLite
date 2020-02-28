package threads.server.core.threads;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Room;

public class THREADS extends ThreadsAPI {

    private static THREADS INSTANCE = null;

    private THREADS(final THREADS.Builder builder) {
        super(builder.threadsDatabase);
    }

    @NonNull
    private static THREADS createThreads(@NonNull ThreadsDatabase threadsDatabase) {


        return new THREADS.Builder()
                .threadsDatabase(threadsDatabase)
                .build();
    }

    public static THREADS getInstance(@NonNull Context context) {


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


    static class Builder {

        ThreadsDatabase threadsDatabase = null;

        THREADS build() {

            return new THREADS(this);
        }

        Builder threadsDatabase(@NonNull ThreadsDatabase threadsDatabase) {

            this.threadsDatabase = threadsDatabase;
            return this;
        }


    }
}
