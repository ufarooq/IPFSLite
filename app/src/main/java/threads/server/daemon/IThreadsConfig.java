package threads.server.daemon;

import android.support.annotation.NonNull;

import threads.iota.server.Certificate;

public interface IThreadsConfig {

    @NonNull
    String getPort();

    Certificate getCertificate();

    String getHostname();
}
