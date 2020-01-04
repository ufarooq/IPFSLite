package threads.share;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

import com.bumptech.glide.Glide;

import threads.core.Preferences;
import threads.core.Singleton;
import threads.ipfs.IPFS;
import threads.server.R;

import static androidx.core.util.Preconditions.checkNotNull;

public class ImageDialogFragment extends DialogFragment implements View.OnTouchListener {
    public static final String TAG = ImageDialogFragment.class.getSimpleName();
    private static final String CID = "CID";

    private Context mContext;
    private ScaleGestureDetector mScaleGestureDetector;
    private float mScaleFactor = 1.0f;
    private ImageView mImageView;

    public static ImageDialogFragment newInstance(@NonNull String cid) {

        checkNotNull(cid);
        Bundle bundle = new Bundle();

        bundle.putString(CID, cid);

        ImageDialogFragment fragment = new ImageDialogFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;

    }

    @Override
    public void onDetach() {
        super.onDetach();
        mContext = null;
    }


    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        Activity activity = getActivity();
        checkNotNull(activity);

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);

        Bundle args = getArguments();
        checkNotNull(args);
        String cid = args.getString(CID);
        checkNotNull(cid);

        LayoutInflater inflater = activity.getLayoutInflater();


        @SuppressWarnings("all")
        View view = inflater.inflate(R.layout.image_view, null);
        view.setOnTouchListener(this);
        mImageView = view.findViewById(R.id.image_view);
        mScaleGestureDetector = new ScaleGestureDetector(mContext, new ScaleListener());
        IPFS ipfs = Singleton.getInstance(mContext).getIpfs();
        int timeout = Preferences.getConnectionTimeout(mContext);
        IPFSData data = IPFSData.create(ipfs, cid, timeout);
        Glide.with(mContext).load(data).into(mImageView);


        builder.setView(view);


        Dialog dialog = builder.create();

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);


        Window window = dialog.getWindow();
        if (window != null) {
            window.getAttributes().windowAnimations = R.style.DialogTopAnimation;
            window.getAttributes().gravity = Gravity.TOP | Gravity.CENTER;
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        handleFullRatio(mImageView);


        return dialog;
    }

    private void handleFullRatio(@NonNull ImageView imageView) {
        checkNotNull(imageView);


        DisplayMetrics metrics = new DisplayMetrics();
        FragmentActivity activity = getActivity();
        if (activity != null) {
            activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);


            Configuration config = getResources().getConfiguration();
            int screenHeight = metrics.heightPixels;
            if (config.orientation == Configuration.ORIENTATION_PORTRAIT) {
                screenHeight = (screenHeight * 6) / 10;
            }

            int screenWidth = metrics.widthPixels;

            ViewGroup.LayoutParams lp = imageView.getLayoutParams();

            lp.width = screenWidth;
            lp.height = screenHeight;

            imageView.setLayoutParams(lp);
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        mScaleGestureDetector.onTouchEvent(motionEvent);
        return true;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
            mScaleFactor *= scaleGestureDetector.getScaleFactor();
            mScaleFactor = Math.max(0.1f,
                    Math.min(mScaleFactor, 10.0f));
            mImageView.setScaleX(mScaleFactor);
            mImageView.setScaleY(mScaleFactor);
            return true;
        }
    }
}
