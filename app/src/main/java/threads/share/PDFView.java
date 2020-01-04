package threads.share;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.fragment.app.Fragment;

public class PDFView {

    public static Builder with(Activity activity) {
        return new ActivityBuilder(activity);
    }


    public static abstract class BaseBuilder {

        protected PDFConfig config;

        public BaseBuilder(Context context) {
            this.config = new PDFConfig();
        }
    }

    public static abstract class Builder extends BaseBuilder {

        public Builder(Activity activity) {
            super(activity);
        }

        public Builder(Fragment fragment) {
            super(fragment.getActivity());
        }

        public Builder cid(String cid) {
            config.setCid(cid);
            return this;
        }


        public Builder swipeHorizontal(boolean swipeOrientation) {
            config.setSwipeorientation(swipeOrientation ? 0 : 1);
            return this;
        }

        public abstract void start();

    }

    static class ActivityBuilder extends Builder {
        private Activity activity;

        public ActivityBuilder(Activity activity) {
            super(activity);
            this.activity = activity;
        }

        @Override
        public void start() {
            Intent intent = new Intent(activity, PDFViewActivity.class);
            intent.putExtra(PDFConfig.EXTRA_CONFIG, config);
            activity.startActivity(intent);
        }
    }

}
