package threads.ipfs;

import androidx.annotation.NonNull;

public class DiscoveryConfig {
    @NonNull
    private final MDNSConfig MDNS = MDNSConfig.create();

    private DiscoveryConfig() {
    }

    public static DiscoveryConfig create() {
        return new DiscoveryConfig();
    }


    @Override
    @NonNull
    public String toString() {
        return "DiscoveryConfig{" +
                "MDNS=" + MDNS +
                '}';
    }

    @NonNull
    public MDNSConfig getMdns() {
        return MDNS;
    }
}
