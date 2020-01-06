package threads.core.events;

import androidx.annotation.NonNull;
import androidx.room.TypeConverter;

import static androidx.core.util.Preconditions.checkNotNull;

public enum MessageKind {
    CMD(0), ERROR(1), INFO(2), DEBUG(3);

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
        } else if (messageKind.equals(MessageKind.DEBUG.getCode())) {
            return MessageKind.DEBUG;
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
