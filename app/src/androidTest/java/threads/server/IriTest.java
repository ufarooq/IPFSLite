package threads.server;

import android.arch.persistence.room.Room;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.InetAddress;

import threads.iota.IOTA;
import threads.iota.Pair;
import threads.iota.PearlDiver;
import threads.iota.event.EventsDatabase;
import threads.iota.server.Certificate;
import threads.iota.server.Server;
import threads.iota.server.ServerDatabase;
import threads.iota.server.ServerInfo;
import threads.iota.server.ServerVisibility;
import threads.server.daemon.IThreadsConfig;
import threads.server.daemon.IThreadsServer;
import threads.server.daemon.ThreadsServer;
import threads.server.daemon.TransactionDatabase;

import static com.google.common.base.Preconditions.checkNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

@RunWith(AndroidJUnit4.class)
public class IriTest {

    private static final String TAG = IriTest.class.getSimpleName();

    private static Context context;
    private static IThreadsServer threadsServer;
    private static ServerDatabase serverDatabase;
    private static Certificate certificate = Server.createCertificate();


    @BeforeClass
    public static void setup() {


        // Example which uses the ROOM database (Note: other database
        // can be used, but the IThreadsDatabase has to be overloaded)
        context = InstrumentationRegistry.getTargetContext();

        Server serverConfig = Server.createServer("https",
                "nodes.thetangle.org", "443", "", Server.getDefaultServerAlias());
        EventsDatabase eventsDatabase = Room.inMemoryDatabaseBuilder(context, EventsDatabase.class).build();
        TransactionDatabase transactionDatabase = Room.inMemoryDatabaseBuilder(context, TransactionDatabase.class).build();

        serverDatabase = Room.inMemoryDatabaseBuilder(context, ServerDatabase.class).build();

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
    public void checkIPv6Address() {
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
            public Certificate getCertificate() {
                return certificate;
            }

            @Override
            public String getHostname() {
                return ipv6.first.getHostAddress();
            }
        });

        assertTrue(threadsServer.isRunning());

        Server server = Server.createServer(IThreadsServer.HTTPS_PROTOCOL,
                IThreadsServer.getIPv6HostAddress().first,
                String.valueOf(IThreadsServer.TCP_PORT), certificate.getShaHash(), Server.getDefaultServerAlias());

        boolean result = Server.isReachable(server);
        assertTrue(result);

        IOTA tangleClient = IOTA.getIota(serverDatabase, server, new PearlDiver());

        ServerInfo serverInfo = tangleClient.getServerInfo();
        assertTrue(serverInfo.isOnline());


        threadsServer.shutdown();

        assertFalse(threadsServer.isRunning());
    }


}