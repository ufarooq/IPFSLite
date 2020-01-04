package threads.core.api;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;

import threads.iota.Entity;
import threads.ipfs.api.CID;
import threads.ipfs.api.Encryption;
import threads.ipfs.api.PID;

import static androidx.core.util.Preconditions.checkNotNull;

public class ThreadDecoder {

    private static final String TAG = ThreadDecoder.class.getSimpleName();

    @Nullable
    public static Thread convert(@NonNull Entity entity, @NonNull String aesKey) {
        checkNotNull(entity);
        checkNotNull(aesKey);
        try {

            String data = entity.getContent();
            Gson gson = new Gson();
            Content content = gson.fromJson(data, Content.class);

            String senderPid = content.get(Content.PID);
            checkNotNull(senderPid);

            // ENCRYPTED
            String additions = Encryption.decrypt(content.get(Content.ADDS), aesKey);
            checkNotNull(additions);

            String sesKey = content.get(Content.SKEY);
            checkNotNull(sesKey);

            // ENCRYPTED
            String senderAlias = Encryption.decrypt(content.get(Content.ALIAS), aesKey);
            checkNotNull(senderAlias);

            String senderKey = content.get(Content.PKEY);
            checkNotNull(senderKey);

            String cidValue = content.get(Content.CID);
            CID cid = null;
            if (cidValue != null) {
                cid = CID.create(cidValue);
            }

            String date = content.get(Content.DATE);
            checkNotNull(date);
            long timestamp = Long.valueOf(date);

            Thread thread = Thread.createThread(
                    Status.INIT,
                    PID.create(senderPid),
                    senderAlias,
                    senderKey,
                    sesKey,
                    Kind.IN,
                    Long.valueOf(date),
                    0);

            thread.setCid(cid);

            if (!additions.isEmpty()) {
                thread.setExternalAdditions(Additionals.toHashMap(additions));
            }


            String imgValue = content.get(Content.IMG);
            if (imgValue != null) {
                thread.setImage(CID.create(imgValue));
            }

            String expireDate = content.get(Content.EXPIRE_DATE);
            checkNotNull(expireDate);
            thread.setExpireDate(Long.valueOf(expireDate));
            thread.setMarked(false);

            thread.setHash(entity.getHash());
            thread.setTimestamp(timestamp);
            return thread;

        } catch (Throwable e) {
            Log.e(TAG, "" + e.getLocalizedMessage(), e);
        }
        return null;
    }

}
