package threads.server;

import androidx.annotation.NonNull;
import androidx.room.TypeConverter;

import static com.google.common.base.Preconditions.checkNotNull;

public enum MessageKind {
    CMD(0), ERROR(1), INFO(2);

    @NonNull
    private final Integer code;

    MessageKind(@NonNull Integer code) {
        checkNotNull(code);
        this.code = code;
    }

    @TypeConverter
    public static MessageKind toMessageKind(Integer messageKind) {
        checkNotNull(messageKind);
        if (messageKind.equals(MessageKind.CMD.getCode())) {
            return MessageKind.CMD;
        } else if (messageKind.equals(MessageKind.ERROR.getCode())) {
            return MessageKind.ERROR;
        } else if (messageKind.equals(MessageKind.INFO.getCode())) {
            return MessageKind.INFO;
        } else {
            throw new IllegalArgumentException("Could not recognize status");
        }
    }

    @TypeConverter
    public static Integer toInteger(@NonNull MessageKind messageKind) {
        checkNotNull(messageKind);
        return messageKind.getCode();
    }

    @NonNull
    private Integer getCode() {
        return code;
    }
}
