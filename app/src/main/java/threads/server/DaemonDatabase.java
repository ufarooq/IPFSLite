package threads.server;

import android.arch.persistence.room.RoomDatabase;
import android.support.annotation.NonNull;

import static com.google.common.base.Preconditions.checkNotNull;

@android.arch.persistence.room.Database(entities = {Message.class, Status.class}, version = 1, exportSchema = false)
public abstract class DaemonDatabase extends RoomDatabase {
    public static final String STATUS_UID = threads.server.Status.class.getSimpleName();
    private static final String TAG = DaemonDatabase.class.getSimpleName();

    public abstract MessageDao messageDao();

    public abstract StatusDao statusDao();


    public void insertMessage(@NonNull String message) {
        checkNotNull(message);
        messageDao().insertMessages(Message.createMessage(message));
    }

    public void clear() {
        messageDao().clear();
    }


    public threads.server.Status getStatus() {
        Status status = statusDao().getStatus(STATUS_UID);
        if (status == null) {
            status = threads.server.Status.createStatus(STATUS_UID);
            statusDao().insertStatus(status);
        }
        return status;
    }

    public void updateStatus(@NonNull threads.server.Status status) {
        statusDao().updateStatus(status);
    }


}
