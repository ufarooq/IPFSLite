package threads.server;

import android.webkit.URLUtil;

import com.google.common.base.Preconditions;

import java.net.URI;

import androidx.annotation.NonNull;

public class CodecDecider {


    private String multihash = null;
    private URI uri = null;
    private Codec codex = Codec.UNKNOWN;

    private CodecDecider() {
    }

    static CodecDecider evaluate(@NonNull String code) {
        Preconditions.checkNotNull(code);
        CodecDecider codecDecider = new CodecDecider();


        // check if multihash is valid
        try {
            codecDecider.setMultihash(code);
            codecDecider.setCodex(Codec.MULTIHASH);
            return codecDecider;
        } catch (Throwable e) {
            // exceptions ignored
        }


        if (URLUtil.isValidUrl(code)) {
            // ok now it is a URI, but is the content is an ipfs multihash
            try {
                URI uri = new URI(code);
                String path = uri.getPath();
                if (path.startsWith("/ipfs/")) {
                    String multihash = path.replace("/ipfs/", "");
                    codecDecider.setMultihash(multihash);
                    codecDecider.setCodex(Codec.URI);
                    codecDecider.setUri(uri);
                    return codecDecider;
                }
            } catch (Throwable e) {
                // ignore exception
            }

        }

        codecDecider.setCodex(Codec.UNKNOWN);
        return codecDecider;
    }

    String getMultihash() {
        return multihash;
    }

    private void setMultihash(String multihash) {
        this.multihash = multihash;
    }

    Codec getCodex() {
        return codex;
    }

    private void setCodex(Codec codex) {
        this.codex = codex;
    }

    public URI getUri() {
        return uri;
    }

    private void setUri(URI uri) {
        this.uri = uri;
    }

    public enum Codec {
        UNKNOWN, MULTIHASH, URI
    }
}
