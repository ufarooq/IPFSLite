package threads.server.utils;

import android.webkit.URLUtil;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;

import java.net.URI;

import threads.ipfs.Multihash;
import threads.server.core.peers.Content;

import static androidx.core.util.Preconditions.checkNotNull;


public class CodecDecider {

    private static final Gson gson = new Gson();
    private String multihash = null;
    private Content map = null;
    private Codec codex = Codec.UNKNOWN;

    private CodecDecider() {
    }

    public static CodecDecider evaluate(@NonNull String code) {
        checkNotNull(code);
        CodecDecider codecDecider = new CodecDecider();


        // check if multihash is valid
        try {
            Multihash.fromBase58(code);
            codecDecider.setMultihash(code);
            codecDecider.setCodex(Codec.MULTIHASH);
            return codecDecider;
        } catch (Throwable e) {
            // exceptions ignored
        }


        try {
            if (URLUtil.isValidUrl(code)) {
                // ok now it is a URI, but is the content is an ipfs multihash
                try {
                    URI uri = new URI(code);
                    String path = uri.getPath();
                    if (path.startsWith("/ipfs/")) {
                        String multihash = path.replace("/ipfs/", "");
                        Multihash.fromBase58(multihash);
                        codecDecider.setMultihash(multihash);
                        codecDecider.setCodex(Codec.URI);
                        return codecDecider;
                    }
                } catch (Throwable e) {
                    // ignore exception
                }
            }
        } catch (Throwable e) {
            // ignore exception
        }

        try {
            Content map = gson.fromJson(code, Content.class);
            if (map != null) {
                codecDecider.setCodex(Codec.CONTENT);
                codecDecider.setContent(map);
                return codecDecider;
            }
        } catch (Throwable e) {
            // ignore exception
        }

        codecDecider.setCodex(Codec.UNKNOWN);
        return codecDecider;
    }

    public String getMultihash() {
        return multihash;
    }

    private void setMultihash(String multihash) {
        this.multihash = multihash;
    }

    public Codec getCodex() {
        return codex;
    }

    private void setCodex(Codec codex) {
        this.codex = codex;
    }


    @Nullable
    public Content getContent() {
        return map;
    }

    private void setContent(@NonNull Content map) {
        checkNotNull(map);
        this.map = map;
    }

    public enum Codec {
        UNKNOWN, MULTIHASH, URI, CONTENT
    }
}
