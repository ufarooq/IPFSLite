package threads.core.events;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Room;

import static androidx.core.util.Preconditions.checkNotNull;

public class EVENTS extends EventsAPI {
    public static final String TAG = EVENTS.class.getSimpleName();

    private static EVENTS INSTANCE = null;

    private EVENTS(final EVENTS.Builder builder) {
        super(builder.eventsDatabase);
    }

    @NonNull
    private static EVENTS createEvents(@NonNull EventsDatabase eventsDatabase) {

        checkNotNull(eventsDatabase);

        return new EVENTS.Builder()
                .eventsDatabase(eventsDatabase)
                .build();
    }

    public static EVENTS getInstance(@NonNull Context context) {
        checkNotNull(context);

        if (INSTANCE == null) {
            synchronized (EVENTS.class) {
                if (INSTANCE == null) {
                    EventsDatabase eventsDatabase =
                            Room.inMemoryDatabaseBuilder(context,
                                    EventsDatabase.class).build();
                    INSTANCE = EVENTS.createEvents(eventsDatabase);
                }
            }
        }
        return INSTANCE;
    }


    public static class Builder {
        EventsDatabase eventsDatabase = null;

        public EVENTS build() {
            checkNotNull(eventsDatabase);

            return new EVENTS(this);
        }

        public Builder eventsDatabase(@NonNull EventsDatabase eventsDatabase) {
            checkNotNull(eventsDatabase);
            this.eventsDatabase = eventsDatabase;
            return this;
        }


    }
}