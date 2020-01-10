package threads.ipfs.api;

import androidx.annotation.NonNull;

import static androidx.core.util.Preconditions.checkNotNull;

public class RoutingConfig {

    @NonNull
    private TypeEnum Type = TypeEnum.dht;

    private RoutingConfig() {
    }

    public static RoutingConfig create() {
        return new RoutingConfig();
    }

    @Override
    @NonNull
    public String toString() {
        return "RoutingConfig{" +
                "TypeEnum='" + Type.name() + '\'' +
                '}';
    }

    @NonNull
    public TypeEnum getType() {
        return Type;
    }

    public void setType(@NonNull TypeEnum type) {
        checkNotNull(type);
        this.Type = type;
    }

    public enum TypeEnum {
        dht, dhtclient, none
    }
}
