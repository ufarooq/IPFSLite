package threads.server;

import android.webkit.URLUtil;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.net.URI;
import java.util.Map;

import threads.core.api.Content;
import threads.ipfs.api.Multihash;

import static androidx.core.util.Preconditions.checkNotNull;


public class CodecDecider {

    private static final Gson gson = new Gson();
    private String multihash = null;
    private URI uri = null;
    private Content map = null;
    private Codec codex = Codec.UNKNOWN;

    private CodecDecider() {
    }

    static CodecDecider evaluate(@NonNull String code) {
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
                        codecDecider.setUri(uri);
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

            Type type = new TypeToken<Map<String, String>>() {
            }.getType();
            Content map = gson.fromJson(code, type);
            if (map != null) {
                codecDecider.setCodex(Codec.JSON_MAP);
                codecDecider.setContent(map);
                return codecDecider;
            }
        } catch (Throwable e) {
            // ignore exception
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


    @Nullable
    public Content getContent() {
        return map;
    }

    private void setContent(@NonNull Content map) {
        checkNotNull(map);
        this.map = map;
    }

    public enum Codec {
        UNKNOWN, MULTIHASH, URI, JSON_MAP
    }
}
