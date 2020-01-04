package threads.share;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.appcompat.widget.AppCompatImageView;

public class CustomZoomableImageView extends AppCompatImageView {

    private static final int INVALID_POINTER_ID = -1;
    private static final String LOG_TAG = "TouchImageView";
    float pivotPointX = 0f;
    float pivotPointY = 0f;
    private float mPosX = 0f;
    private float mPosY = 0f;
    private float mLastTouchX;
    private float mLastTouchY;
    // The ‘active pointer’ is the one currently moving our object.
    private int mActivePointerId = INVALID_POINTER_ID;
    private ScaleGestureDetector mScaleDetector;
    private float mScaleFactor = 1.f;

    public CustomZoomableImageView(Context context) {
        this(context, null, 0);
    }

    public CustomZoomableImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    // Existing code ...
    public CustomZoomableImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        // Create our ScaleGestureDetector
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        // Let the ScaleGestureDetector inspect all events.
        mScaleDetector.onTouchEvent(ev);

        final int action = ev.getAction();
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: {
                final float x = ev.getX();
                final float y = ev.getY();

                mLastTouchX = x;
                mLastTouchY = y;

                mActivePointerId = ev.getPointerId(0);
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                final int pointerIndex = ev.findPointerIndex(mActivePointerId);
                final float x = ev.getX(pointerIndex);
                final float y = ev.getY(pointerIndex);

                // Only move if the ScaleGestureDetector isn't processing a gesture.
                if (!mScaleDetector.isInProgress()) {
                    final float dx = x - mLastTouchX;
                    final float dy = y - mLastTouchY;

                    mPosX += dx;
                    mPosY += dy;

                    invalidate();
                }

                mLastTouchX = x;
                mLastTouchY = y;
                break;
            }

            case MotionEvent.ACTION_UP: {
                mActivePointerId = INVALID_POINTER_ID;
                break;
            }

            case MotionEvent.ACTION_CANCEL: {
                mActivePointerId = INVALID_POINTER_ID;
                break;
            }

            case MotionEvent.ACTION_POINTER_UP: {
                final int pointerIndex = (ev.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                final int pointerId = ev.getPointerId(pointerIndex);
                if (pointerId == mActivePointerId) {
                    // This was our active pointer going up. Choose a new
                    // active pointer and adjust accordingly.
                    final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                    mLastTouchX = ev.getX(newPointerIndex);
                    mLastTouchY = ev.getY(newPointerIndex);
                    mActivePointerId = ev.getPointerId(newPointerIndex);
                }
                break;
            }
        }

        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see android.view.View#draw(android.graphics.Canvas)
     */
    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (this.getDrawable() != null) {
            canvas.save();
            canvas.translate(mPosX, mPosY);

            Matrix matrix = new Matrix();
            matrix.postScale(mScaleFactor, mScaleFactor, pivotPointX,
                    pivotPointY);
            // canvas.setMatrix(matrix);

            canvas.drawBitmap(
                    ((BitmapDrawable) this.getDrawable()).getBitmap(), matrix,
                    null);

            // this.getDrawable().draw(canvas);
            canvas.restore();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * android.widget.ImageView#setImageDrawable(android.graphics.drawable.Drawable
     * )
     */
    @Override
    public void setImageDrawable(Drawable drawable) {
        // Constrain to given size but keep aspect ratio
        if (drawable != null) {
            int width = drawable.getIntrinsicWidth();
            int height = drawable.getIntrinsicHeight();
            mLastTouchX = mPosX = 0;
            mLastTouchY = mPosY = 0;

            mScaleFactor = Math.min(((float) getLayoutParams().width)
                    / width, ((float) getLayoutParams().height)
                    / height);
            pivotPointX = (((float) getLayoutParams().width) - (int) (width * mScaleFactor)) / 2;
            pivotPointY = (((float) getLayoutParams().height) - (int) (height * mScaleFactor)) / 2;
        }
        super.setImageDrawable(drawable);
    }

    private class ScaleListener extends
            ScaleGestureDetector.SimpleOnScaleGestureListener {

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            mScaleFactor *= detector.getScaleFactor();

            pivotPointX = detector.getFocusX();
            pivotPointY = detector.getFocusY();

            Log.d(LOG_TAG, "mScaleFactor " + mScaleFactor);
            Log.d(LOG_TAG, "pivotPointY " + pivotPointY + ", pivotPointX= "
                    + pivotPointX);
            mScaleFactor = Math.max(0.1f, Math.min(mScaleFactor, 10.0f));

            invalidate();
            return true;
        }
    }
}