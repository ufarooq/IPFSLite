package threads.server;

import android.arch.persistence.room.Room;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Pair;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.InetAddress;

import threads.iota.IOTA;
import threads.iota.PearlDiver;
import threads.server.daemon.IThreadsConfig;
import threads.server.daemon.IThreadsServer;
import threads.server.daemon.ThreadsServer;
import threads.server.daemon.TransactionDatabase;
import threads.server.event.EventsDatabase;

import static com.google.common.base.Preconditions.checkNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

@RunWith(AndroidJUnit4.class)
public class IriTest {

    private static final String TAG = IriTest.class.getSimpleName();

    private static Context context;
    private static IThreadsServer threadsServer;


    @BeforeClass
    public static void setup() {


        // Example which uses the ROOM database (Note: other database
        // can be used, but the IThreadsDatabase has to be overloaded)
        context = InstrumentationRegistry.getTargetContext();

        EventsDatabase eventsDatabase = Room.inMemoryDatabaseBuilder(context, EventsDatabase.class).build();
        TransactionDatabase transactionDatabase = Room.inMemoryDatabaseBuilder(context, TransactionDatabase.class).build();

        threadsServer = ThreadsServer.createThreadServer(context, transactionDatabase, eventsDatabase);
        IThreadsConfig threadsConfig = IThreadsServer.getHttpThreadsConfig(context);
        threadsServer.start(threadsConfig);


    }

    @Test
    public void checkThreadsServer() {


        assertTrue(threadsServer.isRunning());

        threadsServer.shutdown();

        assertFalse(threadsServer.isRunning());

        IThreadsConfig threadsConfig = IThreadsServer.getHttpThreadsConfig(context);

        threadsServer.start(threadsConfig);

        assertTrue(threadsServer.isRunning());

        threadsServer.shutdown();

        assertFalse(threadsServer.isRunning());

    }


    @Test
    public void checkIPv6Address() throws IOException {
        Pair<InetAddress, ServerVisibility> ipv6 = ThreadsServer.getInetAddress(false);
        checkNotNull(ipv6);
        ipv6.first.getHostAddress();
        ipv6.first.getAddress();


        threadsServer.start(new IThreadsConfig() {
            @NonNull
            @Override
            public String getPort() {
                return String.valueOf(IThreadsServer.TCP_PORT);
            }

            @Override
            public String getHostname() {
                return ipv6.first.getHostAddress();
            }
        });

        assertTrue(threadsServer.isRunning());


        IOTA tangleClient = IOTA.getIota(IThreadsServer.HTTPS_PROTOCOL,
                IThreadsServer.getIPv6HostAddress().first,
                String.valueOf(IThreadsServer.TCP_PORT), new PearlDiver());


        assertTrue(tangleClient.getNodeInfo() != null);


        threadsServer.shutdown();

        assertFalse(threadsServer.isRunning());
    }


}