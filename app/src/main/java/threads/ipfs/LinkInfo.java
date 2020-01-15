package threads.ipfs;

import android.util.Log;

import androidx.annotation.NonNull;

import java.util.Map;

import static androidx.core.util.Preconditions.checkNotNull;

public class LinkInfo {
    private static final String TAG = LinkInfo.class.getSimpleName();
    private static final String NAME = "Name";
    private static final String TYPE = "Type";
    private static final String HASH = "Hash";
    private static final String SIZE = "Size";

    @NonNull
    private final String hash;
    @NonNull
    private final String name;
    private long size;
    private int type;


    private LinkInfo(@NonNull String hash,
                     @NonNull String name,
                     long size,
                     int type) {
        this.hash = hash;
        this.name = name;
        this.size = size;
        this.type = type;
    }

    public static LinkInfo create(@NonNull Map map) {
        checkNotNull(map);
        String name = (String) map.get(NAME);
        checkNotNull(name);
        String hash = (String) map.get(HASH);
        checkNotNull(hash);
        long size = -1;
        int type = -1;
        try {
            Object object = map.get(SIZE);
            if (object instanceof Long) {
                size = (Long) object;
            } else if (object instanceof Integer) {
                size = (Integer) object;
            } else if (object instanceof Double) {
                size = ((Double) object).longValue();
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
            // ignore exception (can be not defined or "-")
        }
        try {
            Object object = map.get(TYPE);
            if (object instanceof Long) {
                type = ((Long) object).intValue();
            } else if (object instanceof Integer) {
                type = (Integer) object;
            } else if (object instanceof Double) {
                type = ((Double) object).intValue();
            }
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
            // ignore exception (can be not defined or "-")
        }

        return new LinkInfo(hash, name, size, type);
    }

    @NonNull
    public CID getCid() {
        return CID.create(hash);
    }


    public long getSize() {
        return size;
    }

    @NonNull
    public String getName() {
        return name;
    }


    public int getType() {
        return type;
    }


    public boolean isDirectory() {
        return type == 1;

    }


    @Override
    public String toString() {
        return "LinkInfo{" +
                "hash='" + hash + '\'' +
                ", name='" + name + '\'' +
                ", size=" + size +
                ", type=" + type +
                '}';
    }

    public boolean isFile() {
        return type == 2;

    }


    public boolean isRaw() {
        return type == 0;
    }
}
