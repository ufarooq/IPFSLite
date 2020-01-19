package threads.server.utils;

import android.util.Log;

import androidx.annotation.NonNull;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.data.DataFetcher;

import java.io.InputStream;

import threads.ipfs.IPFS;

public class IPFSDataFetcher implements DataFetcher<InputStream> {
    private static final String TAG = IPFSDataFetcher.class.getSimpleName();
    private final IPFSData model;

    IPFSDataFetcher(@NonNull IPFSData model) {
        this.model = model;
    }

    @Override
    public void loadData(@NonNull Priority priority, @NonNull DataCallback<? super InputStream> callback) {

        try {
            IPFS ipfs = model.getIpfs();
            if (ipfs != null) {
                try (InputStream stream = ipfs.getInputStream(model.getCid())) {
                    callback.onDataReady(stream);
                }
            } else {
                callback.onDataReady(null);
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
            callback.onDataReady(null);
        }
    }

    @Override
    public void cleanup() {
    }

    @Override
    public void cancel() {
    }

    @NonNull
    @Override
    public Class<InputStream> getDataClass() {
        return InputStream.class;
    }

    @NonNull
    @Override
    public DataSource getDataSource() {
        return DataSource.LOCAL;
    }
}
