package threads.server;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.TypeConverters;
import android.support.annotation.NonNull;

import static com.google.common.base.Preconditions.checkNotNull;

@Entity(tableName = "Message")
public class Message {
    @NonNull
    @ColumnInfo(name = "message")
    private final String message;
    @NonNull
    @TypeConverters(MessageKind.class)
    @ColumnInfo(name = "messageKind")
    private final MessageKind messageKind;
    @PrimaryKey(autoGenerate = true)
    private long idx;

    Message(@NonNull MessageKind messageKind, @NonNull String message) {
        checkNotNull(message);
        this.message = message;
        this.messageKind = messageKind;
    }

    static Message createMessage(@NonNull MessageKind messageKind, @NonNull String message) {
        return new Message(messageKind, message);
    }

    public MessageKind getMessageKind() {
        return messageKind;
    }

    @Override
    public String toString() {
        return "Message{" +
                "message='" + message + '\'' +
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
        return message.idx == this.idx;
    }

    public boolean sameContent(@NonNull Message message) {
        return this.getMessage().equals(message.getMessage());
    }
}