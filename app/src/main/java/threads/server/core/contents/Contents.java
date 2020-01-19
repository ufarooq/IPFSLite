package threads.server.core.contents;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import threads.ipfs.CID;
import threads.server.core.threads.Thread;

import static androidx.core.util.Preconditions.checkNotNull;

public class Contents extends ArrayList<ContentEntry> {

    private void add(@NonNull Thread thread) {
        CID cid = thread.getContent();
        checkNotNull(cid);
        String filename = thread.getName();
        long filesize = thread.getSize();
        String mimeType = thread.getMimeType();

        String image = null;

        CID imageCID = thread.getThumbnail();

        if (imageCID != null) {
            image = imageCID.getCid();
        }


        this.add(new ContentEntry(cid.getCid(), filename, filesize, mimeType, image));
    }

    public void add(@NonNull List<Thread> threads) {
        for (Thread thread : threads) {
            add(thread);
        }
    }
}
