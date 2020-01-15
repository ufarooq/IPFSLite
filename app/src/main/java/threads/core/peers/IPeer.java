package threads.core.peers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import threads.ipfs.CID;
import threads.ipfs.PID;

public interface IPeer {

    @NonNull
    PID getPID();

    boolean areItemsTheSame(@NonNull IPeer peer);

    boolean sameContent(@NonNull IPeer peer);

    boolean isBlocked();

    @NonNull
    String getAlias();

    @Nullable
    CID getImage();

    boolean isConnected();

    boolean isDialing();
}
