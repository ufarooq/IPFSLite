package threads.server;

import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.annotation.UiThread;

public interface ScannerListener {
    @UiThread
    void handleScannerCode(@NonNull String scannerCode);

    @UiThread
    void onCameraError(@StringRes int errorTextRes);
}
