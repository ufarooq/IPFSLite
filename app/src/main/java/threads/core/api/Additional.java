package threads.core.api;

import androidx.annotation.NonNull;

import static androidx.core.util.Preconditions.checkNotNull;

public class Additional {
    @NonNull
    private final Boolean internal;
    @NonNull
    private String value;

    private Additional(@NonNull String value, @NonNull Boolean internal) {
        this.value = value;
        this.internal = internal;
    }

    @NonNull
    public static Additional createAdditional(@NonNull String value, @NonNull Boolean internal) {
        checkNotNull(value);
        checkNotNull(internal);
        return new Additional(value, internal);
    }

    @NonNull
    public String getValue() {
        return value;
    }

    public void setValue(@NonNull String value) {
        checkNotNull(value);
        this.value = value;
    }

    @NonNull
    Boolean getInternal() {
        return internal;
    }

}
