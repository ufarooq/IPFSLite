package threads.server.services;

import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

public class RegistrationService implements NsdManager.RegistrationListener {
    private static final String TAG = RegistrationService.class.getSimpleName();
    private static RegistrationService INSTANCE = null;

    public static RegistrationService getInstance() {
        if (INSTANCE == null) {

            synchronized (RegistrationService.class) {

                if (INSTANCE == null) {
                    INSTANCE = new RegistrationService();
                }
            }

        }
        return INSTANCE;
    }

    @Override
    public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
        Log.e(TAG, "RegistrationFailed : " + errorCode);
    }

    @Override
    public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
        Log.e(TAG, "Un-RegistrationFailed : " + errorCode);
    }

    @Override
    public void onServiceRegistered(NsdServiceInfo serviceInfo) {
        Log.e(TAG, "ServiceRegistered : " + serviceInfo.getServiceName());
    }

    @Override
    public void onServiceUnregistered(NsdServiceInfo serviceInfo) {
        Log.e(TAG, "Un-ServiceRegistered : " + serviceInfo.getServiceName());
    }
}
