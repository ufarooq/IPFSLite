package threads.server.daemon;

import android.support.annotation.NonNull;


public interface IThreadsConfig {

    @NonNull
    String getPort();

    String getHostname();
}
