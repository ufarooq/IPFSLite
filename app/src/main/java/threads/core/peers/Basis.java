package threads.core.peers;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.TypeConverters;

import java.util.HashMap;
import java.util.Map;

import static androidx.core.util.Preconditions.checkNotNull;


public class Basis {
    @NonNull
    @TypeConverters(Additionals.class)
    @ColumnInfo(name = "additionals")
    private Additionals additionals = new Additionals();

    @Nullable
    @ColumnInfo(name = "hash")
    private String hash;

    @ColumnInfo(name = "timestamp")
    private long timestamp;

    public Basis() {
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


    public boolean hasHash() {
        return hash != null;
    }

    @NonNull
    public Additionals getAdditionals() {
        return additionals;
    }

    public void setAdditionals(@NonNull Additionals additionals) {
        checkNotNull(additionals);
        this.additionals = additionals;
    }

    public void removeAdditionals() {
        this.additionals.clear();
    }

    public void addAdditional(@NonNull String key, @NonNull String value, @NonNull Boolean internal) {
        checkNotNull(key);
        checkNotNull(value);
        checkNotNull(internal);
        this.additionals.put(key, Additional.createAdditional(value, internal));
    }


    @Nullable
    public Additional getAdditional(@NonNull String key) {
        checkNotNull(key);

        return this.additionals.get(key);

    }

    @NonNull
    public String getAdditionalValue(@NonNull String key) {
        checkNotNull(key);

        Additional additional = this.additionals.get(key);
        if (additional != null) {
            return additional.getValue();
        }
        return "";
    }

    public boolean hasAdditional(@NonNull String key) {
        checkNotNull(key);

        return this.additionals.get(key) != null;

    }

    public void removeAdditional(@NonNull String key) {
        checkNotNull(key);
        this.additionals.remove(key);
    }

    @NonNull
    HashMap<String, String> getExternalAdditions() {

        HashMap<String, String> hashMap = new HashMap<>();

        for (Map.Entry<String, Additional> entry : additionals.entrySet()) {
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
            this.additionals.put(entry.getKey(),
                    Additional.createAdditional(entry.getValue(), false));
        }
    }


}
