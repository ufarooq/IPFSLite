package threads.server.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

import threads.ipfs.CID;
import threads.ipfs.IPFS;

import static androidx.core.util.Preconditions.checkNotNull;

public class IPFSData {
    @NonNull
    private final String multihash;
    @Nullable
    private final IPFS ipfs;

    private IPFSData(@Nullable IPFS ipfs, @NonNull String multihash) {
        checkNotNull(ipfs);
        checkNotNull(multihash);
        this.ipfs = ipfs;
        this.multihash = multihash;
    }

    @NonNull
    public static IPFSData create(@Nullable IPFS ipfs, @NonNull String cid) {
        return new IPFSData(ipfs, cid);
    }

    @NonNull
    public static IPFSData create(@Nullable IPFS ipfs, @NonNull CID cid) {
        return create(ipfs, cid.getCid());
    }

    @Nullable
    public IPFS getIpfs() {
        return ipfs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IPFSData ipfsData = (IPFSData) o;
        return Objects.equals(multihash, ipfsData.multihash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(multihash);
    }

    @NonNull
    public CID getCid() {
        return CID.create(multihash);
    }

}
