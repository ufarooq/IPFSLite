package threads.server.core.peers;

import androidx.annotation.Nullable;
import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class Addresses extends ArrayList<String> {
    @TypeConverter
    @Nullable
    public static Addresses toAddresses(@Nullable String data) {
        if (data == null) {
            return null;
        }
        Gson gson = new Gson();
        Type listType = new TypeToken<Addresses>() {
        }.getType();

        return gson.fromJson(data, listType);
    }

    @TypeConverter
    @Nullable
    public static String toString(@Nullable Addresses addresses) {
        if (addresses == null) {
            return null;
        }
        Gson gson = new Gson();
        return gson.toJson(addresses);
    }
}
