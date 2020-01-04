package threads.core.api;

import androidx.annotation.NonNull;
import androidx.room.TypeConverter;

import static androidx.core.util.Preconditions.checkNotNull;

public enum NoteType {
    MESSAGE(1), INFO(2), DATA(3), LINK(4), AUDIO(5), HTML(6),
    THREAD_REQUEST(7), THREAD_LEAVE(8), THREAD_JOIN(9), THREAD_REJECT(10), THREAD_PUBLISH(11),
    CALL(12), LOCATION(13), CONTACT(14), VIDEO_CALL(15);

    @NonNull
    private final Integer code;

    NoteType(@NonNull Integer code) {
        checkNotNull(code);
        this.code = code;
    }


    @TypeConverter
    public static NoteType toNoteType(@NonNull Integer type) {
        checkNotNull(type);
        if (type.equals(NoteType.MESSAGE.getCode())) {
            return NoteType.MESSAGE;
        } else if (type.equals(NoteType.INFO.getCode())) {
            return NoteType.INFO;
        } else if (type.equals(NoteType.DATA.getCode())) {
            return NoteType.DATA;
        } else if (type.equals(NoteType.AUDIO.getCode())) {
            return NoteType.AUDIO;
        } else if (type.equals(NoteType.HTML.getCode())) {
            return NoteType.HTML;
        } else if (type.equals(NoteType.THREAD_REQUEST.getCode())) {
            return NoteType.THREAD_REQUEST;
        } else if (type.equals(NoteType.THREAD_LEAVE.getCode())) {
            return NoteType.THREAD_LEAVE;
        } else if (type.equals(NoteType.THREAD_JOIN.getCode())) {
            return NoteType.THREAD_JOIN;
        } else if (type.equals(NoteType.THREAD_REJECT.getCode())) {
            return NoteType.THREAD_REJECT;
        } else if (type.equals(NoteType.LINK.getCode())) {
            return NoteType.LINK;
        } else if (type.equals(NoteType.THREAD_PUBLISH.getCode())) {
            return NoteType.THREAD_PUBLISH;
        } else if (type.equals(NoteType.CALL.getCode())) {
            return NoteType.CALL;
        } else if (type.equals(NoteType.VIDEO_CALL.getCode())) {
            return NoteType.VIDEO_CALL;
        } else if (type.equals(NoteType.LOCATION.getCode())) {
            return NoteType.LOCATION;
        } else if (type.equals(NoteType.CONTACT.getCode())) {
            return NoteType.CONTACT;
        } else {
            throw new IllegalArgumentException("Could not recognize type");
        }
    }

    @TypeConverter
    public static Integer toInteger(@NonNull NoteType type) {
        checkNotNull(type);
        return type.getCode();
    }

    @NonNull
    public Integer getCode() {
        return code;
    }

}
