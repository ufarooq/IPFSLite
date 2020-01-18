package threads.ipfs;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static androidx.core.util.Preconditions.checkNotNull;

public class Config {

    @NonNull
    private final List<String> Bootstrap = new ArrayList<>(Arrays.asList("/ip4/104.236.76.40/tcp/4001/p2p/QmSoLV4Bbm51jM9C4gDYZQ9Cy3U6aXMJDAbzgu2fzaDs64",
            "/ip6/2604:a880:800:10::4a:5001/tcp/4001/p2p/QmSoLV4Bbm51jM9C4gDYZQ9Cy3U6aXMJDAbzgu2fzaDs64",
            "/dnsaddr/bootstrap.libp2p.io/p2p/QmbLHAnMoJPWSCR5Zhtx6BHJX9KiKNN6tpvbUcqanj75Nb",
            "/dnsaddr/bootstrap.libp2p.io/p2p/QmcZf59bWwK5XFi76CZX8cbJ4BhTzzA3gU1ZjYZcYW3dwt",
            "/ip4/104.131.131.82/tcp/4001/p2p/QmaCpDMGvV2BGHeYERUEnRQAwe3N8SzbUtfsmvsqQLuvuJ",
            "/ip4/104.236.179.241/tcp/4001/p2p/QmSoLPppuBtQSGwKDZT2M73ULpjvfd3aZ6ha4oFGL1KrGM",
            "/ip6/2604:a880:1:20::203:d001/tcp/4001/p2p/QmSoLPppuBtQSGwKDZT2M73ULpjvfd3aZ6ha4oFGL1KrGM",
            "/ip4/128.199.219.111/tcp/4001/p2p/QmSoLSafTMBsPKadTEgaXctDQVcqN88CNLHXMkTNwMKPnu",
            "/ip6/2400:6180:0:d0::151:6001/tcp/4001/p2p/QmSoLSafTMBsPKadTEgaXctDQVcqN88CNLHXMkTNwMKPnu",
            "/ip4/178.62.158.247/tcp/4001/p2p/QmSoLer265NRgSp2LA3dPaeykiS1J6DifTC88f5uVQKNAd",
            "/ip6/2a03:b0c0:0:1010::23:1001/tcp/4001/p2p/QmSoLer265NRgSp2LA3dPaeykiS1J6DifTC88f5uVQKNAd",
            "/dnsaddr/bootstrap.libp2p.io/p2p/QmNnooDu7bfjPFoTZYxMNLWUQJyrVwtbZg5gBMjTezGAJN",
            "/dnsaddr/bootstrap.libp2p.io/p2p/QmQCU2EcMqAqQPR2i9bChDtGNJchTbq5TbXJJ16u19uLTa"));
    @NonNull
    private IdentityConfig Identity = IdentityConfig.create();
    @NonNull
    private RoutingConfig Routing = RoutingConfig.create();
    @NonNull
    private DiscoveryConfig Discovery = DiscoveryConfig.create();
    @NonNull
    private ExperimentalConfig Experimental = ExperimentalConfig.create();
    @NonNull
    private PubSubConfig Pubsub = PubSubConfig.create();
    @NonNull
    private SwarmConfig Swarm = SwarmConfig.create();
    @NonNull
    private AddressesConfig Addresses = AddressesConfig.create();

    public static Config create() {
        return new Config();
    }

    @Override
    @NonNull
    public String toString() {
        return "Config{" +
                "Bootstrap=" + Bootstrap +
                ", Identity=" + Identity +
                ", Routing=" + Routing +
                ", Discovery=" + Discovery +
                ", Experimental=" + Experimental +
                ", Pubsub=" + Pubsub +
                ", Swarm=" + Swarm +
                ", Addresses=" + Addresses +
                '}';
    }


    @NonNull
    public List<String> getBootstrap() {
        return new ArrayList<>(Bootstrap);
    }

    public void addBootstrap(@NonNull String... entries) {
        checkNotNull(entries);
        for (String entry : entries) {
            if (!Bootstrap.contains(entry)) {
                Bootstrap.add(entry);
            }
        }
    }

    public void addBootstrapEntries(List<String> entries) {
        if (entries == null) {
            clearBootstrap();
        } else {
            addBootstrap(entries.toArray(new String[0]));
        }
    }

    public void clearBootstrap() {
        Bootstrap.clear();
    }

    public void removeBootstrap(@NonNull String entry) {
        checkNotNull(entry);
        Bootstrap.remove(entry);
    }

    @NonNull
    public IdentityConfig getIdentity() {
        return Identity;
    }

    public void setIdentity(@NonNull IdentityConfig identity) {
        Identity = identity;
    }

    @NonNull
    public RoutingConfig getRouting() {
        return Routing;
    }

    public void setRouting(@NonNull RoutingConfig routing) {
        checkNotNull(routing);
        this.Routing = routing;
    }

    @NonNull
    public DiscoveryConfig getDiscovery() {
        return Discovery;
    }

    public void setDiscovery(@NonNull DiscoveryConfig discovery) {
        checkNotNull(discovery);
        this.Discovery = discovery;
    }

    @NonNull
    public ExperimentalConfig getExperimental() {
        return Experimental;
    }

    public void setExperimental(@NonNull ExperimentalConfig experimental) {
        checkNotNull(experimental);
        this.Experimental = experimental;
    }

    @NonNull
    public PubSubConfig getPubsub() {
        return Pubsub;
    }

    public void setPubsub(@NonNull PubSubConfig pubsub) {
        checkNotNull(pubsub);
        this.Pubsub = pubsub;
    }

    @NonNull
    public SwarmConfig getSwarm() {
        return Swarm;
    }

    public void setSwarm(@NonNull SwarmConfig swarm) {
        checkNotNull(swarm);
        this.Swarm = swarm;
    }

    @NonNull
    public AddressesConfig getAddresses() {
        return Addresses;
    }

    public void setAddresses(@NonNull AddressesConfig addresses) {
        checkNotNull(addresses);
        this.Addresses = addresses;
    }

}
