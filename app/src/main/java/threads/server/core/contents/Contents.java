package threads.server.core.contents;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import threads.ipfs.CID;
import threads.server.core.threads.Thread;

public class Contents extends ArrayList<ContentEntry> {

    private void add(@NonNull Thread thread) {
        CID cid = thread.getContent();
        Objects.requireNonNull(cid);
        String filename = thread.getName();
        long fileSize = thread.getSize();
        String mimeType = thread.getMimeType();
        this.add(new ContentEntry(cid.getCid(), filename, mimeType, fileSize));
    }

    public void add(@NonNull List<Thread> threads) {
        for (Thread thread : threads) {
            add(thread);
        }
    }
}
