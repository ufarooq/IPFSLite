package threads.server.core.peers;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Hashtable;

public class Additions extends Hashtable<String, Additional> {

    @TypeConverter
    public static Additions toAdditions(String data) {
        if (data == null) {
            return null;
        }
        Gson gson = new Gson();
        return gson.fromJson(data, Additions.class);
    }

    @TypeConverter
    public static String toString(Additions additions) {
        if (additions == null) {
            return null;
        }
        Gson gson = new Gson();
        return gson.toJson(additions);
    }


    @TypeConverter
    public static HashMap<String, String> toHashMap(String data) {
        if (data == null) {
            return null;
        }
        Gson gson = new Gson();
        Type type = new TypeToken<HashMap<String, String>>() {
        }.getType();
        return gson.fromJson(data, type);
    }

    @TypeConverter
    public static String toString(HashMap<String, String> hashMap) {
        if (hashMap == null) {
            return null;
        }
        Gson gson = new Gson();
        return gson.toJson(hashMap);
    }
}
