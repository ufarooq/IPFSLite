package threads.server;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

import static com.google.common.base.Preconditions.checkNotNull;

@Entity(tableName = "Message")
public class Message {
    @NonNull
    @ColumnInfo(name = "message")
    private final String message;
    @PrimaryKey(autoGenerate = true)
    private Long idx;

    protected Message(@NonNull String message) {
        checkNotNull(message);
        this.message = message;
    }

    public static Message createMessage(@NonNull String message) {
        return new Message(message);
    }

    public Long getIdx() {
        return idx;
    }

    public void setIdx(Long idx) {
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
        return message.message.equals(message.message);
    }
}
