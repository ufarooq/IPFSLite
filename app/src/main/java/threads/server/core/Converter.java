package threads.server.core;

import androidx.room.TypeConverter;

import threads.ipfs.CID;
import threads.ipfs.PID;


public class Converter {

    @TypeConverter
    public static PID toPID(String pid) {
        return pid == null ? null : PID.create(pid);
    }

    @TypeConverter
    public static String toString(PID pid) {
        if (pid == null) {
            return null;
        } else {
            return pid.getPid();
        }
    }


    @TypeConverter
    public static CID toCID(String cid) {
        return cid == null ? null : CID.create(cid);
    }

    @TypeConverter
    public static String toString(CID cid) {
        if (cid == null) {
            return null;
        } else {
            return cid.getCid();
        }
    }
}
