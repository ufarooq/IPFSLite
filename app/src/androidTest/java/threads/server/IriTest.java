package threads.server;

import android.arch.persistence.room.Room;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import threads.iri.Daemon;
import threads.iri.IDaemon;
import threads.iri.IThreadsTangle;


@RunWith(AndroidJUnit4.class)
public class IriTest {


    @Test
    public void checkDaemon() {

        Context context = InstrumentationRegistry.getTargetContext();
        IThreadsTangle tangle = Room.inMemoryDatabaseBuilder(context, ThreadsTangleDatabase.class).build();


        //InputStream snapshot_mainnet = context.getResources().openRawResource(R.raw.snapshot_mainnet);
        //InputStream snapshot_mainnet_sig = context.getResources().openRawResource(R.raw.snapshot_mainnet_sig);

        IDaemon miri = Daemon.getInstance();
        miri.start(tangle, "14265");

        try {
            Thread.sleep(10000000);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        miri.shutdown();


    }


}
