package threads.core.api;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

import java.util.HashMap;

import threads.ipfs.api.Encryption;

import static androidx.core.util.Preconditions.checkNotNull;

public class UserEncoder {

    @NonNull
    public static String convert(@NonNull User user, @NonNull String aesKey) {
        checkNotNull(user);
        checkNotNull(aesKey);
        Content content = new Content();


        content.put(Content.PKEY, user.getPublicKey());
        content.put(Content.ALIAS, user.getAlias());
        content.put(Content.PID, user.getPid());

        HashMap<String, String> additions = user.getExternalAdditions();
        checkNotNull(additions);
        content.put(Content.ADDS, Additionals.toString(additions));

        Gson gson = new Gson();
        String data = gson.toJson(content);

        try {
            data = Encryption.encrypt(data, aesKey);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

        return data;
    }

}
