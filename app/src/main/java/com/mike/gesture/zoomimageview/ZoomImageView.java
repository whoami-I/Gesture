package com.mike.gesture.zoomimageview;


import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;
import android.widget.OverScroller;

import androidx.appcompat.widget.AppCompatImageView;

/**
 * when finger move left, GestureDetector.OnGestureListener.onScroll distanceX is positive,
 * when finger move top, GestureDetector.OnGestureListener.onScroll distanceY is positive,
 * but postTranslate(x,y),x>0 y>0 ,then view will move right and down
 */
public class ZoomImageView extends AppCompatImageView {

    private static final String TAG = "ZoomImageView";

    private static final int MODE_UNINIT = -1;
    private static final int MODE_FLING = 0;
    private static final int MODE_SCROLL = 1;
    private static final int MODE_RESTORE = 2;
    private static final int MODE_SCALE = 3;
    private static final int MODE_IDLE = 4;
    private int mState = MODE_UNINIT;

    private Matrix mMatrix;

    private int mPrevDistance;
    /* rect to calculate the size and position of current drawable */
    private RectF mRectF = new RectF();
    /* Gesture detector,detect scroll and fling */
    private GestureDetector mGestureDector;
    private ScaleGestureDetector mScaleGestureDetector;

    // to help fling
    private OverScroller mScroller;

    public ZoomImageView(Context context) {
        this(context, null);
    }

    public ZoomImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ZoomImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    /* to store the value of last scroller value */
    int[] xyLast = new int[2];

    @Override
    public void computeScroll() {
        //super.computeScroll();
        if (mScroller.computeScrollOffset()) {
            //scrollTo(0, mScroller.getCurrY());
            int x = mScroller.getCurrX() - xyLast[0];
            int y = mScroller.getCurrY() - xyLast[1];
            Log.d(TAG, "CurrX->" + mScroller.getCurrX() + "; CurrY -> " + mScroller.getCurrY() + "; xLast ->" + xyLast[0] + "; yLast ->" + xyLast[1]);
            postTranslate(x, y);
            postInvalidate();
            xyLast[0] = mScroller.getCurrX();
            xyLast[1] = mScroller.getCurrY();
        } else {
            //changeState(MODE_IDLE);
        }
    }

    private GestureDetector.OnGestureListener mGestureListener = new GestureDetector.SimpleOnGestureListener() {

        @Override
        public boolean onDown(MotionEvent e) {
            /* end the fling */
            mScroller.forceFinished(true);
            return true;
        }

        /* finger move right velocityX > 0, move down velocityY > 0*/
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            Log.d(TAG, "velocityX -->" + velocityX + "; velocityY -->" + velocityY);
            xyLast[0] = 0;
            xyLast[1] = 0;
            /* calculate the parameters of fling operation*/
            int[] xy = new int[2];
            int minX, maxX, minY, maxY;
            int x, y;
            //1 calculate xy[] according to direction,set x and y to biggest fling value
            x = velocityX > 0 ? Integer.MIN_VALUE : Integer.MAX_VALUE;
            y = velocityY > 0 ? Integer.MIN_VALUE : Integer.MAX_VALUE;
            fixXY(x, y, xy);
            if (velocityX > 0) {
                minX = 0;
                maxX = -xy[0];
            } else {
                minX = -xy[0];
                maxX = 0;
            }
            if (velocityY > 0) {
                minY = 0;
                maxY = -xy[1];
            } else {
                minY = -xy[1];
                maxY = 0;
            }
            //2 xy[] has the value that can fling most and min Fling value
            if (xy[0] != 0 || xy[1] != 0) {
                changeState(MODE_FLING);
                mScroller.fling(0, 0, (int) velocityX, (int) velocityY, minX, maxX, minY, maxY);
            }
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (mState == MODE_SCROLL) {
                Log.d(TAG, "distanceX -> " + distanceX + "; distanceY -> " + distanceY);
                int[] xy = new int[2];
                fixXY((int) distanceX, (int) distanceY, xy);
                postTranslate(-xy[0], -xy[1]);
            }
            return true;
        }
    };

    /**
     * 修正当前最终的x,y不能超出边界
     *
     * @param x     滑动x
     * @param y     滑动y
     * @param outXY 经过计算，c存储可以滑动的x和y
     */
    private void fixXY(int x, int y, int[] outXY) {
        int xx;
        int yy;
        float currentScale = getCurrentScale(getImageMatrix());

        //获取当前图片经过放大 位移之后的矩形位置
        mRectF.set(0, 0, getDrawable().getIntrinsicWidth(), getDrawable().getIntrinsicHeight());
        getImageMatrix().mapRect(mRectF);
        Log.d(TAG, "left -> " + mRectF.left + "; top -> " + mRectF.top + "; right -> " + mRectF.right + "; bottom -> " + mRectF.bottom);
        if (x < 0) {
            // finger move right xx < 0
            if (mRectF.left >= 0) {
                xx = 0;
            } else {
                int tmpX = (int) (mRectF.left - x);
                if (tmpX >= 0) {
                    xx = (int) mRectF.left;
                } else {
                    xx = x;
                }
            }
        } else {
            // finger move left xx > 0 , x > 0
            if (mRectF.right <= getWidth()) {
                xx = 0;
            } else {
                int tmpX = (int) (mRectF.right - getWidth() - x);
                if (tmpX <= 0) {
                    xx = (int) (mRectF.right - getWidth());
                } else {
                    xx = x;
                }
            }
        }
        if (y < 0) {
            //finger move up yy<0 y<0
            if (mRectF.top >= 0) {
                yy = 0;
            } else {
                int tmpY = (int) (mRectF.top - y);
                if (tmpY >= 0) {
                    yy = (int) mRectF.top;
                } else {
                    yy = y;
                }
            }
        } else {
            //finger move down yy>0 y>0
            if (mRectF.bottom <= getHeight()) {
                yy = 0;
            } else {
                int tmpY = (int) (mRectF.bottom - getHeight() - y);
                if (tmpY <= 0) {
                    yy = (int) (mRectF.bottom - getHeight());
                } else {
                    yy = y;
                }
            }
        }
        Log.d("TAG", "xx->" + xx + "; x -> " + x);
        Log.d("TAG", "yy->" + yy + "; y -> " + y);
        outXY[0] = xx;
        outXY[1] = yy;
    }

    private void changeState(int state) {
        mState = state;
    }


    public float getCurrentScale(Matrix matrix) {
        float[] values = new float[9];
        matrix.getValues(values);
        return values[Matrix.MSCALE_X];
    }

    private ScaleGestureDetector.OnScaleGestureListener mScaleGestureListener = new ScaleGestureDetector.SimpleOnScaleGestureListener() {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            //mMatrix.reset();
            mMatrix.set(getImageMatrix());
            //mMatrix = getMatrix();
            mMatrix.postScale(detector.getScaleFactor(), detector.getScaleFactor(),
                    detector.getFocusX(), detector.getFocusY());
            setImageMatrix(mMatrix);
            Log.d(TAG, "scale-->" + detector.getScaleFactor());
            return true;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            setScaleType(ScaleType.MATRIX);
            return super.onScaleBegin(detector);
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            super.onScaleEnd(detector);
        }
    };

    private void init() {
        mGestureDector = new GestureDetector(getContext(), mGestureListener);
        mScaleGestureDetector = new ScaleGestureDetector(getContext(), mScaleGestureListener);
        mMatrix = new Matrix();
        mScroller = new OverScroller(getContext());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean result = false;
        mGestureDector.onTouchEvent(event);
        //mScaleGestureDetector.onTouchEvent(event);

        int action = event.getActionMasked();
        int x = (int) event.getX();
        int y = (int) event.getY();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                setScaleType(ImageView.ScaleType.MATRIX);
                mPrevDistance = 0;
                changeState(MODE_SCROLL);
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                break;
            case MotionEvent.ACTION_MOVE:
                //Log.d(TAG, "scale --> " + getCurrentScale());
                if (event.getPointerCount() >= 2) {
                    changeState(MODE_SCALE);
                    int x1 = (int) event.getX();
                    int y1 = (int) event.getY();
                    int x2 = (int) event.getX(1);
                    int y2 = (int) event.getY(1);

                    int currDistance = getDistance(x1, y1, x2, y2);
                    if (mPrevDistance == 0) {
                        mPrevDistance = currDistance;
                        break;
                    }
                    float scale = currDistance * 1.0f / mPrevDistance;
                    mMatrix.set(getImageMatrix());
                    mMatrix.postScale(scale, scale, (x1 + x2) / 2, (y1 + y2) / 2);
                    setImageMatrix(mMatrix);
                    mPrevDistance = currDistance;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                mPrevDistance = 0;
                //restoreCenter();
                break;
            default:
                break;
        }


        return true;
    }

    private int getDistance(int x1, int y1, int x2, int y2) {
        return (int) Math.hypot(x1 - x2, y1 - y2);
    }

    private void postTranslate(float x, float y) {
        mMatrix.set(getImageMatrix());
        mMatrix.postTranslate(x, y);
        setImageMatrix(mMatrix);
    }
}
