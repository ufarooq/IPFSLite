package threads.server;

import android.arch.persistence.room.RoomDatabase;
import android.support.annotation.NonNull;

import static com.google.common.base.Preconditions.checkNotNull;

@android.arch.persistence.room.Database(entities = {Message.class}, version = 1, exportSchema = false)
public abstract class MessageDatabase extends RoomDatabase {
    private static final String TAG = MessageDatabase.class.getSimpleName();

    public abstract MessageDao messageDao();

    public void insertMessage(@NonNull String message) {
        checkNotNull(message);
        messageDao().insertMessages(Message.createMessage(message));
    }
}
