package threads.core.api;

import androidx.annotation.NonNull;

import java.util.HashMap;

import threads.ipfs.api.CID;
import threads.ipfs.api.Encryption;

import static androidx.core.util.Preconditions.checkArgument;
import static androidx.core.util.Preconditions.checkNotNull;

public class NoteRequestEncoder {


    @NonNull
    public static Content convert(@NonNull Thread thread,
                                  @NonNull Note note,
                                  @NonNull String publicKey,
                                  boolean addImage,
                                  boolean addTitle) throws Exception {
        checkNotNull(thread);
        checkNotNull(publicKey);
        checkNotNull(note);
        checkArgument(!publicKey.isEmpty());
        Content content = new Content();

        String sesKey = note.getSesKey();

        // NOT ENCRYPTED
        content.put(Content.PID, note.getSenderPid().getPid());

        // NOT ENCRYPTED
        content.put(Content.PKEY, note.getSenderKey());


        // ENCRYPTED
        content.put(Content.ALIAS, Encryption.encrypt(note.getSenderAlias(), sesKey));

        // NOT ENCRYPTED
        if (!sesKey.isEmpty()) {
            content.put(Content.SKEY, Encryption.encryptRSA(sesKey, publicKey));
        }

        // NOT ENCRYPTED
        content.put(Content.ID, String.valueOf(thread.getTimestamp()));

        // NOT ENCRYPTED
        if (addImage) {
            CID image = thread.getImage();
            if (image != null) {
                content.put(Content.IMG, image.getCid());
            }
        }

        // ENCRYPTED
        if (addTitle) {
            String title = thread.getTitle();
            if (title != null) {
                content.put(Content.TITLE, Encryption.encrypt(title, sesKey));
            }
        }

        // ENCRYPTED
        HashMap<String, String> additions = note.getExternalAdditions();
        checkNotNull(additions);
        if (!additions.isEmpty()) {
            content.put(Content.ADDS, Encryption.encrypt(Additionals.toString(additions), sesKey));
        }

        return content;
    }
}
