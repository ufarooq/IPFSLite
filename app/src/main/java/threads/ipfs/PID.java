package threads.ipfs;

import androidx.annotation.NonNull;

import java.util.Objects;

import static androidx.core.util.Preconditions.checkNotNull;

public class PID {
    @NonNull
    private String pid;

    private PID(@NonNull String pid) {
        this.pid = pid;
    }

    public static PID create(@NonNull String pid) {
        checkNotNull(pid);
        return new PID(pid);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PID pid1 = (PID) o;
        return Objects.equals(pid, pid1.pid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pid);
    }

    @Override
    @NonNull
    public String toString() {
        return pid;
    }

    @NonNull
    public String getPid() {
        return pid;
    }
}
