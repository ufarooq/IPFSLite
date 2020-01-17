package threads.ipfs;


import android.content.Context;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class IpfsDaemonTest {
    private static String TAG = IpfsDaemonTest.class.getSimpleName();
    private static Context context;

    @BeforeClass
    public static void setup() {
        context = ApplicationProvider.getApplicationContext();
    }

    @Test
    public void dummy() {
        assertNotNull(context);
    }

    //@Test
    public void testConnectionBytes() throws Exception {
        IPFS ipfs = TestEnv.getTestInstance(context);

        String content = getRandomString(100);
        CID hash58Base = ipfs.storeText(content);
        Log.e(TAG, hash58Base.getCid());

        byte[] contentLocal = ipfs.getData(hash58Base);
        assertEquals(content, new String(contentLocal));


    }

    private String getRandomString(int number) {
        return "" + RandomStringUtils.randomAscii(number);
    }

}
