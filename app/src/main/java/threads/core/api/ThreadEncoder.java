package threads.core.api;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

import java.util.HashMap;

import threads.ipfs.api.CID;
import threads.ipfs.api.Encryption;

import static androidx.core.util.Preconditions.checkNotNull;

public class ThreadEncoder {


    @NonNull
    public static String convert(@NonNull Thread thread, @NonNull String aesKey) throws Exception {
        checkNotNull(thread);
        checkNotNull(aesKey);
        Content content = new Content();
        Gson gson = new Gson();

        content.put(Content.EXPIRE_DATE, String.valueOf(thread.getExpire()));

        CID cid = thread.getCid();
        if (cid != null) {
            content.put(Content.CID, cid.getCid());
        }

        CID image = thread.getImage();
        if (image != null) {
            content.put(Content.IMG, image.getCid());
        }

        // ENCRYPTED
        content.put(Content.ALIAS, Encryption.encrypt(thread.getSenderAlias(), aesKey));

        content.put(Content.SKEY, thread.getSesKey());

        content.put(Content.DATE, String.valueOf(thread.getDate()));


        HashMap<String, String> additions = thread.getExternalAdditions();
        checkNotNull(additions);
        // ENCRYPTED
        content.put(Content.ADDS, Encryption.encrypt(Additionals.toString(additions), aesKey));

        content.put(Content.PID, thread.getSenderPid().getPid());

        content.put(Content.PKEY, thread.getSenderKey());

        return gson.toJson(content);

    }
}
