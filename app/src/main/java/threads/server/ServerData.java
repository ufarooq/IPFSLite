package threads.server;

import android.arch.persistence.room.TypeConverter;
import android.support.annotation.NonNull;

import com.google.gson.Gson;

import static com.google.common.base.Preconditions.checkNotNull;

public class ServerData {
    @NonNull
    private final String host;
    @NonNull
    private final String port;
    @NonNull
    private final String protocol;

    ServerData(@NonNull String protocol,
               @NonNull String host,
               @NonNull String port) {
        checkNotNull(protocol);
        checkNotNull(host);
        checkNotNull(port);

        this.protocol = protocol;
        this.port = port;
        this.host = host;
    }

    public static ServerData createServerData(@NonNull String protocol,
                                              @NonNull String host,
                                              @NonNull String port) {
        checkNotNull(protocol);
        checkNotNull(host);
        checkNotNull(port);
        return new ServerData(protocol, host, port);
    }

    @TypeConverter
    public static String toString(@NonNull ServerData server) {
        checkNotNull(server);
        Gson gson = new Gson();
        return gson.toJson(server);
    }

    @TypeConverter
    public static ServerData toServerData(@NonNull String data) {
        checkNotNull(data);
        Gson gson = new Gson();
        return gson.fromJson(data, ServerData.class);
    }

    @NonNull
    public String getHost() {
        return host;
    }

    @NonNull
    public String getPort() {
        return port;
    }

    @NonNull
    public String getProtocol() {
        return protocol;
    }

}
