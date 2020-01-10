package threads.ipfs.api;

import androidx.annotation.NonNull;

public interface PubsubReader {
    void receive(@NonNull PubsubInfo message);
}
