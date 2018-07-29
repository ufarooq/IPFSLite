package threads.server;

import android.arch.persistence.room.Room;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.google.common.collect.Iterables;
import com.iota.iri.TransactionValidator;
import com.iota.iri.conf.Configuration;
import com.iota.iri.controllers.TipsViewModel;
import com.iota.iri.controllers.TransactionViewModel;
import com.iota.iri.model.Hash;
import com.iota.iri.network.Neighbor;
import com.iota.iri.network.Node;
import com.iota.iri.network.TransactionRequester;
import com.iota.iri.network.UDPNeighbor;
import com.iota.iri.network.UDPReceiver;
import com.iota.iri.utils.Converter;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import jota.model.Transaction;
import threads.iri.IThreadsTangle;
import threads.iri.ITransactionStorage;
import threads.iri.tangle.TangleUtils;

import static junit.framework.TestCase.assertEquals;

@RunWith(AndroidJUnit4.class)
public class NodeTest {
    private final static int TRYTES_SIZE = 2673;

    private IThreadsTangle threadsDatabase;

    @Before
    public void createDb() {
        Context context = InstrumentationRegistry.getTargetContext();
        threadsDatabase = Room.inMemoryDatabaseBuilder(context, ThreadsTangleDatabase.class).build();

    }

    @Test
    public void simpleTest() throws Exception {
        TipsViewModel tipsViewModel = new TipsViewModel();
        Configuration configuration = new Configuration();
        TransactionRequester transactionRequester = new TransactionRequester(threadsDatabase);
        long snapshotTimestamp = configuration.longNum(Configuration.DefaultConfSettings.SNAPSHOT_TIME);
        int udpPort = configuration.integer(Configuration.DefaultConfSettings.UDP_RECEIVER_PORT);

        TransactionValidator transactionValidator = new TransactionValidator(threadsDatabase, tipsViewModel, transactionRequester,
                snapshotTimestamp);
        Node node = new Node(configuration, threadsDatabase, transactionValidator, transactionRequester, tipsViewModel);
        UDPReceiver udpReceiver = new UDPReceiver(udpPort, node, configuration.integer(Configuration.DefaultConfSettings.TRANSACTION_PACKET_SIZE));

        transactionValidator.init(false, configuration.integer(Configuration.DefaultConfSettings.MWM));
        transactionRequester.init(configuration.doubling(Configuration.DefaultConfSettings.P_REMOVE_REQUEST.name()));
        udpReceiver.init();
        node.init();

        String randomString = getRandomString(100);
        byte[] bytes = randomString.getBytes();
        System.out.println("Bytes : " + bytes.length / 1000 + "[kb]");
        String seed = TangleUtils.generateSeed();
        String address = TangleUtils.getAddress(seed);
        System.out.println(address);
        List<String> result = TangleUtils.splitForTransfer(address,
                TangleUtils.randomTag(),
                bytes);


        String trunkTransaction = "DXLCWCFIFRXXDJEOWMHQZDNGWCEMPJXMBMMCCSLGOZRJBHYYRQWZ9SRIOWPVQAYZVRPNSX9SGRLEZ9999";
        String branchTransaction = "DXLCWCFIFRXXDJEOWMHQZDNGWCEMPJXMBMMCCSLGOZRJBHYYRQWZ9SRIOWPVQAYZVRPNSX9SGRLEZ9999";

        List<String> checkList = TangleUtils.localPow(
                trunkTransaction,
                branchTransaction,
                1,
                Iterables.toArray(result, String.class));

        List<Transaction> transactions = new ArrayList<>();
        for (String tryte : checkList) {
            Transaction transaction = new Transaction(tryte);
            transactions.add(transaction);
        }
        Transaction trans = transactions.get(0);
        String hash = trans.getHash();
        String tryte = trans.toTrytes();
        int[] trits = Converter.allocateTritsForTrytes(TRYTES_SIZE);
        Converter.trits(tryte, trits, 0);

        byte[] transaction = Converter.allocateBytesForTrits(trits.length);
        Converter.bytes(trits, 0, transaction, 0, trits.length);

        Hash requestedHash = Hash.convertToHash(hash);
        TransactionViewModel transactionViewModel =
                new TransactionViewModel(threadsDatabase, transaction, requestedHash);
        assertEquals(transactionViewModel.getAddress(), address);
        InetAddress inetAddress = InetAddress.getLocalHost();
        Neighbor neighbor = new UDPNeighbor(
                new InetSocketAddress(inetAddress, udpPort), node.getUdpSocket(), true);

        node.addNeighbor(neighbor);
        node.sendPacket(transactionViewModel, neighbor);

        try {
            // thread to sleep for 1000 milliseconds
            Thread.sleep(10000);
        } catch (Exception e) {
            throw new RuntimeException(e.getLocalizedMessage());
        }

        ITransactionStorage storage = threadsDatabase.getTransactionStorage(hash);

        assertEquals(storage.getAddress(), trans.getAddress());
        assertEquals(storage.getTrunk(), trans.getTrunkTransaction());
        assertEquals(storage.getTag(), trans.getTag());
        assertEquals(storage.getBundle(), trans.getBundle());
        assertEquals(storage.getBranch(), trans.getBranchTransaction());
        assertEquals(storage.getHash(), trans.getHash());

        // TODO test shutdown


    }


    private String getRandomString(int number) {
        return "" + RandomStringUtils.random(number);

    }
}
