package threads.server;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;

import threads.server.daemon.IThreadsServer;
import threads.server.event.Event;
import threads.server.event.EventsDatabase;
import threads.server.event.IEvent;

public class EventViewModel extends AndroidViewModel {

    private final EventsDatabase eventsDatabase;

    public EventViewModel(@NonNull Application application) {
        super(application);
        eventsDatabase = threads.server.Application.getEventsDatabase();
    }


    public LiveData<Event> getDaemonServerOnlineEvent() {
        return eventsDatabase.eventDao().getEvent(IThreadsServer.DAEMON_SERVER_ONLINE_EVENT);
    }

    public LiveData<Event> getDaemonServerOfflineEvent() {
        return eventsDatabase.eventDao().getEvent(IThreadsServer.DAEMON_SERVER_OFFLINE_EVENT);
    }

    public LiveData<Event> getHostNameChangeEvent() {
        return eventsDatabase.eventDao().getEvent(IThreadsServer.DAEMON_SERVER_RENAME_HOST_EVENT);
    }

    public LiveData<Event> getPublicIPChangeEvent() {
        return eventsDatabase.eventDao().getEvent(IThreadsServer.PUBLIC_IP_CHANGE_EVENT);
    }

    public void removeEvent(@NonNull final Event event) {
        new Thread(new Runnable() {
            public void run() {
                eventsDatabase.eventDao().deleteEvent(event);
            }
        }).start();
    }

    public void issueEvent(@NonNull String identifier) {
        new Thread(new Runnable() {
            public void run() {
                IEvent event = eventsDatabase.createEvent(identifier);
                eventsDatabase.insertEvent(event);
            }
        }).start();
    }
}