package com.mike.gesture.zoomimageview;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
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
    private static final int MODE_TAP = 5;
    private int mState = MODE_UNINIT;

    private static final int SHORT_MODE = 30;
    private static final int LONG_MODE = 31;

    private int DEFAULT_SCALE_TIME = 150;
    private ScaleAnimator mScaleAnimator;

    private Matrix mMatrix;

    private int mPrevDistance;
    /* rect to calculate the size and position of current drawable */
    private RectF mRectF = new RectF();
    /* Gesture detector,detect scroll and fling */
    private GestureDetector mGestureDector;
    private ScaleGestureDetector mScaleGestureDetector;

    // to help fling
    private OverScroller mScroller;

    // for aniamtion


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

        boolean mShortMode = false;

        @Override
        public boolean onDown(MotionEvent e) {
            /* end the fling */
            mScroller.forceFinished(true);
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            Log.d(TAG,"onDoubleTap");
            if (mShortMode) {
                fill(SHORT_MODE);
            } else {
                fill(LONG_MODE);
            }
            mShortMode = !mShortMode;
            return true;
        }

        /* finger move right velocityX > 0, move down velocityY > 0*/
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            Log.d(TAG, "velocityX -->" + velocityX + "; velocityY -->" + velocityY);
            if (mState != MODE_SCROLL) {
                //如果现在的状态不是scroll，那么就不fling
                return true;
            }
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

    private void fill(int mode) {
        if (mScaleAnimator.isRunning()) {
            mScaleAnimator.cancel();
        }
        Matrix matrix = new Matrix();
        matrix.set(getImageMatrix());
        mRectF.set(0, 0, getDrawable().getIntrinsicWidth(), getDrawable().getIntrinsicHeight());
        getImageMatrix().mapRect(mRectF);
        float widthRatio = getWidth() / mRectF.width();
        float heightRatio = getHeight() / mRectF.height();
        float ratio = 0.0f;
        float dx = (getWidth() / 2 - (mRectF.right + mRectF.left) / 2);
        float dy = (int) (getHeight() / 2 - (mRectF.bottom + mRectF.top) / 2);
        if (mode == SHORT_MODE) {
            ratio = (widthRatio > heightRatio) ? heightRatio : widthRatio;
        } else if (mode == LONG_MODE) {
            ratio = (widthRatio < heightRatio) ? heightRatio : widthRatio;
        } else {
            throw new RuntimeException("mode must be one of SHORT_MODE or LONG_MODE");
        }
        matrix.postTranslate(dx, dy);
        matrix.postScale(ratio, ratio, getWidth() / 2, getHeight() / 2);
        mScaleAnimator.set(getImageMatrix(), matrix);
        mScaleAnimator.start();
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
        mScaleAnimator = new ScaleAnimator();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean result = false;
        mGestureDector.onTouchEvent(event);
        //mScaleGestureDetector.onTouchEvent(event);

        int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                setScaleType(ImageView.ScaleType.MATRIX);
                mPrevDistance = 0;
                changeState(MODE_SCROLL);
//                if (mScaleAnimator.isRunning()) {
//                    mScaleAnimator.cancel();
//                }
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
            case MotionEvent.ACTION_POINTER_UP:
                mPrevDistance = 0;
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                // after finger up, set mPrevDistance to 0
                mPrevDistance = 0;
                //当手指抬起时，需要确定现在的状态：FLING or SCALE
                if (mState == MODE_SCALE) {
                    afterScale();
                }
                break;
            default:
                break;
        }
        return true;
    }

    /**
     * scale之后，drawable可能会小于Imagview或者是偏移了一部分，这时就要将重新设置imagview的状态
     * 1 如果drawable两条边都小于imageview，则按照宽边原则，将drawable放大到imagview大小
     * 2 如果drawable有一边小于imagview，那么就要使得drawable这一条边居中
     * 3
     */
    private void afterScale() {
        if (mScaleAnimator.isRunning()) {
            mScaleAnimator.cancel();
        }

        Matrix matrix = new Matrix();
        matrix.set(getImageMatrix());
        mRectF.set(0, 0, getDrawable().getIntrinsicWidth(), getDrawable().getIntrinsicHeight());
        getImageMatrix().mapRect(mRectF);
        Log.d("HJKL", "width->" + getWidth() + "    height->" + getHeight());
        Log.d("HJKL", "left->" + mRectF.left + "    top->" + mRectF.top + "    right" + mRectF.right + "    bottom" + mRectF.bottom);
        if (mRectF.width() < getWidth() && mRectF.height() < getHeight()) {
            // 需要放大并且将drawable中心移到imageview中心
            //1 移到中心
            float postX = getWidth() / 2 - (mRectF.right + mRectF.left) / 2;
            float postY = getHeight() / 2 - (mRectF.bottom + mRectF.top) / 2;
            matrix.postTranslate(postX, postY);
            //2 放大相应的倍数
            float widthRatio = getWidth() / mRectF.width();
            float heightRatio = getHeight() / mRectF.height();
            //选取更小的放大倍数
            float ratio = (widthRatio > heightRatio) ? heightRatio : widthRatio;
            matrix.postScale(ratio, ratio, getWidth() / 2, getHeight() / 2);
            mScaleAnimator.set(getImageMatrix(), matrix);
            mScaleAnimator.start();
        } else {

            //剩下的情况都是移动即可
            float dx = 0;
            float dy = 0;
            /* 条件1和2不可能同时满足两个，只会有一个成立 */
            //条件1
            if (mRectF.width() > getWidth()) {
                //此时，drawable比Imagiew宽，但是drawable的边在ImageView里面，则需要移动
                if (mRectF.right < getWidth()) {
                    dx = getWidth() - mRectF.right;
                }
                if (mRectF.left > 0) {
                    dx = -mRectF.left;
                }
                //如果此时还有drawable高度比Imagiew高度小的话，那么竖直居中
                if (mRectF.height() <= getHeight()) {
                    dy = (getHeight() - mRectF.bottom - mRectF.top) / 2;
                }
            }
            //条件2
            if (mRectF.height() > getHeight()) {
                if (mRectF.top > 0) {
                    dy = -mRectF.top;
                }
                if (mRectF.bottom < getHeight()) {
                    dy = getHeight() - mRectF.bottom;
                }
                //如果此时还有drawable高度比Imagiew宽度小的话，那么水平居中
                if (mRectF.width() <= getWidth()) {
                    dx = (getWidth() - mRectF.left - mRectF.right) / 2;
                }
            }
            matrix.postTranslate(dx, dy);
            mScaleAnimator.set(getImageMatrix(), matrix);
            mScaleAnimator.start();
        }
    }

    private int getDistance(int x1, int y1, int x2, int y2) {
        return (int) Math.hypot(x1 - x2, y1 - y2);
    }

    private void postTranslate(float x, float y) {
        mMatrix.set(getImageMatrix());
        mMatrix.postTranslate(x, y);
        setImageMatrix(mMatrix);
    }


    private class ScaleAnimator extends ValueAnimator implements ValueAnimator.AnimatorUpdateListener {
        float[] result = new float[9];
        float[] start = new float[9];
        float[] end = new float[9];

        public ScaleAnimator() {
            initOthers();
        }

        public ScaleAnimator(Matrix startMatrix, Matrix endMatrix) {
            super();
            startMatrix.getValues(start);
            endMatrix.getValues(end);
            initOthers();
        }

        void initOthers() {
            addUpdateListener(this);
            addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    getImageMatrix().setValues(end);
                    Log.d(TAG, "onAnimationEnd");
                    invalidate();
                }

                @Override
                public void onAnimationStart(Animator animation) {
                    super.onAnimationStart(animation);
                }
            });
            setDuration(DEFAULT_SCALE_TIME);
            setFloatValues(0, 1);
        }

        public void set(Matrix startMatrix, Matrix endMatrix) {
            startMatrix.getValues(start);
            endMatrix.getValues(end);
        }

        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            float fraction = animation.getAnimatedFraction();
            for (int i = 0; i < 9; i++) {
                result[i] = start[i] + fraction * (end[i] - start[i]);
            }
            getImageMatrix().setValues(result);
            invalidate();
        }
    }
}
