package threads.ipfs;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class IpfsStreamTest {
    private static String TAG = IpfsStreamTest.class.getSimpleName();
    private static Context context;

    @BeforeClass
    public static void setup() {
        context = ApplicationProvider.getApplicationContext();
    }


    @Test
    public void test_string() throws Exception {
        IPFS ipfs = TestEnv.getTestInstance(context);

        String text = "Hello Moin und Zehn Elf";
        CID hash = ipfs.storeText(text);
        assertNotNull(hash);
        List<LinkInfo> links = ipfs.ls(hash, 10, true);
        assertEquals(links.size(), 0);


        byte[] result = ipfs.getData(hash);
        assertEquals(text, new String(result));


        CID hash2 = ipfs.storeText("TEST test");
        assertNotNull(hash2);
        links = ipfs.ls(hash2, 10, true);
        assertEquals(links.size(), 0); // TODO result is odd (should be 1)


    }
}
