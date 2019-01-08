package threads.server;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.TypeConverter;
import android.arch.persistence.room.TypeConverters;
import android.support.annotation.NonNull;

import java.util.Date;

import static com.google.common.base.Preconditions.checkNotNull;

@Entity(tableName = "Event")
public class Event {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "identifier")
    private final String identifier;


    @NonNull
    @TypeConverters(Event.class)
    @ColumnInfo(name = "date")
    private final Date date;

    Event(@NonNull String identifier, @NonNull Date date) {
        checkNotNull(identifier);
        checkNotNull(date);
        this.identifier = identifier;
        this.date = date;
    }

    static Event createEvent(@NonNull String identifier) {
        return new Event(identifier, new Date());
    }

    @TypeConverter
    public static Date toDate(Long value) {
        return value == null ? null : new Date(value);
    }

    @TypeConverter
    public static Long toLong(Date date) {
        if (date == null) {
            return null;
        } else {
            return date.getTime();
        }
    }

    @NonNull
    public Date getDate() {
        return date;
    }

    @NonNull
    public String getIdentifier() {
        return identifier;
    }
}
