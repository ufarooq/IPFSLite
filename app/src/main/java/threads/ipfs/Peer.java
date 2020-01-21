package threads.ipfs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static androidx.core.util.Preconditions.checkNotNull;

public class Peer implements Comparable<Peer> {
    @NonNull
    private final String multiAddress;
    @NonNull
    private final PID pid;
    private final long latency;
    @Nullable
    private final String muxer;
    private final int direction;

    @NonNull
    private final List<Protocol> streams = new ArrayList<>();

    private Peer(@NonNull PID pid,
                 @NonNull String multiAddress,
                 int direction,
                 long latency,
                 @Nullable String muxer,
                 @NonNull List<Protocol> streams) {
        checkNotNull(multiAddress);
        checkNotNull(pid);
        checkNotNull(streams);
        this.multiAddress = multiAddress;
        this.pid = pid;
        this.direction = direction;
        this.latency = latency;
        this.muxer = muxer;
        this.streams.addAll(streams);
    }


    @NonNull
    public static Peer create(@NonNull Map map) {
        checkNotNull(map);
        Double latency = (Double) map.get("Latency");
        checkNotNull(latency);
        String addr = (String) map.get("Addr");
        checkNotNull(addr);
        String peerID = (String) map.get("Peer");
        checkNotNull(peerID);
        Object direction = map.get("Direction");
        int direct = 1;
        try {
            if (direction != null) {
                direct = ((Double) (direction)).intValue();
            }
        } catch (Throwable e) {
            // ignore exception
        }
        String muxer = (String) map.get("Muxer");
        Object streams = map.get("Streams");
        List<Protocol> protocols = new ArrayList<>();
        if (streams instanceof List) {
            for (Object stream : (List) streams) {
                if (stream instanceof Map) {
                    Map streamMap = (Map) stream;
                    Object streamMapProtocol = streamMap.get("Protocol");
                    if (streamMapProtocol instanceof String) {
                        String protocol = (String) streamMapProtocol;
                        if (!protocol.isEmpty()) {
                            Protocol proto = Protocol.create(protocol);
                            if (!protocols.contains(proto)) {
                                protocols.add(proto);
                            }
                        }
                    }
                }
            }
        }


        long latencyValue = Long.MAX_VALUE;
        if (latency > 0) {
            latencyValue = latency.longValue() / 1000000; // now have in ms
        }
        PID pid = PID.create(peerID);

        return new Peer(pid, addr, direct, latencyValue, muxer, protocols);
    }

    public boolean isRelay() {

        if (!this.getStreams().isEmpty()) {
            return this.hasCircuitRelayProtocol();
        }

        return false;
    }

    public boolean isFloodSub() {


        if (!this.getStreams().isEmpty()) {
            return this.hasFloodSubProtocol();
        }

        return false;
    }


    public boolean isMeshSub() {

        if (!this.getStreams().isEmpty()) {
            return this.hasMeshSubProtocol();
        }

        return false;
    }

    public boolean isAutonat() {

        if (!this.getStreams().isEmpty()) {
            return this.hasAutonatProtocol();
        }

        return false;
    }

    public boolean hasCircuitRelayProtocol() {

        for (Protocol protocol : streams) {
            if (protocol.isCircuitRelay()) {
                return true;
            }
        }

        return false;
    }

    public boolean hasFloodSubProtocol() {

        for (Protocol protocol : streams) {
            if (protocol.isFloodSub()) {
                return true;
            }
        }

        return false;
    }

    public boolean hasMeshSubProtocol() {

        for (Protocol protocol : streams) {
            if (protocol.isMeshSub()) {
                return true;
            }
        }

        return false;
    }

    public boolean hasAutonatProtocol() {

        for (Protocol protocol : streams) {
            if (protocol.isAutonat()) {
                return true;
            }
        }

        return false;
    }

    public int getDirection() {
        return direction;
    }

    @NonNull
    public String getMultiAddress() {
        return multiAddress;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Peer peer = (Peer) o;
        return Objects.equals(pid, peer.pid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pid);
    }

    @NonNull
    public PID getPid() {
        return pid;
    }

    public long getLatency() {
        return latency;
    }

    @Nullable
    public String getMuxer() {
        return muxer;
    }

    @NonNull
    public List<Protocol> getStreams() {
        return streams;
    }


    @Override
    @NonNull
    public String toString() {
        return "Peer{" +
                "multiAddress=" + multiAddress +
                ", pid=" + pid.getPid() +
                ", direction=" + direction +
                ", latency=" + latency +
                ", muxer='" + muxer + '\'' +
                ", streams=" + streams.toString() +
                '}';
    }

    @Override
    public int compareTo(@NonNull Peer peer) {
        return Double.compare(this.latency, peer.latency);
    }


}
