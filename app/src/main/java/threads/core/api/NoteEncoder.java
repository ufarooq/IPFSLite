package threads.core.api;

import androidx.annotation.NonNull;

import java.util.HashMap;

import threads.ipfs.api.CID;
import threads.ipfs.api.Encryption;

import static androidx.core.util.Preconditions.checkNotNull;

public class NoteEncoder {

    @NonNull
    public static Content convert(@NonNull Thread thread, @NonNull Note note) throws Exception {
        checkNotNull(thread);
        checkNotNull(note);

        Content content = new Content();

        // NOT ENCRYPTED
        NoteType type = note.getNoteType();
        content.put(Content.EST, String.valueOf(type.getCode()));

        // NOT ENCRYPTED
        content.put(Content.PID, note.getSenderPid().getPid());  // Useful when stored on tangle


        // NOT ENCRYPTED
        content.put(Content.PKEY, note.getSenderKey()); // Useful when stored on tangle

        // NOT ENCRYPTED
        content.put(Content.DATE, String.valueOf(note.getDate()));


        // ENCRYPTED
        CID cid = note.getCid();
        if (cid != null) {
            content.put(Content.CID,
                    Encryption.encrypt(cid.getCid(), note.getSesKey()));
        }

        // ENCRYPTED
        CID image = note.getImage();
        if (image != null) {
            content.put(Content.IMG,
                    Encryption.encrypt(image.getCid(), note.getSesKey()));
        }

        // ENCRYPTED
        content.put(Content.MIME_TYPE,
                Encryption.encrypt(note.getMimeType(), note.getSesKey()));
        // ENCRYPTED
        content.put(Content.ALIAS,
                Encryption.encrypt(note.getSenderAlias(), note.getSesKey()));

        // ENCRYPTED
        HashMap<String, String> additions = note.getExternalAdditions();
        checkNotNull(additions);
        content.put(Content.ADDS,
                Encryption.encrypt(Additionals.toString(additions), note.getSesKey()));


        return content;

    }
}
