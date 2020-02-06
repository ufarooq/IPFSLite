package threads.server.core.events;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Room;

import static androidx.core.util.Preconditions.checkNotNull;

public class EVENTS extends EventsAPI {

    public static final String ERROR = "ERROR";
    public static final String WARNING = "WARNING";
    public static final String INFO = "INFO";

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

    public void error(@NonNull String content) {
        checkNotNull(content);
        storeEvent(createEvent(ERROR, content));
    }


    public void postWarning(@NonNull String content) {
        java.lang.Thread threadError = new java.lang.Thread(()
                -> warning(content));
        threadError.start();
    }

    public void warning(@NonNull String content) {
        checkNotNull(content);
        storeEvent(createEvent(WARNING, content));
    }

    public void exception(@NonNull Throwable throwable) {
        checkNotNull(throwable);
        error("" + throwable.getLocalizedMessage());
    }

    static class Builder {
        EventsDatabase eventsDatabase = null;

        EVENTS build() {
            checkNotNull(eventsDatabase);

            return new EVENTS(this);
        }

        Builder eventsDatabase(@NonNull EventsDatabase eventsDatabase) {
            checkNotNull(eventsDatabase);
            this.eventsDatabase = eventsDatabase;
            return this;
        }


    }
}
