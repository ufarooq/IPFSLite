package threads.share;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.signature.ObjectKey;

import java.io.InputStream;


public class IPFSModelLoader implements ModelLoader<IPFSData, InputStream> {
    @Nullable
    @Override
    public LoadData<InputStream> buildLoadData(@NonNull IPFSData s,
                                               int width,
                                               int height,
                                               @NonNull Options options) {
        return new LoadData<>(new ObjectKey(s), new IPFSDataFetcher(s));
    }

    @Override
    public boolean handles(@NonNull IPFSData s) {
        return !s.getCid().getCid().isEmpty();
    }
}
