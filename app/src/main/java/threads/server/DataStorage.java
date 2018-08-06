package threads.server;


import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

import java.util.Arrays;

import threads.iri.IDataStorage;

import static com.google.api.client.repackaged.com.google.common.base.Preconditions.checkArgument;
import static com.google.api.client.repackaged.com.google.common.base.Preconditions.checkNotNull;


@Entity(tableName = "DataStorage")
public class DataStorage implements IDataStorage {


    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    private final byte[] data;

    @NonNull
    @ColumnInfo(name = "address")
    private final String address;

    @NonNull
    @ColumnInfo(name = "cidx")
    private final long cidx;

    protected DataStorage(@NonNull String address, long cidx, byte[] data) {
        this.address = address;
        this.cidx = cidx;
        this.data = data;
    }
    @PrimaryKey(autoGenerate = true)
    private Long idx;

    public static DataStorage createDataStorage(@NonNull String address, long cidx, byte[] data) {
        checkNotNull(address);
        checkNotNull(data);
        checkArgument(cidx >= 0);
        return new DataStorage(address, cidx, data);
    }

    @NonNull
    public long getCidx() {
        return cidx;
    }


    @NonNull
    public Long getIdx() {
        return idx;
    }

    public void setIdx(Long idx) {
        this.idx = idx;
    }

    @Override
    public String toString() {
        return "DataStorage{" +
                ", index=" + cidx +
                ", address='" + address + '\'' +
                ", data=" + Arrays.toString(data) +
                '}';
    }

    public byte[] getData() {
        return data;
    }

    @NonNull
    public String getAddress() {
        return address;
    }

    @NonNull
    public Long getIndex() {
        return getCidx();
    }

    @Override
    public int compareTo(@NonNull IDataStorage storage) {
        return Long.valueOf(this.getIndex()).compareTo(storage.getIndex());
    }
}
