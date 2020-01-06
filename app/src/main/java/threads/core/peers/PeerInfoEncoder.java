package threads.core.peers;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

import java.util.HashMap;

import static androidx.core.util.Preconditions.checkNotNull;

public class PeerInfoEncoder {
    @NonNull
    public static String convert(@NonNull PeerInfo peer) {
        checkNotNull(peer);

        Content content = new Content();
        Gson gson = new Gson();
        try {

            Addresses addresses = peer.getAddresses();
            checkNotNull(addresses);
            if (!addresses.isEmpty()) {
                String peers = Addresses.toString(addresses);
                checkNotNull(peers);
                content.put(Content.PEERS, peers);
            }

            HashMap<String, String> additions = peer.getExternalAdditions();
            checkNotNull(additions);
            content.put(Content.ADDS, Additionals.toString(additions));

            // NOT ENCRYPTED
            content.put(Content.DATE, String.valueOf(peer.getTimestamp()));

            return gson.toJson(content);


        } catch (Throwable e) {
            throw new RuntimeException(e);
        }


    }
}
