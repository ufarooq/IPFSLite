package threads.core.api;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;

import java.util.HashMap;

import threads.iota.Entity;
import threads.ipfs.api.PID;

import static androidx.core.util.Preconditions.checkNotNull;

public class PeerInfoDecoder {
    private static final String TAG = PeerInfoDecoder.class.getSimpleName();

    @Nullable
    public static PeerInfo convert(@NonNull PID owner, @NonNull Entity entity) {
        checkNotNull(owner);
        checkNotNull(entity);
        Gson gson = new Gson();

        try {

            Content content = gson.fromJson(entity.getContent(), Content.class);
            PeerInfo peer = convert(owner, content);
            if (peer != null) {
                peer.setHash(entity.getHash());
            }
            return peer;

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
        return null;
    }

    @Nullable
    public static PeerInfo convert(@NonNull PID owner, @NonNull Content content) {
        checkNotNull(owner);
        checkNotNull(content);

        try {
            PeerInfo peer = PeerInfo.createPeerInfo(owner);


            String peers = content.get(Content.PEERS);
            if (peers != null) {
                Addresses addresses = Addresses.toAddresses(peers);
                checkNotNull(addresses);
                peer.setAddresses(addresses);
            }

            // NOT ENCRYPTED
            String date = content.get(Content.DATE);
            checkNotNull(date);
            long timestamp = Long.valueOf(date);
            peer.setTimestamp(timestamp);

            String additions = content.get(Content.ADDS);
            checkNotNull(additions);

            if (!additions.isEmpty()) {
                HashMap<String, String> adds = Additionals.toHashMap(additions);
                checkNotNull(adds);
                peer.setExternalAdditions(adds);
            }

            return peer;

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
        return null;
    }
}
