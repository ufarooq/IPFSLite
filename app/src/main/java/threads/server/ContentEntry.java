package threads.server;

import androidx.annotation.NonNull;

import java.util.Objects;

import static androidx.core.util.Preconditions.checkNotNull;

public class ContentEntry {
    private final String filename;
    private final String size;
    private final String cid;

    private final String mimeType;

    public ContentEntry(@NonNull String cid,
                        @NonNull String filename,
                        @NonNull String size,
                        @NonNull String mimeType) {
        checkNotNull(cid);
        checkNotNull(filename);
        checkNotNull(size);
        this.cid = cid;
        this.filename = filename;
        this.size = size;
        this.mimeType = mimeType;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getFilename() {
        return filename;
    }

    @Override
    public String toString() {
        return "ContentEntry{" +
                "filename='" + filename + '\'' +
                ", size='" + size + '\'' +
                ", cid='" + cid + '\'' +
                ", mimeType='" + mimeType + '\'' +
                '}';
    }

    public String getSize() {
        return size;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContentEntry file = (ContentEntry) o;
        return Objects.equals(filename, file.filename) &&
                Objects.equals(size, file.size) &&
                Objects.equals(cid, file.cid) &&
                Objects.equals(mimeType, file.mimeType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filename, size, cid, mimeType);
    }


    public String getCid() {
        return cid;
    }
}
