package threads.ipfs.api;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

import org.apache.commons.codec.binary.Base64;

import java.util.Map;
import java.util.Objects;

import static androidx.core.util.Preconditions.checkNotNull;


public class PubsubInfo {

    private static Gson gson = new Gson();
    @NonNull
    private final String from;
    @NonNull
    private final String data;
    @NonNull
    private final String seqno;
    @NonNull
    private final String topic;

    private PubsubInfo(@NonNull String topic, @NonNull String from, @NonNull String data, @NonNull String seqno) {
        checkNotNull(topic);
        checkNotNull(from);
        checkNotNull(data);
        checkNotNull(seqno);
        this.topic = topic;
        this.from = from;
        this.data = data;
        this.seqno = seqno;
    }

    public static PubsubInfo create(@NonNull String message, byte[] data) {
        checkNotNull(message);
        Map map = gson.fromJson(message, Map.class);
        String topic = (String) map.get("Topic");
        checkNotNull(topic);
        String from = (String) map.get("From");
        checkNotNull(from);
        String seqno = (String) map.get("Seqno");
        checkNotNull(seqno);

        String encData = new String(Base64.decodeBase64(data));

        return new PubsubInfo(topic, from, encData, seqno);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PubsubInfo that = (PubsubInfo) o;
        return Objects.equals(from, that.from) &&
                Objects.equals(data, that.data) &&
                Objects.equals(seqno, that.seqno);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, seqno);
    }

    @Override
    @NonNull
    public String toString() {
        return "{" +
                "Topic='" + getTopic() + "\' " +
                ", From='" + getSenderPid() + "\' " +
                ", Data='" + getMessage() + "\' " +
                ", Seqno='" + getSeqno() + "\' " +
                '}';
    }


    @NonNull
    public String getSenderPid() {
        return from;
    }

    @NonNull
    public String getMessage() {
        return data;
    }

    @NonNull
    public String getSeqno() {
        return seqno;
    }

    @NonNull
    public String getTopic() {
        return topic;
    }
}
