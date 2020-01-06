package threads.core.events;

import androidx.annotation.NonNull;

import static androidx.core.util.Preconditions.checkNotNull;

public class EVENTS extends EventsAPI {
    public static final String TAG = EVENTS.class.getSimpleName();


    private EVENTS(final EVENTS.Builder builder) {
        super(builder.eventsDatabase);
    }

    @NonNull
    public static EVENTS createEvents(@NonNull EventsDatabase eventsDatabase) {

        checkNotNull(eventsDatabase);

        return new EVENTS.Builder()
                .eventsDatabase(eventsDatabase)
                .build();
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
