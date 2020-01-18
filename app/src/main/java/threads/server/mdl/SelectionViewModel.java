package threads.server.mdl;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SelectionViewModel extends ViewModel {

    private MutableLiveData<Long> parentThread;

    @NonNull
    public MutableLiveData<Long> getParentThread() {
        if (parentThread == null) {
            parentThread = new MutableLiveData<>(0L);
        }
        return parentThread;
    }

    public void setParentThread(long idx) {
        getParentThread().postValue(idx);
    }


}
