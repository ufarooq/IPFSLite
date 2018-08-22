package threads.server;

import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;

public class StatusViewModel extends AndroidViewModel {

    private final LiveData<Status> status;
    private final DaemonDatabase daemonDatabase;

    public StatusViewModel(android.app.Application application) {
        super(application);
        daemonDatabase = Application.getDaemonDatabase();

        status = daemonDatabase.statusDao().getLiveDataStatus(DaemonDatabase.STATUS_UID);
    }


    public LiveData<Status> getStatus() {
        return status;
    }
}
