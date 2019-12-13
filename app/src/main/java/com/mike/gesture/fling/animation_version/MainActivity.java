package com.mike.gesture.fling.animation_version;

import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.hardware.SensorManager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.LinearInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mike.gesture.R;
import com.mike.gesture.base.BaseActivity;

import butterknife.BindView;

public class MainActivity extends BaseActivity {

    private static final String TAG = "MainActivity";
    @BindView(R.id.ll)
    LinearLayout ll;

    @Override
    public int getContentViewId() {
        return R.layout.activity_fling_animation;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void afterSetContentView() {
        //填充linealayout
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < 100; i++) {
            View view = inflater.inflate(R.layout.item_home_list, ll, false);
            ((TextView) view.findViewById(R.id.tv_item_name)).setText("" + i);
            ll.addView(view);
        }

        final GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            public boolean onScroll(MotionEvent e1, MotionEvent e2,
                                    float distanceX, float distanceY) {

//                View firstItem = ll.getChildAt(0);
//                Log.d("haha", "getY->" + firstItem.getY() + "; getTop-->" + firstItem.getTop() + ";getTranslationY-->" + firstItem.getTranslationY());

                //跟随手指移动
                if (distanceY > 0) {
                    //down
                    if (!arriveBottom()) {
                        ll.scrollBy(0, (int) distanceY);
                    }
                } else {
                    if (!arriveTop()) {
                        ll.scrollBy(0, (int) distanceY);
                    }
                }
                //如果在fling，那么当手指在屏幕上，那么停止fling
                if (flingValueAnimator.isRunning()) {
                    flingValueAnimator.cancel();
                }
                return true;
            }


            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                                   float velocityY) {
//                Log.d("HAHA", "velocityX -> " + velocityX + "; velocityY" + velocityY);
//                Log.d("HAHA", "getHeight -> " + ll.getHeight());

                fling(velocityX, velocityY);
                return true;
            }

        });

        ll.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                gestureDetector.onTouchEvent(event);
                return true;
            }
        });

        //初始化fling相关参数
        init();
    }

    /* --------- fling的滑动参数 --------*/

    /* 滑动递减速率 */
    private static float DECELERATION_RATE = (float) (Math.log(0.78) / Math.log(0.9));
    private static final float INFLEXION = 0.35f;
    /* 摩擦系数 */
    private float mFlingFriction = ViewConfiguration.getScrollFriction();
    float ppi;
    /* 物理摩擦系数 */
    private float mPhysicalCoeff = SensorManager.GRAVITY_EARTH // g (m/s^2)
            * 39.37f // inch/meter
            * ppi
            * 0.84f; // look and feel tuning

    /* 动画 */
    TimeInterpolator interpolator = new ViscousFluidInterpolator();
    float mVelocityY;
    double mFlingDistance;
    int mFlingDuration;
    ValueAnimator flingValueAnimator;
    double mLast = 0;

    private void init() {
        ppi = getResources().getDisplayMetrics().density * 160.0f;
        mPhysicalCoeff = SensorManager.GRAVITY_EARTH // g (m/s^2)
                * 39.37f // inch/meter
                * ppi
                * 0.84f;
        flingValueAnimator = new ValueAnimator();
        flingValueAnimator.setInterpolator(new LinearInterpolator());
        flingValueAnimator.removeAllUpdateListeners();
        flingValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float currTime = (float) animation.getAnimatedValue();
                float p = interpolator.getInterpolation((currTime / mFlingDuration));
                double dis = p * mFlingDistance;
                ll.scrollBy(0, (int) -(dis - mLast));
                Log.d("TAG", "dis:" + (dis - mLast));
                //记录本次动画期间上一次总的滑动距离
                mLast = dis;
            }
        });
    }


    private void fling(float velocityX, final float velocityY) {

        //        Log.d("TAG","getSplineFlingDistance:"+flingDistance);
//        Log.d("TAG","setDuration:"+duration);

        if (flingValueAnimator.isRunning()) {
            flingValueAnimator.cancel();
        }
        mLast = 0;
        mVelocityY = velocityY;
        final int duration = getSplineFlingDuration((int) velocityY);
        final double flingDistance = getSplineFlingDistance((int) velocityY);
        flingValueAnimator.setFloatValues(0, duration);
        flingValueAnimator.setDuration(duration);
        mFlingDistance = flingDistance;
        mFlingDuration = duration;
        flingValueAnimator.start();
    }


    private int getSplineFlingDuration(int velocity) {
        final double l = getSplineDeceleration(velocity);
        final double decelMinusOne = DECELERATION_RATE - 1.0;
        return (int) (1000.0 * Math.exp(l / decelMinusOne));
    }

    private double getSplineDeceleration(int velocity) {
        return Math.log(INFLEXION * Math.abs(velocity) / (mFlingFriction * mPhysicalCoeff));
    }

    private double getSplineFlingDistance(int velocity) {
        final double l = getSplineDeceleration(velocity);
        final double decelMinusOne = DECELERATION_RATE - 1.0;
        return mFlingFriction * mPhysicalCoeff * Math.exp(DECELERATION_RATE / decelMinusOne * l) * Math.signum(velocity);
    }

    boolean arriveTop() {
        int scrollY = ll.getScrollY();
        View firstItem = ll.getChildAt(0);
        float y = firstItem.getY();
        if (scrollY <= 0) {
            return true;
        } else {
            return false;
        }
    }

    boolean arriveBottom() {
        View lastItem = ll.getChildAt(ll.getChildCount() - 1);
        float y = lastItem.getY();
        if (y <= ll.getHeight()) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
