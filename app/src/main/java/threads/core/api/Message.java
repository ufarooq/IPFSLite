package threads.core.api;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import static androidx.core.util.Preconditions.checkNotNull;

@Entity
public class Message {
    @NonNull
    @ColumnInfo(name = "message")
    private final String message;

    @ColumnInfo(name = "timestamp")
    private final long timestamp;

    @NonNull
    @TypeConverters(MessageKind.class)
    @ColumnInfo(name = "messageKind")
    private final MessageKind messageKind;
    @PrimaryKey(autoGenerate = true)
    private long idx;

    Message(@NonNull MessageKind messageKind, @NonNull String message, long timestamp) {
        checkNotNull(message);
        this.message = message;
        this.messageKind = messageKind;
        this.timestamp = timestamp;
    }

    static Message createMessage(@NonNull MessageKind messageKind,
                                 @NonNull String message,
                                 long timestamp) {
        checkNotNull(messageKind);
        checkNotNull(message);
        return new Message(messageKind, message, timestamp);
    }

    public long getTimestamp() {
        return timestamp;
    }

    @NonNull
    public MessageKind getMessageKind() {
        return messageKind;
    }

    @Override
    @NonNull
    public String toString() {
        return "Message{" +
                "message='" + message + '\'' +
                ", timestamp=" + timestamp +
                ", messageKind=" + messageKind +
                ", idx=" + idx +
                '}';
    }

    public long getIdx() {
        return idx;
    }

    public void setIdx(long idx) {
        this.idx = idx;
    }

    @NonNull
    public String getMessage() {
        return message;
    }

    public boolean areItemsTheSame(@NonNull Message message) {
        checkNotNull(message);
        return message.idx == this.idx;
    }

    public boolean sameContent(@NonNull Message message) {
        checkNotNull(message);
        return areItemsTheSame(message);
    }
}
