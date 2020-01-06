package threads.core.peers;

import androidx.annotation.NonNull;

import static androidx.core.util.Preconditions.checkNotNull;

public class PEERS extends PeersAPI {
    public static final String TAG = PEERS.class.getSimpleName();


    private PEERS(final PEERS.Builder builder) {
        super(builder.peersInfoDatabase, builder.peersDatabase);
    }

    @NonNull
    public static PEERS createPeers(@NonNull PeersInfoDatabase peersInfoDatabase,
                                    @NonNull PeersDatabase peersDatabase) {
        checkNotNull(peersInfoDatabase);
        checkNotNull(peersDatabase);

        return new PEERS.Builder()
                .peersInfoDatabase(peersInfoDatabase)
                .peersDatabase(peersDatabase)
                .build();
    }


    public static class Builder {
        PeersInfoDatabase peersInfoDatabase = null;
        PeersDatabase peersDatabase = null;

        public PEERS build() {
            checkNotNull(peersInfoDatabase);
            checkNotNull(peersDatabase);
            return new PEERS(this);
        }


        public Builder peersInfoDatabase(@NonNull PeersInfoDatabase peersDatabase) {
            checkNotNull(peersDatabase);
            this.peersInfoDatabase = peersDatabase;
            return this;
        }


        public Builder peersDatabase(@NonNull PeersDatabase peersDatabase) {
            checkNotNull(peersDatabase);
            this.peersDatabase = peersDatabase;
            return this;
        }
    }
}
