package threads.core.api;

import java.util.Hashtable;

public class Content extends Hashtable<String, String> {
    public static final String EST = "est";
    public static final String ADDS = "adds";
    public static final String DATE = "date";
    public static final String ALIAS = "alias";   // alias name of the sender
    public static final String PKEY = "pkey";     // public key of the sender
    public static final String SKEY = "skey";     // session key for a thread (encryption, is AesKey)
    public static final String PID = "pid";       // PID of the sender
    public static final String ID = "id";
    public static final String CID = "cid";
    public static final String FCM = "fcm";
    public static final String MIME_TYPE = "type";
    public static final String EXPIRE_DATE = "exp";
    public static final String READ_ONLY = "ro";
    public static final String IMG = "img";
    public static final String ICES = "ices";
    public static final String TYPE = "type";
    public static final String PEERS = "peers";
    public static final String PEER = "peer";
    public static final String TEXT = "text";

    // other stuff
    public static final String FILENAME = "fn";
    public static final String FILESIZE = "fs";
    public static final String HASH = "hash";

    public static final String ERROR = "error";
    public static final String PORT = "port";
    public static final String PROTOCOL = "protocol";
    public static final String HOST = "host";
    public static final String TITLE = "title";
    public static final String SDP = "SDP";
    public static final String MID = "MID";
    public static final String INDEX = "INDEX";
    public static final String USER = "USER";


}
