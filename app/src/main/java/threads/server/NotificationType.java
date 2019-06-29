package threads.server;

import androidx.annotation.NonNull;

import static androidx.core.util.Preconditions.checkNotNull;

public enum NotificationType {

    OFFER(1), PROVIDE(2);

    @NonNull
    private final Integer code;

    NotificationType(@NonNull Integer code) {
        checkNotNull(code);
        this.code = code;
    }


    public static NotificationType toNotificationType(@NonNull Integer type) {
        checkNotNull(type);
        if (type.equals(NotificationType.OFFER.getCode())) {
            return NotificationType.OFFER;
        } else if (type.equals(NotificationType.PROVIDE.getCode())) {
            return NotificationType.PROVIDE;
        } else {
            throw new IllegalArgumentException("Could not recognize type");
        }
    }

    @NonNull
    public Integer getCode() {
        return code;
    }

    @NonNull
    public String toString() {
        return getCode().toString();
    }

}
