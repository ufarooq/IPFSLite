package threads.ipfs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static androidx.core.util.Preconditions.checkArgument;
import static androidx.core.util.Preconditions.checkNotNull;

public class AddressesConfig {
    @NonNull
    private final List<String> Swarm = new ArrayList<>(Arrays.asList("/ip4/0.0.0.0/tcp/4001", "/ip6/::/tcp/4001"));
    @NonNull
    private final List<String> NoAnnounce = new ArrayList<>();
    @NonNull
    private final List<String> Announce = new ArrayList<>();
    @NonNull
    private String API = "";
    @NonNull
    private String Gateway = "";

    private AddressesConfig() {
    }

    @NonNull
    public static AddressesConfig create() {
        return new AddressesConfig();
    }


    public static AddressesConfig create(@NonNull Integer swarmPort,
                                         @Nullable Integer quicPort) {
        checkNotNull(swarmPort);
        checkArgument(swarmPort >= 0);

        AddressesConfig addresses = new AddressesConfig();
        addresses.clearSwarm();

        addresses.addSwarm("/ip4/0.0.0.0/tcp/" + swarmPort);
        addresses.addSwarm("/ip6/::/tcp/" + swarmPort);


        if (quicPort != null) {
            addresses.addSwarm("/ip4/0.0.0.0/udp/" + quicPort + "/quic");
            addresses.addSwarm("/ip6/::/udp/" + quicPort + "/quic");
        }


        return addresses;
    }


    @NonNull
    public List<String> getNoAnnounce() {
        return new ArrayList<>(NoAnnounce);
    }

    @Override
    @NonNull
    public String toString() {
        return "AddressesConfig{" +
                "SwarmConfig=" + Swarm +
                ", NoAnnounce=" + NoAnnounce +
                ", Announce=" + Announce +
                ", API='" + API + '\'' +
                ", Gateway='" + Gateway + '\'' +
                '}';
    }

    @NonNull
    public List<String> getAnnounce() {
        return new ArrayList<>(Announce);
    }

    @NonNull
    public List<String> getSwarm() {
        return new ArrayList<>(Swarm);
    }

    @NonNull
    public String getAPI() {
        return API;
    }


    @NonNull
    public String getGateway() {
        return Gateway;
    }


    public void addSwarm(@NonNull String... entries) {
        checkNotNull(entries);
        for (String entry : entries) {
            if (!Swarm.contains(entry)) {
                Swarm.add(entry);
            }
        }
    }

    public void addAnnounce(@NonNull String... entries) {
        checkNotNull(entries);
        for (String entry : entries) {
            if (!Announce.contains(entry)) {
                Announce.add(entry);
            }
        }
    }

    public void addNoAnnounce(@NonNull String... entries) {
        checkNotNull(entries);
        for (String entry : entries) {
            if (!NoAnnounce.contains(entry)) {
                NoAnnounce.add(entry);
            }
        }

    }

    public void addSwarmEntries(List<String> entries) {
        if (entries == null) {
            clearSwarm();
        } else {
            addSwarm(entries.toArray(new String[0]));
        }
    }

    public void addAnnounceEntries(List<String> entries) {
        if (entries == null) {
            clearAnnounce();
        } else {
            addAnnounce(entries.toArray(new String[0]));
        }
    }

    public void addNoAnnounceEntries(List<String> entries) {
        if (entries == null) {
            clearNoAnnounce();
        } else {
            addNoAnnounce(entries.toArray(new String[0]));
        }

    }

    public void clearSwarm() {
        Swarm.clear();
    }

    public void clearNoAnnounce() {
        NoAnnounce.clear();
    }

    public void clearAnnounce() {
        Announce.clear();
    }

    public void removeSwarm(@NonNull String entry) {
        checkNotNull(entry);
        Swarm.remove(entry);
    }


}
