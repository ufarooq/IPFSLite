package threads.core.api;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;

import threads.iota.Entity;
import threads.ipfs.api.Encryption;
import threads.ipfs.api.PID;

import static androidx.core.util.Preconditions.checkArgument;
import static androidx.core.util.Preconditions.checkNotNull;

public class UserDecoder {
    private static final String TAG = UserDecoder.class.getSimpleName();

    @Nullable
    public static User convert(@NonNull Entity entity, @NonNull String aesKey) {
        try {
            checkNotNull(entity);
            checkNotNull(aesKey);
            Gson gson = new Gson();
            String data = Encryption.decrypt(entity.getContent(), aesKey);

            Content content = gson.fromJson(data, Content.class);

            String publicKey = content.get(Content.PKEY);
            checkNotNull(publicKey);

            String displayName = content.get(Content.ALIAS);
            checkNotNull(displayName);
            checkArgument(!displayName.isEmpty());

            String pid = content.get(Content.PID);
            checkNotNull(pid);


            String additions = content.get(Content.ADDS);

            User user = User.createUser(UserType.UNKNOWN,
                    displayName, publicKey, PID.create(pid), null);

            if (additions != null && !additions.isEmpty()) {
                user.setExternalAdditions(Additionals.toHashMap(additions));
            }

            user.setHash(entity.getHash());

            return user;
        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
        return null;
    }


}
