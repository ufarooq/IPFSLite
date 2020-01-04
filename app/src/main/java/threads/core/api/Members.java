package threads.core.api;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashSet;

import threads.ipfs.api.PID;


public class Members extends HashSet<PID> {
    @TypeConverter
    public static Members toMembers(String data) {
        if (data == null) {
            return new Members();
        }
        Gson gson = new Gson();
        Type listType = new TypeToken<Members>() {
        }.getType();

        return gson.fromJson(data, listType);
    }

    @TypeConverter
    public static String toString(Members members) {
        Gson gson = new Gson();
        return gson.toJson(members);
    }
}
