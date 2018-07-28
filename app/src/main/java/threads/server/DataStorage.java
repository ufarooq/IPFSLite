package threads.server;


import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

import java.util.Arrays;

import threads.iri.IDataStorage;


@Entity(tableName = "DataStorage")
public class DataStorage implements IDataStorage {


    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    private final byte[] data;

    @NonNull
    @ColumnInfo(name = "address")
    private final String address;
    @NonNull
    @ColumnInfo(name = "chunkIndex")
    private final int chunkIndex;
    @PrimaryKey(autoGenerate = true)
    private Long idx;

    protected DataStorage(@NonNull String address, int chunkIndex, byte[] data) {
        this.address = address;
        this.chunkIndex = chunkIndex;
        this.data = data;
    }

    public static DataStorage createDataStorage(@NonNull String fileUid, int chunkIndex, byte[] data) {
        return new DataStorage(fileUid, chunkIndex, data);
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
                ", chunkIndex=" + chunkIndex +
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
    public int getChunkIndex() {
        return chunkIndex;
    }

    @Override
    public int compareTo(@NonNull IDataStorage storage) {
        return Integer.valueOf(this.getChunkIndex()).compareTo(storage.getChunkIndex());
    }
}
