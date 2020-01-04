package threads.core.mdl;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

import threads.core.Singleton;
import threads.core.api.Note;
import threads.core.api.ThreadsDatabase;

public class NotesViewModel extends AndroidViewModel {
    @NonNull
    private final ThreadsDatabase threadsDatabase;

    public NotesViewModel(@NonNull Application application) {
        super(application);
        threadsDatabase = Singleton.getInstance(
                application.getApplicationContext()).getThreadsDatabase();
    }

    @NonNull
    public LiveData<List<Note>> getNotes() {
        return threadsDatabase.noteDao().getLiveDataNotes();
    }
}
