package threads.server;

import androidx.annotation.NonNull;

import java.util.Objects;

import static androidx.core.util.Preconditions.checkNotNull;

public class ContentFile {
    private final String filename;
    private final String size;
    private final String cid;

    public ContentFile(@NonNull String cid,
                       @NonNull String filename,
                       @NonNull String size) {
        checkNotNull(cid);
        checkNotNull(filename);
        checkNotNull(size);
        this.cid = cid;
        this.filename = filename;
        this.size = size;
    }

    public String getFilename() {
        return filename;
    }

    public String getSize() {
        return size;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContentFile file = (ContentFile) o;
        return Objects.equals(filename, file.filename) &&
                Objects.equals(size, file.size) &&
                Objects.equals(cid, file.cid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filename, size, cid);
    }

    @Override
    public String toString() {
        return "ContentFile{" +
                "filename='" + filename + '\'' +
                ", size='" + size + '\'' +
                ", cid='" + cid + '\'' +
                '}';
    }

    public String getCid() {
        return cid;
    }
}
