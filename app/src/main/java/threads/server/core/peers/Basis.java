package threads.server.core.peers;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.TypeConverters;

import java.util.HashMap;
import java.util.Map;

import static androidx.core.util.Preconditions.checkNotNull;


public class Basis {
    @NonNull
    @TypeConverters(Additions.class)
    @ColumnInfo(name = "additions")
    private Additions additions = new Additions();

    @Nullable
    @ColumnInfo(name = "hash")
    private String hash;

    @ColumnInfo(name = "timestamp")
    private long timestamp;

    Basis() {
        this.timestamp = System.currentTimeMillis();
    }

    public long getTimestamp() {
        return timestamp;
    }

    void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Nullable
    public String getHash() {
        return hash;
    }

    public void setHash(@Nullable String hash) {
        this.hash = hash;
    }


    @NonNull
    public Additions getAdditions() {
        return additions;
    }

    public void setAdditions(@NonNull Additions additions) {
        checkNotNull(additions);
        this.additions = additions;
    }

    public void removeAdditions() {
        this.additions.clear();
    }

    public void addAdditional(@NonNull String key, @NonNull String value, @NonNull Boolean internal) {
        checkNotNull(key);
        checkNotNull(value);
        checkNotNull(internal);
        this.additions.put(key, Additional.createAdditional(value, internal));
    }


    @Nullable
    Additional getAdditional(@NonNull String key) {
        checkNotNull(key);

        return this.additions.get(key);

    }

    @NonNull
    public String getAdditionalValue(@NonNull String key) {
        checkNotNull(key);

        Additional additional = this.additions.get(key);
        if (additional != null) {
            return additional.getValue();
        }
        return "";
    }

    boolean hasAdditional(@NonNull String key) {
        checkNotNull(key);

        return this.additions.get(key) != null;

    }

    public void removeAdditional(@NonNull String key) {
        checkNotNull(key);
        this.additions.remove(key);
    }

    @NonNull
    HashMap<String, String> getExternalAdditions() {

        HashMap<String, String> hashMap = new HashMap<>();

        for (Map.Entry<String, Additional> entry : additions.entrySet()) {
            Additional additional = entry.getValue();
            if (!additional.getInternal()) {
                hashMap.put(entry.getKey(), additional.getValue());
            }
        }

        return hashMap;
    }

    void setExternalAdditions(@NonNull Map<String, String> hashMap) {
        checkNotNull(hashMap);
        for (Map.Entry<String, String> entry : hashMap.entrySet()) {
            this.additions.put(entry.getKey(),
                    Additional.createAdditional(entry.getValue(), false));
        }
    }


}
