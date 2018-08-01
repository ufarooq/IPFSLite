package threads.server;

import android.arch.persistence.room.Room;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.apache.commons.lang3.RandomStringUtils;
import org.bouncycastle.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicBoolean;

import threads.iri.ITangle;
import threads.iri.tangle.Encryption;
import threads.iri.tangle.TangleUtils;
import threads.iri.udp.IBytesReader;
import threads.iri.udp.IBytesWriter;
import threads.iri.udp.IUDPApi;
import threads.iri.udp.UDPApi;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class ServerTest {
    private static final String TAG = ServerTest.class.getSimpleName();

    private ITangle threadsDatabase;

    @Before
    public void createDb() {
        Context context = InstrumentationRegistry.getTargetContext();
        threadsDatabase = Room.inMemoryDatabaseBuilder(context, TangleDatabase.class).build();

    }
    @Test
    public void simpleTest() throws Exception {

        String seed = TangleUtils.generateSeed();
        String address = TangleUtils.getAddress(seed);
        System.out.println(address);

        String aesKey = Encryption.generateAESKey();

        String randomString = getRandomString(1000000);
        byte[] bytes = randomString.getBytes();

        byte[] encBytes = Encryption.encrypt(bytes, aesKey);

        System.out.println("Bytes : " + encBytes.length / 1000 + "[kb]");

        byte[][] result = TangleUtils.splitArray(encBytes, IUDPApi.DATGAGRAM_PACKET_SIZE);


        Runnable runnable = () -> {
            try {
                UDPApi.getInstance(threadsDatabase).startServer(new IBytesWriter() {
                    @Override
                    public byte[] getBytes(@NonNull String address, int chunkIndex) {
                        try {
                            return result[chunkIndex];
                        } catch (Throwable e) {
                            System.out.println(e.getLocalizedMessage());
                        }
                        return new byte[0];
                    }
                }, UDPApi.UDP_DAEMON_DATA_PORT);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        };

        Thread thread = new Thread(runnable);
        thread.start();


        while (!UDPApi.getInstance(threadsDatabase).isRunning()) {
            try {
                // thread to sleep for 1000 milliseconds
                Thread.sleep(1000);
            } catch (Exception e) {
                throw new RuntimeException(e.getLocalizedMessage());
            }
        }


        Reader bytesReader = new Reader();
        InetAddress server = InetAddress.getByName(threads.iri.Utility.getIPAddress(true));
        UDPApi.getInstance(threadsDatabase).startClient(address, bytesReader, server, IUDPApi.UDP_DAEMON_DATA_PORT);


        while (!bytesReader.isFinised()) {
            try {
                // thread to sleep for 1000 milliseconds
                Thread.sleep(1000);
            } catch (Exception e) {
                System.out.println(e.getLocalizedMessage());
            }
        }

        byte[] desBytes = Encryption.decrypt(bytesReader.getData(), aesKey);
        assertEquals(new String(desBytes), new String(bytes));
        assertEquals(bytesReader.getMessage(), "");

    }


    private String getRandomString(int number) {
        return RandomStringUtils.randomAlphabetic(number);

    }

    private class Reader implements IBytesReader {
        byte[] data = new byte[0];
        private String message = "";
        private AtomicBoolean finished = new AtomicBoolean(false);

        @Override
        public void addData(String address, int index, byte[] chunk) {
            data = Arrays.concatenate(data, chunk);
        }

        @Override
        public boolean isDataLoaded(@NonNull String fileUid, int chunkIndex) {
            return false;
        }

        @Override
        public void finish() {
            finished.set(true);
        }

        @Override
        public boolean isFinised() {
            return finished.get();
        }

        @Override
        public String getMessage() {
            return message;
        }

        @Override
        public void setMessage(@NonNull String message) {
            this.message = message;
        }

        public byte[] getData() {
            return data;
        }


    }
}

