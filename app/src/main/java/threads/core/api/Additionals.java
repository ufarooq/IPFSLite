package threads.core.api;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Hashtable;

public class Additionals extends Hashtable<String, Additional> {

    @TypeConverter
    public static Additionals toAdditionals(String data) {
        if (data == null) {
            return null;
        }
        Gson gson = new Gson();
        return gson.fromJson(data, Additionals.class);
    }

    @TypeConverter
    public static String toString(Additionals additionals) {
        if (additionals == null) {
            return null;
        }
        Gson gson = new Gson();
        return gson.toJson(additionals);
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
