package threads.core.api;

import androidx.annotation.NonNull;
import androidx.room.TypeConverter;

import static androidx.core.util.Preconditions.checkNotNull;

public enum Kind {
    IN(0), OUT(1);
    @NonNull
    private final Integer code;

    Kind(@NonNull Integer code) {
        checkNotNull(code);
        this.code = code;
    }

    @TypeConverter
    public static Kind toKind(Integer type) {
        if (type == null) {
            return null;
        }
        if (type.equals(Kind.IN.getCode())) {
            return Kind.IN;
        } else if (type.equals(Kind.OUT.getCode())) {
            return Kind.OUT;
        } else {
            throw new IllegalArgumentException("Could not recognize type");
        }
    }

    @TypeConverter
    public static Integer toInteger(Kind type) {

        if (type == null) {
            return null;
        }
        return type.getCode();
    }

    @NonNull
    private Integer getCode() {
        return code;
    }
}


