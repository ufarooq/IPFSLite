package threads.server.core.peers;

import androidx.annotation.NonNull;

import threads.ipfs.PID;

public interface IPeer {

    @NonNull
    PID getPID();

    boolean isBlocked();

    @NonNull
    String getAlias();

    boolean isConnected();

    boolean isDialing();
}
