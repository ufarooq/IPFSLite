package threads.server.core.peers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Room;

import static androidx.core.util.Preconditions.checkNotNull;

public class PEERS extends PeersAPI {

    private static PEERS INSTANCE = null;

    private PEERS(final PEERS.Builder builder) {
        super(builder.usersDatabase, builder.peersDatabase);
    }

    @NonNull
    private static PEERS createPeers(@NonNull UsersDatabase usersDatabase,
                                     @NonNull PeersDatabase peersDatabase) {
        checkNotNull(usersDatabase);
        checkNotNull(peersDatabase);

        return new Builder()
                .usersDatabase(usersDatabase)
                .peersDatabase(peersDatabase)
                .build();
    }

    public static PEERS getInstance(@NonNull Context context) {
        checkNotNull(context);

        if (INSTANCE == null) {
            synchronized (PEERS.class) {
                if (INSTANCE == null) {
                    UsersDatabase usersDatabase = Room.databaseBuilder(context, UsersDatabase.class,
                            UsersDatabase.class.getSimpleName()).fallbackToDestructiveMigration().build();

                    PeersDatabase peersDatabase =
                            Room.inMemoryDatabaseBuilder(context, PeersDatabase.class).build();

                    INSTANCE = PEERS.createPeers(usersDatabase, peersDatabase);
                }
            }
        }
        return INSTANCE;
    }


    public static class Builder {
        UsersDatabase usersDatabase = null;
        PeersDatabase peersDatabase = null;

        public PEERS build() {
            checkNotNull(usersDatabase);
            checkNotNull(peersDatabase);
            return new PEERS(this);
        }


        Builder usersDatabase(@NonNull UsersDatabase usersDatabase) {
            checkNotNull(usersDatabase);
            this.usersDatabase = usersDatabase;
            return this;
        }


        Builder peersDatabase(@NonNull PeersDatabase peersDatabase) {
            checkNotNull(peersDatabase);
            this.peersDatabase = peersDatabase;
            return this;
        }
    }
}
