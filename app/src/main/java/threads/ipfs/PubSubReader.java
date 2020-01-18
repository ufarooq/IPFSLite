package threads.ipfs;

import androidx.annotation.NonNull;

public interface PubSubReader {
    void receive(@NonNull PubSubInfo message);
}
