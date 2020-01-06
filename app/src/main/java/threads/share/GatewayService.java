package threads.share;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import threads.core.Singleton;
import threads.core.peers.PEERS;
import threads.core.peers.Peer;
import threads.ipfs.IPFS;
import threads.ipfs.api.PID;
import threads.ipfs.api.PeerInfo;

import static androidx.core.util.Preconditions.checkArgument;
import static androidx.core.util.Preconditions.checkNotNull;

public class GatewayService {

    public static final String TAG = GatewayService.class.getSimpleName();


    public static PeerSummary evaluateAllPeers(@NonNull Context context) {
        IPFS ipfs = Singleton.getInstance(context).getIpfs();
        PEERS threads = Singleton.getInstance(context).getPeers();

        checkNotNull(ipfs);

        // important reset all connection status
        threads.resetPeersConnected();
        List<threads.ipfs.api.Peer> peers = ipfs.swarmPeers();

        List<Long> latencies = new ArrayList<>();
        int size = peers.size();
        for (threads.ipfs.api.Peer peer : peers) {

            long lat = peer.getLatency(); // TODO remove cast
            if (lat < Long.MAX_VALUE) {
                latencies.add(lat);
            }
            // do not store circuit addresses
            if (!peer.getMultiAddress().endsWith("/p2p-circuit")) {
                storePeer(context, peer);
            }
        }

        if (Network.isConnected(context)) {
            List<Peer> stored = threads.getPeers();
            for (Peer peer : stored) {
                if (!peer.isConnected() &&
                        !peer.isRelay() &&
                        !peer.isAutonat() &&
                        !peer.isPubsub()) {
                    threads.removePeer(ipfs, peer);
                }
            }
        }

        long latency = (long)
                latencies.stream().mapToLong(val -> val).average().orElse(Long.MAX_VALUE);

        return new PeerSummary(size, latency);
    }

    public static int evaluatePeers(@NonNull Context context, boolean pubsubs) {
        IPFS ipfs = Singleton.getInstance(context).getIpfs();
        checkNotNull(ipfs);
        List<threads.ipfs.api.Peer> peers = ipfs.swarmPeers();
        int size = peers.size();
        for (threads.ipfs.api.Peer peer : peers) {
            // do not store circuit addresses
            if (!peer.getMultiAddress().endsWith("/p2p-circuit")) {
                if (pubsubs) {
                    if (peer.isAutonat() || peer.isRelay() || peer.isMeshSub() || peer.isFloodSub()) {
                        storePeer(context, peer);
                    }
                } else {
                    if (peer.isAutonat() || peer.isRelay()) {
                        storePeer(context, peer);
                    }
                }
            }
        }
        return size;
    }

    public synchronized static List<Peer> getRelayPeers(
            @NonNull Context context, @NonNull String tag, int numRelays, int timeout) {

        checkNotNull(context);
        checkArgument(numRelays >= 0);
        checkArgument(timeout > 0);

        List<Peer> result = new ArrayList<>();


        if (!Network.isConnected(context)) {
            return result;
        }

        final IPFS ipfs = Singleton.getInstance(context).getIpfs();


        if (ipfs != null) {

            List<threads.ipfs.api.Peer> peers = ipfs.swarmPeers();

            peers.sort(threads.ipfs.api.Peer::compareTo);

            for (threads.ipfs.api.Peer peer : peers) {

                if (result.size() == numRelays) {
                    break;
                }

                if (peer.isRelay()) {

                    if (ipfs.isConnected(peer.getPid())) {

                        if (!tag.isEmpty()) {
                            ipfs.protectPeer(peer.getPid(), tag);
                        }

                        result.add(storePeer(context, peer));


                    } else if (ipfs.swarmConnect(peer, timeout)) {

                        if (!tag.isEmpty()) {
                            ipfs.protectPeer(peer.getPid(), tag);
                        }

                        result.add(storePeer(context, peer));

                    }

                }
            }

        }

        return result;
    }

    public static List<Peer> getConnectedPeers(
            @NonNull Context context, @NonNull String tag, int numPeers) {

        checkNotNull(context);
        checkNotNull(tag);

        List<Peer> connected = new ArrayList<>();

        if (!Network.isConnected(context)) {
            return connected;
        }

        final IPFS ipfs = Singleton.getInstance(context).getIpfs();


        if (ipfs != null) {

            List<threads.ipfs.api.Peer> peers = ipfs.swarmPeers();

            peers.sort(threads.ipfs.api.Peer::compareTo);

            for (threads.ipfs.api.Peer peer : peers) {

                if (connected.size() == numPeers) {
                    break;
                }


                if (ipfs.isConnected(peer.getPid())) {

                    if (!tag.isEmpty()) {
                        ipfs.protectPeer(peer.getPid(), tag);
                    }
                    connected.add(storePeer(context, peer));
                }
            }
        }
        return connected;
    }

    private static Peer storePeer(@NonNull Context context,
                                  @NonNull threads.ipfs.api.Peer peer) {
        checkNotNull(context);
        checkNotNull(peer);

        // the given peer is connected (so rating will be dependent of peer
        int rating = 0;
        try {
            double latency = peer.getLatency();
            if (latency < 1000) {
                rating = (int) (1000 - latency);
            }

        } catch (Throwable e) {
            // ignore any exceptions here
        }

        // now add higher rating when peer has specific attributes
        boolean isConnected = false;
        try {
            int timeout = 5;
            IPFS ipfs = Singleton.getInstance(context).getIpfs();
            if (ipfs != null) {
                PeerInfo info = ipfs.id(peer, timeout);
                if (info != null) {

                    String protocol = info.getProtocolVersion();
                    String agent = info.getAgentVersion();

                    if (protocol != null && protocol.equals("ipfs/0.1.0")) {
                        rating = rating + 100;
                    } else {
                        rating = rating - 100;
                    }
                    if (agent != null) {
                        if (agent.startsWith("go-ipfs/0.4.2")) {
                            rating = rating + 100;
                        } else if (agent.startsWith("go-ipfs/0.5")) {
                            rating = rating + 150;
                        }
                    }
                }
                isConnected = ipfs.isConnected(peer.getPid());
            }
        } catch (Throwable e) {
            // ignore any exceptions here
        }
        if (rating < 0) {
            rating = 0;
        }
        boolean isPubsub = peer.isFloodSub() || peer.isMeshSub();

        return storePeer(context, peer.getPid(),
                peer.getMultiAddress(), peer.isRelay(), peer.isAutonat(),
                isPubsub, isConnected, rating);
    }

    @NonNull
    private static Peer storePeer(@NonNull Context context,
                                  @NonNull PID pid,
                                  @NonNull String multiAddress,
                                  boolean isRelay,
                                  boolean isAutonat,
                                  boolean isPubsub,
                                  boolean isConnected,
                                  int rating) {

        final PEERS threads = Singleton.getInstance(context).getPeers();


        Peer peer = threads.getPeerByPID(pid);
        if (peer != null) {
            peer.setMultiAddress(multiAddress);
            peer.setRating(rating);
            peer.setConnected(isConnected);
            threads.updatePeer(peer);
        } else {
            peer = threads.createPeer(pid, multiAddress);
            peer.setRelay(isRelay);
            peer.setAutonat(isAutonat);
            peer.setPubsub(isPubsub);
            peer.setRating(rating);
            peer.setConnected(isConnected);
            threads.storePeer(peer);
        }
        return peer;
    }

    public static List<Peer> connectStoredAutonat(
            @NonNull Context context, int numConnections, int timeout) {

        checkNotNull(context);
        checkArgument(numConnections >= 0);
        checkArgument(timeout > 0);
        List<Peer> connected = new ArrayList<>();
        if (!Network.isConnected(context)) {
            return connected;
        }

        final IPFS ipfs = Singleton.getInstance(context).getIpfs();
        final PEERS threads = Singleton.getInstance(context).getPeers();

        final AtomicInteger counter = new AtomicInteger(0);

        if (ipfs != null) {

            List<Peer> peers = threads.getAutonatPeers();

            peers.sort(Peer::compareTo);

            for (Peer autonat : peers) {

                if (counter.get() == numConnections) {
                    break;
                }

                if (ipfs.isConnected(autonat.getPID())) {
                    counter.incrementAndGet();
                    threads.setTimestamp(autonat, System.currentTimeMillis());
                    connected.add(autonat);
                } else {

                    String ma = autonat.getMultiAddress() + "/" +
                            IPFS.Style.p2p.name() + "/" + autonat.getPid();

                    if (ipfs.swarmConnect(ma, timeout)) {
                        counter.incrementAndGet();
                        threads.setTimestamp(autonat, System.currentTimeMillis());
                        connected.add(autonat);
                    } else {
                        if (Network.isConnected(context)) {
                            if (lifeTimeExpired(autonat)) {
                                threads.removePeer(ipfs, autonat);
                            }
                        }
                    }
                }
            }
        }
        return connected;
    }

    private static boolean lifeTimeExpired(@NonNull Peer peer) {
        return System.currentTimeMillis() >
                peer.getTimestamp() + (TimeUnit.HOURS.toMillis(24));
    }

    public static List<Peer> connectStoredPubsub(
            @NonNull Context context, int numConnections, int timeout) {

        checkNotNull(context);
        checkArgument(numConnections >= 0);
        checkArgument(timeout > 0);

        List<Peer> connected = new ArrayList<>();
        if (!Network.isConnected(context)) {
            return connected;
        }

        final IPFS ipfs = Singleton.getInstance(context).getIpfs();
        final PEERS threads = Singleton.getInstance(context).getPeers();

        final AtomicInteger counter = new AtomicInteger(0);

        if (ipfs != null) {

            List<Peer> peers = threads.getPubsubPeers();

            peers.sort(Peer::compareTo);

            for (Peer pubsub : peers) {

                if (counter.get() == numConnections) {
                    break;
                }

                if (ipfs.isConnected(pubsub.getPID())) {
                    counter.incrementAndGet();
                    threads.setTimestamp(pubsub, System.currentTimeMillis());
                    connected.add(pubsub);

                } else {

                    String ma = pubsub.getMultiAddress() + "/" +
                            IPFS.Style.p2p.name() + "/" + pubsub.getPid();

                    if (ipfs.swarmConnect(ma, timeout)) {
                        counter.incrementAndGet();
                        threads.setTimestamp(pubsub, System.currentTimeMillis());
                        connected.add(pubsub);
                    } else {
                        if (Network.isConnected(context)) {

                            if (lifeTimeExpired(pubsub)) {
                                threads.removePeer(ipfs, pubsub);
                            }
                        }
                    }
                }
            }
        }
        return connected;
    }

    public static List<Peer> connectStoredRelays(
            @NonNull Context context, @NonNull String tag, int numConnections, int timeout) {

        checkNotNull(context);
        checkNotNull(tag);
        checkArgument(numConnections >= 0);
        checkArgument(timeout > 0);

        List<Peer> connected = new ArrayList<>();
        if (!Network.isConnected(context)) {
            return connected;
        }


        final IPFS ipfs = Singleton.getInstance(context).getIpfs();
        final PEERS threads = Singleton.getInstance(context).getPeers();

        final AtomicInteger counter = new AtomicInteger(0);

        if (ipfs != null) {

            List<Peer> peers = threads.getRelayPeers();

            peers.sort(Peer::compareTo);

            for (Peer relay : peers) {

                if (counter.get() == numConnections) {
                    break;
                }

                if (ipfs.isConnected(relay.getPID())) {
                    counter.incrementAndGet();
                    threads.setTimestamp(relay, System.currentTimeMillis());
                    if (!tag.isEmpty()) {
                        ipfs.protectPeer(relay.getPID(), tag);
                    }
                    connected.add(relay);
                } else {

                    String ma = relay.getMultiAddress() + "/" +
                            IPFS.Style.p2p.name() + "/" + relay.getPid();

                    if (ipfs.swarmConnect(ma, timeout)) {
                        counter.incrementAndGet();
                        threads.setTimestamp(relay, System.currentTimeMillis());
                        if (!tag.isEmpty()) {
                            ipfs.protectPeer(relay.getPID(), tag);
                        }
                        connected.add(relay);
                    } else {

                        if (Network.isConnected(context)) {
                            if (lifeTimeExpired(relay)) {
                                threads.removePeer(ipfs, relay);
                            }
                        }
                    }
                }
            }
        }
        return connected;
    }

    public static List<Peer> connectStoredPeers(
            @NonNull Context context, int timeout) {
        checkNotNull(context);
        checkArgument(timeout > 0);

        List<Peer> connected = new ArrayList<>();

        if (!Network.isConnected(context)) {
            return connected;
        }

        final IPFS ipfs = Singleton.getInstance(context).getIpfs();
        final PEERS threads = Singleton.getInstance(context).getPeers();


        if (ipfs != null) {

            List<Peer> peers = threads.getPeers();

            peers.sort(Peer::compareTo);

            for (Peer peer : peers) {

                if (!ipfs.isConnected(peer.getPID())) {

                    String ma = peer.getMultiAddress() + "/" +
                            IPFS.Style.p2p.name() + "/" + peer.getPid();

                    if (!ipfs.swarmConnect(ma, timeout)) {
                        if (Network.isConnected(context)) {
                            threads.removePeer(ipfs, peer);
                        }
                    } else {
                        connected.add(peer);
                    }
                } else {
                    connected.add(peer);
                }
            }
        }
        return connected;
    }

    public static class PeerSummary {
        private final int numPeers;
        private final long latency;

        PeerSummary(int numPeers, long latency) {
            this.numPeers = numPeers;
            this.latency = latency;
        }

        public int getNumPeers() {
            return numPeers;
        }

        public long getLatency() {
            return latency;
        }
    }
}
