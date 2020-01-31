package threads.server.model;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SelectionViewModel extends ViewModel {

    @NonNull
    private final MutableLiveData<Long> parentThread = new MutableLiveData<>(0L);
    @NonNull
    private final MutableLiveData<String> query = new MutableLiveData<>("");

    @NonNull
    public MutableLiveData<Long> getParentThread() {
        return parentThread;
    }

    public void setParentThread(long idx) {
        getParentThread().postValue(idx);
    }

    @NonNull
    public MutableLiveData<String> getQuery() {
        return query;
    }

    public void setQuery(String query) {
        getQuery().postValue(query);
    }

    public boolean isTopLevel() {
        Long value = getParentThread().getValue();
        if (value != null) {
            return value == 0L;
        }
        return true;
    }

}
