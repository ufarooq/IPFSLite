package threads.server;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

import static androidx.core.util.Preconditions.checkNotNull;

class ContentEntry {
    private final String filename;
    private final String size;
    private final String cid;
    private final String image;
    private final String mimeType;

    ContentEntry(@NonNull String cid,
                 @NonNull String filename,
                 @NonNull String size,
                 @NonNull String mimeType,
                 @Nullable String image) {
        checkNotNull(cid);
        checkNotNull(filename);
        checkNotNull(size);
        this.cid = cid;
        this.filename = filename;
        this.size = size;
        this.mimeType = mimeType;
        this.image = image;
    }

    String getImage() {
        return image;
    }

    String getMimeType() {
        return mimeType;
    }

    String getFilename() {
        return filename;
    }

    @Override
    @NonNull
    public String toString() {
        return "ContentEntry{" +
                "filename='" + filename + '\'' +
                ", size='" + size + '\'' +
                ", cid='" + cid + '\'' +
                ", mimeType='" + mimeType + '\'' +
                ", image='" + image + '\'' +
                '}';
    }

    String getSize() {
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
