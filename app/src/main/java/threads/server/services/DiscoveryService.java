package threads.server.services;

import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import androidx.annotation.NonNull;

import static androidx.core.util.Preconditions.checkNotNull;

public class DiscoveryService implements NsdManager.DiscoveryListener {
    private static final String TAG = DiscoveryService.class.getSimpleName();
    private static DiscoveryService INSTANCE = null;
    private OnServiceFoundListener mListener = null;

    public static DiscoveryService getInstance() {
        if (INSTANCE == null) {

            synchronized (DiscoveryService.class) {

                if (INSTANCE == null) {
                    INSTANCE = new DiscoveryService();
                }
            }

        }
        return INSTANCE;
    }

    public void setOnServiceFoundListener(@NonNull OnServiceFoundListener listener) {
        checkNotNull(listener);
        mListener = listener;
    }

    @Override
    public void onStartDiscoveryFailed(String serviceType, int errorCode) {
        Log.e(TAG, "onStartDiscoveryFailed");
    }

    @Override
    public void onStopDiscoveryFailed(String serviceType, int errorCode) {
        Log.e(TAG, "onStopDiscoveryFailed");
    }

    @Override
    public void onDiscoveryStarted(String serviceType) {
        Log.e(TAG, "onDiscoveryStarted");
    }

    @Override
    public void onDiscoveryStopped(String serviceType) {
        Log.e(TAG, "onDiscoveryStopped");
    }

    @Override
    public void onServiceFound(NsdServiceInfo serviceInfo) {
        Log.e(TAG, "onServiceFound");
        if (mListener != null) {
            mListener.resolveService(serviceInfo);
        }
    }

    @Override
    public void onServiceLost(NsdServiceInfo serviceInfo) {
        Log.e(TAG, "onServiceLost");
    }

    public interface OnServiceFoundListener {

        void resolveService(@NonNull NsdServiceInfo serviceInfo);
    }
}
