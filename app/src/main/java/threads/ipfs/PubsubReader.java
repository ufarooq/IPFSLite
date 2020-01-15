package threads.ipfs;

import androidx.annotation.NonNull;

public interface PubsubReader {
    void receive(@NonNull PubsubInfo message);
}
