package threads.core.api;

import androidx.annotation.NonNull;
import androidx.room.TypeConverter;

import static androidx.core.util.Preconditions.checkNotNull;


public enum Status {
    INIT(0),       // note just created
    DONE(2),       // note is fully defined
    DELETING(3),   // note will be deleted
    ERROR(4);      // note is in error state
    @NonNull
    private final Integer code;

    Status(@NonNull Integer code) {
        checkNotNull(code);
        this.code = code;
    }

    @TypeConverter
    public static Status toStatus(Integer status) {
        checkNotNull(status);
        if (status.equals(Status.DONE.getCode())) {
            return Status.DONE;
        } else if (status.equals(Status.INIT.getCode())) {
            return Status.INIT;
        } else if (status.equals(Status.DELETING.getCode())) {
            return Status.DELETING;
        } else if (status.equals(Status.ERROR.getCode())) {
            return Status.ERROR;
        } else {
            throw new IllegalArgumentException("Could not recognize status");
        }
    }

    @TypeConverter
    public static Integer toInteger(Status status) {
        checkNotNull(status);
        return status.getCode();
    }

    @NonNull
    private Integer getCode() {
        return code;
    }
}
