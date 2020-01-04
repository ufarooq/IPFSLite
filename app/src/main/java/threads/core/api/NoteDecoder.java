package threads.core.api;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;

import java.util.HashMap;

import threads.iota.Entity;
import threads.ipfs.api.CID;
import threads.ipfs.api.Encryption;
import threads.ipfs.api.PID;

import static androidx.core.util.Preconditions.checkNotNull;

public class NoteDecoder {
    private static final String TAG = NoteDecoder.class.getSimpleName();


    @Nullable
    public static Note convert(@NonNull Thread thread, @NonNull Entity entity) {
        checkNotNull(thread);
        checkNotNull(entity);
        Gson gson = new Gson();
        Content content = gson.fromJson(entity.getContent(), Content.class);
        Note note = convert(thread, content);
        if (note != null) {
            note.setHash(entity.getHash());
        }
        return note;
    }

    @Nullable
    public static Note convert(@NonNull Thread thread, @NonNull Content content) {
        checkNotNull(thread);
        checkNotNull(content);
        try {
            String sesKey = thread.getSesKey();

            // NOT ENCRYPTED
            String estType = content.get(Content.EST);
            checkNotNull(estType);
            NoteType noteType = NoteType.toNoteType(Integer.valueOf(estType));

            // NOT ENCRYPTED
            String senderPid = content.get(Content.PID);
            checkNotNull(senderPid);


            // NOT ENCRYPTED
            String senderKey = content.get(Content.PKEY);
            checkNotNull(senderKey);

            // NOT ENCRYPTED
            String date = content.get(Content.DATE);
            checkNotNull(date);
            long timestamp = Long.valueOf(date);

            // ENCRYPTED
            String additions = content.get(Content.ADDS);
            checkNotNull(additions);
            additions = Encryption.decrypt(additions, sesKey);

            // ENCRYPTED
            CID cid = null;
            String cidValue = content.get(Content.CID);
            if (cidValue != null) {
                cidValue = Encryption.decrypt(cidValue, sesKey);
                cid = CID.create(cidValue);
            }

            // ENCRYPTED
            CID image = null;
            String imgValue = content.get(Content.IMG);
            if (imgValue != null) {
                imgValue = Encryption.decrypt(imgValue, sesKey);
                image = CID.create(imgValue);
            }

            // ENCRYPTED
            String senderAlias = content.get(Content.ALIAS);
            checkNotNull(senderAlias);
            senderAlias = Encryption.decrypt(senderAlias, sesKey);

            // ENCRYPTED
            String mimeType = content.get(Content.MIME_TYPE);
            checkNotNull(mimeType);
            mimeType = Encryption.decrypt(mimeType, sesKey);


            Note note = Note.createNote(
                    thread.getIdx(),
                    PID.create(senderPid),
                    senderAlias,
                    senderKey,
                    sesKey,
                    Status.INIT,
                    Kind.IN,
                    noteType,
                    mimeType,
                    timestamp);

            note.setCid(cid);
            note.setImage(image);
            note.setTimestamp(timestamp);

            if (!additions.isEmpty()) {
                HashMap<String, String> adds = Additionals.toHashMap(additions);
                checkNotNull(adds);
                note.setExternalAdditions(adds);
            }

            return note;

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
        return null;
    }
}
