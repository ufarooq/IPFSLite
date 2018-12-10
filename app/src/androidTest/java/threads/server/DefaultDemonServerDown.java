package threads.server;

import android.arch.persistence.room.Room;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import threads.iota.IOTA;
import threads.iota.PearlDiver;
import threads.iota.server.Server;
import threads.iota.server.ServerDatabase;
import threads.server.daemon.IThreadsConfig;
import threads.server.daemon.IThreadsServer;
import threads.server.daemon.ThreadsServer;
import threads.server.daemon.TransactionDatabase;
import threads.server.event.EventsDatabase;

import static junit.framework.TestCase.assertTrue;

@RunWith(AndroidJUnit4.class)
public class DefaultDemonServerDown {

    private static IThreadsServer threadsServer;
    private static Context context;
    private static ServerDatabase serverDatabase;

    @BeforeClass
    public static void setUp() {
        context = InstrumentationRegistry.getTargetContext();


        EventsDatabase eventsDatabase = Room.inMemoryDatabaseBuilder(context, EventsDatabase.class).build();
        TransactionDatabase transactionDatabase = Room.inMemoryDatabaseBuilder(context, TransactionDatabase.class).build();

        serverDatabase = Room.inMemoryDatabaseBuilder(context, ServerDatabase.class).build();
        threadsServer = ThreadsServer.createThreadServer(context, transactionDatabase, eventsDatabase);

        IThreadsConfig threadsConfig = IThreadsServer.getHttpsThreadsConfig(context);
        threadsServer.start(threadsConfig);

    }

    @AfterClass
    public static void tearDown() {
        threadsServer.shutdown();
    }

    @Test
    public void testConnection() {

        assertTrue(threadsServer.isRunning());

        IThreadsConfig threadsConfig = IThreadsServer.getHttpsThreadsConfig(context);
        Server server = IThreadsServer.getServer(context, threadsConfig);

        IOTA tangleServerConnect = IOTA.getIota(serverDatabase, server, new PearlDiver());

        assertTrue(tangleServerConnect.getServerInfo().isOnline());


    }
}