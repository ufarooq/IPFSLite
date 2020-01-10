package threads.ipfs.api;

import androidx.annotation.NonNull;

import java.util.Objects;

import static androidx.core.util.Preconditions.checkNotNull;

public class Protocol {
    @NonNull
    private final String name;

    private Protocol(@NonNull String name) {
        this.name = name;
    }

    @NonNull
    public static Protocol create(@NonNull String name) {
        checkNotNull(name);
        return new Protocol(name);
    }

    @NonNull
    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Protocol protocol = (Protocol) o;
        return Objects.equals(name, protocol.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    public boolean isCircuitRelay() {
        return name.startsWith("/libp2p/circuit/relay/");
    }

    public boolean isAutonat() {
        return name.startsWith("/libp2p/autonat/");
    }

    public boolean isFloodSub() {
        return name.startsWith("/floodsub/");
    }

    public boolean isMeshSub() {
        return name.startsWith("/meshsub/");
    }

    @Override
    @NonNull
    public String toString() {
        return "Protocol{" +
                "name='" + name + '\'' +
                '}';
    }
}
