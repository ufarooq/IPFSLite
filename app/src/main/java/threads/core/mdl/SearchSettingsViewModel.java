package threads.core.mdl;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import threads.core.Preferences;
import threads.core.Singleton;
import threads.core.api.Settings;
import threads.core.api.ThreadsDatabase;

public class SearchSettingsViewModel extends AndroidViewModel {

    private final LiveData<Settings> settings;

    public SearchSettingsViewModel(Application application) {
        super(application);


        ThreadsDatabase threadsDatabase = Singleton.getInstance(
                application.getApplicationContext()).getThreadsDatabase();

        settings = threadsDatabase.settingsDao().getLiveDataSettings(
                Preferences.SEARCH_SETTINGS_ID);
    }


    public LiveData<Settings> getSearchSettings() {
        return settings;
    }
}
