package threads.core.api;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.PrimaryKey;

import static androidx.core.util.Preconditions.checkNotNull;

@androidx.room.Entity
public class Settings extends Entity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    private final String id;


    Settings(@NonNull String id) {
        checkNotNull(id);
        this.id = id;
    }

    @NonNull
    public static Settings createSettings(@NonNull String id) {
        checkNotNull(id);
        return new Settings(id);
    }


    @NonNull
    public String getId() {
        return id;
    }
}
