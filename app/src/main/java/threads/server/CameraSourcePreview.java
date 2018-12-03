package threads.server;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;

public class CameraSourcePreview extends ViewGroup {

    private SurfaceView mSurfaceView;
    private boolean mStartRequested;
    private boolean mSurfaceAvailable;
    private CameraSource mCameraSource;

    private OnCameraErrorListener mCameraErrorListener;

    public CameraSourcePreview(Context context, AttributeSet attrs) {
        super(context, attrs);
        mStartRequested = false;
        mSurfaceAvailable = false;

        mSurfaceView = new SurfaceView(context);
        mSurfaceView.getHolder().addCallback(new SurfaceCallback());
        addView(mSurfaceView);
    }

    public void start(CameraSource cameraSource) throws SecurityException {
        if (cameraSource == null) {
            stop();
        }

        mCameraSource = cameraSource;
        if (mCameraSource != null) {
            mStartRequested = true;
            startIfReady();
        }
    }

    public void stop() {
        if (mCameraSource != null) {
            mCameraSource.stop();
        }
    }

    public void release() {
        if (mCameraSource != null) {
            mCameraSource.release();
            mCameraSource = null;
        }
    }

    private void startIfReady() {
        try {
            if (mStartRequested && mSurfaceAvailable) {
                mCameraSource.start(mSurfaceView.getHolder());
                mStartRequested = false;
            }
        } catch (Exception e) {
            handleCameraException(e);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        for (int i = 0; i < getChildCount(); ++i) {
            getChildAt(0).layout(0, 0, right - left, bottom - top);
        }
        startIfReady();
    }

    private void handleCameraException(@NonNull final Exception e) {
        if (mCameraErrorListener != null) {
            mCameraErrorListener.onCameraError();
        }
    }

    public void setOnCameraErrorListener(@Nullable final OnCameraErrorListener listener) {
        mCameraErrorListener = listener;
    }

    public interface OnCameraErrorListener {
        void onCameraError();
    }

    private class SurfaceCallback implements SurfaceHolder.Callback {
        @Override
        public void surfaceCreated(SurfaceHolder surface) {
            mSurfaceAvailable = true;
            startIfReady();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surface) {
            mSurfaceAvailable = false;
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        }
    }
}