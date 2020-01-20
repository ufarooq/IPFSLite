package threads.server.mdl;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SelectionViewModel extends ViewModel {

    @NonNull
    private MutableLiveData<Long> parentThread = new MutableLiveData<>(0L);

    @NonNull
    public MutableLiveData<Long> getParentThread() {
        return parentThread;
    }

    public void setParentThread(long idx) {
        getParentThread().postValue(idx);
    }

    public boolean isTopLevel() {
        Long value = getParentThread().getValue();
        if (value != null) {
            return value == 0L;
        }
        return true;
    }

}
