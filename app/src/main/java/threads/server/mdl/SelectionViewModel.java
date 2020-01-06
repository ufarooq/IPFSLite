package threads.server.mdl;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SelectionViewModel extends ViewModel {

    private MutableLiveData<Long> parentThread;

    public MutableLiveData<Long> getParentThread() {
        if (parentThread == null) {
            parentThread = new MutableLiveData<>(0L);
        }
        return parentThread;
    }

    public void setParentThread(long idx, boolean post) {

        if (post) {
            getParentThread().postValue(idx);
        } else {
            getParentThread().setValue(idx);
        }
    }


}
