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

public class NoteRequestDecoder {
    private static final String TAG = NoteRequestDecoder.class.getSimpleName();

    @Nullable
    public static Thread convert(@NonNull Entity entity, @NonNull String privateKey) {
        checkNotNull(privateKey);
        checkNotNull(entity);
        Gson gson = new Gson();
        Content content = gson.fromJson(entity.getContent(), Content.class);
        Thread thread = convert(content, privateKey);
        if (thread != null) {
            thread.setHash(entity.getHash());
        }
        return thread;
    }

    @Nullable
    public static Thread convert(@NonNull Content content, @NonNull String privateKey) {
        checkNotNull(privateKey);
        checkNotNull(content);
        try {
            // NOT ENCRYPTED
            String sesKey = "";

            // ENCRYPTED RSA
            String encSesKey = content.get(Content.SKEY);
            if (encSesKey != null) {
                sesKey = Encryption.decryptRSA(encSesKey, privateKey);
            }

            // NOT ENCRYPTED
            String senderPid = content.get(Content.PID);
            checkNotNull(senderPid);


            // NOT ENCRYPTED
            String senderKey = content.get(Content.PKEY);
            checkNotNull(senderKey);


            // ENCRYPTED
            String senderAlias = content.get(Content.ALIAS);
            checkNotNull(senderAlias);
            senderAlias = Encryption.decrypt(senderAlias, sesKey);

            // NOT ENCRYPTED
            String id = content.get(Content.ID);
            checkNotNull(id);
            long timestamp = Long.valueOf(id);

            // NOT ENCRYPTED
            String additions = content.get(Content.ADDS);


            // NOT ENCRYPTED
            CID image = null;
            String imgValue = content.get(Content.IMG);
            if (imgValue != null) {
                image = CID.create(imgValue);
            }

            // ENCRYPTED
            String title = content.get(Content.TITLE);
            if (title != null) {
                title = Encryption.decrypt(title, sesKey);
            }

            Thread newThread = Thread.createThread(
                    Status.INIT,
                    PID.create(senderPid),
                    senderAlias,
                    senderKey,
                    sesKey,
                    Kind.IN,
                    timestamp,
                    0L);
            newThread.setRequest(true);
            newThread.setTitle(title);
            newThread.setImage(image);
            newThread.setTimestamp(timestamp);
            newThread.addMember(PID.create(senderPid));


            if (additions != null && additions.isEmpty()) {
                HashMap<String, String> adds = Additionals.toHashMap(
                        Encryption.decrypt(additions, sesKey));
                checkNotNull(adds);
                newThread.setExternalAdditions(adds);
            }


            return newThread;
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
        return null;
    }

}
