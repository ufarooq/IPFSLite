package threads.server.core.peers;

import java.util.Hashtable;

public class Content extends Hashtable<String, String> {
    public static final String EST = "est";
    public static final String ADDS = "adds";
    public static final String DATE = "date";
    public static final String ALIAS = "alias";   // alias name of the sender
    public static final String PKEY = "pkey";     // public key of the sender
    public static final String PID = "pid";       // PID of the sender
    public static final String CID = "cid";


    public static final String PEERS = "peers";


}
