package threads.server.core.peers;

import androidx.annotation.NonNull;
import androidx.room.TypeConverter;

import static androidx.core.util.Preconditions.checkNotNull;

public enum UserType {
    VERIFIED(0), UNKNOWN(1);

    @NonNull
    private final Integer code;

    UserType(@NonNull Integer code) {
        checkNotNull(code);
        this.code = code;
    }


    @TypeConverter
    public static UserType toUserType(@NonNull Integer type) {
        checkNotNull(type);
        if (type.equals(UserType.VERIFIED.getCode())) {
            return UserType.VERIFIED;
        } else if (type.equals(UserType.UNKNOWN.getCode())) {
            return UserType.UNKNOWN;
        } else {
            throw new IllegalArgumentException("Could not recognize type");
        }
    }

    @TypeConverter
    public static Integer toInteger(@NonNull UserType type) {
        checkNotNull(type);
        return type.getCode();
    }

    @NonNull
    public Integer getCode() {
        return code;
    }
}
