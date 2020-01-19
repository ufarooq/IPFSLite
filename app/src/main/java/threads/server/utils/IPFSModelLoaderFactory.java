package threads.server.utils;

import androidx.annotation.NonNull;

import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;

import java.io.InputStream;

public class IPFSModelLoaderFactory implements ModelLoaderFactory<IPFSData, InputStream> {
    @NonNull
    @Override
    public ModelLoader<IPFSData, InputStream> build(@NonNull MultiModelLoaderFactory multiFactory) {
        return new IPFSModelLoader();
    }

    @Override
    public void teardown() {

    }
}
