package threads.server.core.peers;

import java.util.Hashtable;

public class Content extends Hashtable<String, String> {
    public static final String PID = "pid";       // PID of the sender
    public static final String CID = "cid";
    public static final String IDX = "idx";
    public static final String HOST = "host";
    public static final String PORT = "port";
    public static final String INET6 = "inet6";

}
