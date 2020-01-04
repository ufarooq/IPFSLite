package threads.share;

import android.content.Context;
import android.media.MediaDataSource;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;

import mobile.Reader;
import threads.core.Singleton;
import threads.ipfs.IPFS;
import threads.ipfs.api.CID;

import static androidx.core.util.Preconditions.checkNotNull;

public class IPFSMediaDataSource extends MediaDataSource {
    private static final String TAG = IPFSMediaDataSource.class.getSimpleName();

    private Reader fileReader;

    public IPFSMediaDataSource(@NonNull Context context, @NonNull String cid) throws Exception {
        IPFS ipfs = Singleton.getInstance(context).getIpfs();
        checkNotNull(ipfs);
        fileReader = ipfs.getReader(CID.create(cid), true);
    }


    @Override
    public int readAt(long position, byte[] buffer, int offset, int size) throws IOException {
        try {
            fileReader.readAt(position, size);

            long read = fileReader.getRead();
            if (read > 0) {
                byte[] data = fileReader.getData();
                for (int i = 0; i < data.length; i++) {
                    buffer[offset + i] = data[i];
                }
            }
            return (int) read;
        } catch (Throwable e) {
            throw new IOException(e);
        }

    }

    @Override
    public long getSize() {
        return fileReader.getSize();
    }

    @Override
    public void close() {
        try {
            fileReader.close();
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }

    }

}
