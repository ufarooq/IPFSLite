package threads.server;

import androidx.annotation.NonNull;

import java.util.ArrayList;

import threads.core.api.Content;
import threads.core.api.Thread;
import threads.ipfs.api.CID;

import static androidx.core.util.Preconditions.checkNotNull;

public class ContentFiles extends ArrayList<ContentFile> {

    public void add(@NonNull Thread thread) {
        CID cid = thread.getCid();
        checkNotNull(cid);
        String filename = thread.getAdditional(Content.FILENAME);
        String filesize = thread.getAdditional(Content.FILESIZE);
        this.add(new ContentFile(cid.getCid(), filename, filesize));
    }
}
