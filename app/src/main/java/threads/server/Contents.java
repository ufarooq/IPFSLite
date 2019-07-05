package threads.server;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import threads.core.api.Content;
import threads.core.api.Thread;
import threads.ipfs.api.CID;

import static androidx.core.util.Preconditions.checkNotNull;

public class Contents extends ArrayList<ContentEntry> {

    public void add(@NonNull Thread thread) {
        CID cid = thread.getCid();
        checkNotNull(cid);
        String filename = thread.getAdditional(Content.FILENAME);
        String filesize = thread.getAdditional(Content.FILESIZE);
        this.add(new ContentEntry(cid.getCid(), filename, filesize));
    }

    public void add(@NonNull List<Thread> threads) {
        for (Thread thread : threads) {
            add(thread);
        }
    }
}