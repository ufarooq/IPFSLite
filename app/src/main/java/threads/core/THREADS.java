package threads.core;

import androidx.annotation.NonNull;

import threads.core.api.EventsDatabase;
import threads.core.api.PeersDatabase;
import threads.core.api.PeersInfoDatabase;
import threads.core.api.Thread;
import threads.core.api.ThreadsAPI;
import threads.core.api.ThreadsDatabase;
import threads.iota.EntityService;
import threads.ipfs.IPFS;
import threads.ipfs.api.CID;

import static androidx.core.util.Preconditions.checkNotNull;

public class THREADS extends ThreadsAPI {
    public static final String TAG = THREADS.class.getSimpleName();


    private THREADS(final THREADS.Builder builder) {
        super(builder.threadsDatabase, builder.eventsDatabase, builder.peersInfoDatabase,
                builder.peersDatabase, builder.entityService);
    }

    @NonNull
    public static THREADS createThreads(@NonNull ThreadsDatabase threadsDatabase,
                                        @NonNull EventsDatabase eventsDatabase,
                                        @NonNull PeersInfoDatabase peersInfoDatabase,
                                        @NonNull PeersDatabase peersDatabase,
                                        @NonNull EntityService entityService) {
        checkNotNull(threadsDatabase);
        checkNotNull(eventsDatabase);
        checkNotNull(peersInfoDatabase);
        checkNotNull(peersDatabase);
        checkNotNull(entityService);
        return new THREADS.Builder()
                .threadsDatabase(threadsDatabase)
                .peersInfoDatabase(peersInfoDatabase)
                .peersDatabase(peersDatabase)
                .eventsDatabase(eventsDatabase)
                .entityService(entityService)
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
        EventsDatabase eventsDatabase = null;
        ThreadsDatabase threadsDatabase = null;
        PeersInfoDatabase peersInfoDatabase = null;
        EntityService entityService = null;
        PeersDatabase peersDatabase = null;

        public THREADS build() {
            checkNotNull(threadsDatabase);
            checkNotNull(eventsDatabase);
            checkNotNull(peersInfoDatabase);
            checkNotNull(peersDatabase);
            checkNotNull(entityService);
            return new THREADS(this);
        }

        public Builder threadsDatabase(@NonNull ThreadsDatabase threadsDatabase) {
            checkNotNull(threadsDatabase);
            this.threadsDatabase = threadsDatabase;
            return this;
        }

        public Builder eventsDatabase(@NonNull EventsDatabase eventsDatabase) {
            checkNotNull(eventsDatabase);
            this.eventsDatabase = eventsDatabase;
            return this;
        }

        public Builder peersInfoDatabase(@NonNull PeersInfoDatabase peersDatabase) {
            checkNotNull(peersDatabase);
            this.peersInfoDatabase = peersDatabase;
            return this;
        }

        public Builder entityService(@NonNull EntityService entityService) {
            checkNotNull(entityService);
            this.entityService = entityService;
            return this;
        }

        public Builder peersDatabase(@NonNull PeersDatabase peersDatabase) {
            checkNotNull(peersDatabase);
            this.peersDatabase = peersDatabase;
            return this;
        }
    }
}
