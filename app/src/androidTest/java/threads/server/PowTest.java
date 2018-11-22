package threads.server;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.google.common.collect.Iterables;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import threads.iri.client.dto.response.GetAttachToTangleResponse;
import threads.iri.tangle.Encryption;
import threads.iri.tangle.TangleUtils;


@RunWith(AndroidJUnit4.class)
public class PowTest {
    private static String TAG = PowTest.class.getSimpleName();

    private static Context context;

    @BeforeClass
    public static void setup() {


        // Example which uses the ROOM database (Note: other database
        // can be used, but the IThreadsDatabase has to be overloaded)
        context = InstrumentationRegistry.getTargetContext();

    }

    @Test
    public void test_splitForTransfer() throws Exception {

        String trunkTransaction = "DXLCWCFIFRXXDJEOWMHQZDNGWCEMPJXMBMMCCSLGOZRJBHYYRQWZ9SRIOWPVQAYZVRPNSX9SGRLEZ9999";
        String branchTransaction = "DXLCWCFIFRXXDJEOWMHQZDNGWCEMPJXMBMMCCSLGOZRJBHYYRQWZ9SRIOWPVQAYZVRPNSX9SGRLEZ9999";

        String aesKey = Encryption.generateAESKey();

        String randomString = getRandomString(100);
        byte[] bytes = randomString.getBytes();

        byte[] encBytes = Encryption.encrypt(bytes, aesKey);

        Log.e(TAG, "Bytes : " + encBytes.length / 1000 + "[kb]");
        String seed = TangleUtils.generateSeed();
        String address = TangleUtils.getAddress(seed);
        Log.e(TAG, address);
        long now = System.currentTimeMillis();
        List<String> result = TangleUtils.splitForTransfer(address,
                TangleUtils.randomTag(),
                encBytes);

        for (String tryte : result) {
            Log.e(TAG, tryte);
        }
        Log.e(TAG, "Number of Transfers (Pre) : " + result.size() +
                " Time : " + ((System.currentTimeMillis() - now) / 100) + "[s]");
        Thread.sleep(1000);
        now = System.currentTimeMillis();
        GetAttachToTangleResponse res = TangleUtils.localPow(
                new PearlDiver(context),
                trunkTransaction,
                branchTransaction, 14,
                Iterables.toArray(result, String.class));
        for (String tryte : res.getTrytes()) {
            Log.e(TAG, tryte.length() + " " + tryte);
            if (tryte.length() != TangleUtils.TRYTES_SIZE)
                throw new RuntimeException("Invalid length");
        }

        Log.e(TAG, "Number of Transfers (RS): " + result.size() +
                " Time : " + ((System.currentTimeMillis() - now) / 1000) + "[s]");

        Thread.sleep(1000);
        now = System.currentTimeMillis();
        res = TangleUtils.localPow(
                trunkTransaction,
                branchTransaction, 14,
                Iterables.toArray(result, String.class));


        for (String tryte : res.getTrytes()) {
            Log.e(TAG, tryte.length() + " " + tryte);
            if (tryte.length() != TangleUtils.TRYTES_SIZE)
                throw new RuntimeException("Invalid length");
        }

        Log.e(TAG, "Number of Transfers (CPU): " + result.size() +
                " Time : " + ((System.currentTimeMillis() - now) / 1000) + "[s]");

    }


    private String getRandomString(int number) {
        return "" + RandomStringUtils.random(number);

    }
}
