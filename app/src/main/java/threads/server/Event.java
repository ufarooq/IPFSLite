package threads.server;

import java.util.Date;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverter;
import androidx.room.TypeConverters;

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
