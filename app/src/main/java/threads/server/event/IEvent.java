package threads.server.event;

import android.arch.persistence.room.TypeConverter;

import java.util.Date;

public interface IEvent {
    @TypeConverter
    static Date toDate(Long value) {
        return value == null ? null : new Date(value);
    }

    @TypeConverter
    static Long toLong(Date date) {
        if (date == null) {
            return null;
        } else {
            return date.getTime();
        }
    }

    String getIdentifier();

    Date getDate();
}
