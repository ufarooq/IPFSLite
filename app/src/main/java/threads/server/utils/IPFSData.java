package threads.server.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

import threads.ipfs.CID;
import threads.ipfs.IPFS;

import static androidx.core.util.Preconditions.checkArgument;
import static androidx.core.util.Preconditions.checkNotNull;

public class IPFSData {
    @NonNull
    private final String multihash;
    @Nullable
    private final IPFS ipfs;
    private final int timeout;

    private IPFSData(@Nullable IPFS ipfs, @NonNull String multihash, int timeout) {
        checkNotNull(ipfs);
        checkNotNull(multihash);

        checkArgument(timeout > 0);
        this.ipfs = ipfs;
        this.multihash = multihash;

        this.timeout = timeout;
    }

    @NonNull
    public static IPFSData create(@Nullable IPFS ipfs, @NonNull String cid, int timeout) {
        return new IPFSData(ipfs, cid, timeout);
    }

    @NonNull
    public static IPFSData create(@Nullable IPFS ipfs, @NonNull CID cid, int timeout) {
        return create(ipfs, cid.getCid(), timeout);
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


    public int getTimeout() {
        return timeout;
    }
}
