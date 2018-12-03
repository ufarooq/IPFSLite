package threads.server.event;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.TypeConverters;
import android.support.annotation.NonNull;

import java.util.Date;

import static com.google.common.base.Preconditions.checkNotNull;

@Entity(tableName = "Event")
public class Event implements IEvent {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "identifier")
    private final String identifier;


    @NonNull
    @TypeConverters(IEvent.class)
    @ColumnInfo(name = "date")
    private final Date date;

    Event(@NonNull String identifier, @NonNull Date date) {
        checkNotNull(identifier);
        checkNotNull(date);
        this.identifier = identifier;
        this.date = date;
    }

    public static Event createEvent(@NonNull String identifier) {
        return new Event(identifier, new Date());
    }

    @NonNull
    public Date getDate() {
        return date;
    }


    @NonNull
    @Override
    public String getIdentifier() {
        return identifier;
    }


}
