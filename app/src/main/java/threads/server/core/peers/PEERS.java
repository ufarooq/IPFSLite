package threads.server.core.peers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Room;

import static androidx.core.util.Preconditions.checkNotNull;

public class PEERS extends PeersAPI {

    private static PEERS INSTANCE = null;

    private PEERS(final PEERS.Builder builder) {
        super(builder.peersInfoDatabase, builder.peersDatabase);
    }

    @NonNull
    private static PEERS createPeers(@NonNull PeersInfoDatabase peersInfoDatabase,
                                     @NonNull PeersDatabase peersDatabase) {
        checkNotNull(peersInfoDatabase);
        checkNotNull(peersDatabase);

        return new PEERS.Builder()
                .peersInfoDatabase(peersInfoDatabase)
                .peersDatabase(peersDatabase)
                .build();
    }

    public static PEERS getInstance(@NonNull Context context) {
        checkNotNull(context);

        if (INSTANCE == null) {
            synchronized (PEERS.class) {
                if (INSTANCE == null) {
                    PeersInfoDatabase peersInfoDatabase =
                            Room.inMemoryDatabaseBuilder(context, PeersInfoDatabase.class).build();
                    PeersDatabase peersDatabase = Room.databaseBuilder(context, PeersDatabase.class,
                            PeersDatabase.class.getSimpleName()).fallbackToDestructiveMigration().build();

                    INSTANCE = PEERS.createPeers(peersInfoDatabase, peersDatabase);
                }
            }
        }
        return INSTANCE;
    }


    public static class Builder {
        PeersInfoDatabase peersInfoDatabase = null;
        PeersDatabase peersDatabase = null;

        public PEERS build() {
            checkNotNull(peersInfoDatabase);
            checkNotNull(peersDatabase);
            return new PEERS(this);
        }


        Builder peersInfoDatabase(@NonNull PeersInfoDatabase peersDatabase) {
            checkNotNull(peersDatabase);
            this.peersInfoDatabase = peersDatabase;
            return this;
        }


        Builder peersDatabase(@NonNull PeersDatabase peersDatabase) {
            checkNotNull(peersDatabase);
            this.peersDatabase = peersDatabase;
            return this;
        }
    }
}
