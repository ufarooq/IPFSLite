package threads.ipfs;


import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import mobile.Reader;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class IpfsFileReaderTest {

    private static String TAG = IpfsStreamTest.class.getSimpleName();
    private static Context context;

    @BeforeClass
    public static void setup() {
        context = ApplicationProvider.getApplicationContext();
    }


    @Test
    public void test_string() throws Exception {
        IPFS ipfs = TestEnv.getTestInstance(context);

        String text = "Hello Moin";
        CID hash = ipfs.storeText(text, "", false);
        assertNotNull(hash);
        int txtSize = text.length();


        Reader fileReader = ipfs.getReader(hash, true);
        assertNotNull(fileReader);

        assertEquals(txtSize, fileReader.getSize());

        fileReader.readAt(1, 2);
        long read = fileReader.getRead();
        assertEquals(2, read);

        byte[] bytes = fileReader.getData();
        assertEquals(new String(bytes), "el");


        fileReader.readAt(0, 100);
        read = fileReader.getRead();
        assertEquals(txtSize, read);

        bytes = fileReader.getData();
        assertEquals(new String(bytes), text);


        fileReader.readAt(txtSize - 2, 3);
        read = fileReader.getRead();
        assertEquals(2, read);

        bytes = fileReader.getData();
        assertEquals(new String(bytes), "in");


        fileReader.close();


    }
}
