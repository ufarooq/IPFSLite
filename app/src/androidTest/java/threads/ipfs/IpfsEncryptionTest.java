package threads.ipfs;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class IpfsEncryptionTest {
    private static Context context;

    @BeforeClass
    public static void setup() {
        context = ApplicationProvider.getApplicationContext();
    }

    @Test
    public void add_get_test() throws Exception {


        IPFS ipfs = TestEnv.getTestInstance(context);

        String aesKey = Encryption.generateAESKey();
        String content = "Moin";
        CID cid = ipfs.storeText(content, aesKey);
        assertNotNull(cid);

        String bytes = ipfs.getText(cid, aesKey);

        assertEquals(bytes, content);

    }
}
