package threads.core.threads;

import androidx.annotation.NonNull;

import threads.ipfs.IPFS;
import threads.ipfs.api.CID;

import static androidx.core.util.Preconditions.checkNotNull;

public class THREADS extends ThreadsAPI {
    public static final String TAG = THREADS.class.getSimpleName();


    private THREADS(final THREADS.Builder builder) {
        super(builder.threadsDatabase);
    }

    @NonNull
    public static THREADS createThreads(@NonNull ThreadsDatabase threadsDatabase) {
        checkNotNull(threadsDatabase);

        return new THREADS.Builder()
                .threadsDatabase(threadsDatabase)
                .build();
    }


    public void setImage(@NonNull IPFS ipfs,
                         @NonNull Thread thread,
                         @NonNull byte[] data) throws Exception {
        checkNotNull(ipfs);
        checkNotNull(thread);
        checkNotNull(data);
        CID image = ipfs.storeData(data);
        if (image != null) {
            setImage(thread, image);
        }
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
