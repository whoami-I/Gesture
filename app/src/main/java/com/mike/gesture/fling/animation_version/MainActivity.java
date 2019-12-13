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

            @Override
            public boolean onDown(MotionEvent e) {
                //如果在fling，那么当手指在屏幕上，那么停止fling
                if (flingValueAnimator.isRunning()) {
                    flingValueAnimator.cancel();
                }
                return super.onDown(e);
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
    private static final int NB_SAMPLES = 100;
    private static final float[] SPLINE_POSITION = new float[NB_SAMPLES + 1];
    private static final float[] SPLINE_TIME = new float[NB_SAMPLES + 1];
    private static final float START_TENSION = 0.5f;
    private static final float END_TENSION = 1.0f;
    private static final float P1 = START_TENSION * INFLEXION;
    private static final float P2 = 1.0f - END_TENSION * (1.0f - INFLEXION);


    float mVelocityY;
    int mFlingDistance;
    int mFlingDuration;
    ValueAnimator flingValueAnimator;
    double mLast = 0;

    private void init() {
        ppi = getResources().getDisplayMetrics().density * 160.0f;
        mPhysicalCoeff = SensorManager.GRAVITY_EARTH // g (m/s^2)
                * 39.37f // inch/meter
                * ppi
                * 0.84f;

        float x_min = 0.0f;
        float y_min = 0.0f;
        for (int i = 0; i < NB_SAMPLES; i++) {
            final float alpha = (float) i / NB_SAMPLES;

            float x_max = 1.0f;
            float x, tx, coef;
            while (true) {
                x = x_min + (x_max - x_min) / 2.0f;
                coef = 3.0f * x * (1.0f - x);
                tx = coef * ((1.0f - x) * P1 + x * P2) + x * x * x;
                if (Math.abs(tx - alpha) < 1E-5) break;
                if (tx > alpha) x_max = x;
                else x_min = x;
            }
            SPLINE_POSITION[i] = coef * ((1.0f - x) * START_TENSION + x) + x * x * x;

            float y_max = 1.0f;
            float y, dy;
            while (true) {
                y = y_min + (y_max - y_min) / 2.0f;
                coef = 3.0f * y * (1.0f - y);
                dy = coef * ((1.0f - y) * START_TENSION + y) + y * y * y;
                if (Math.abs(dy - alpha) < 1E-5) break;
                if (dy > alpha) y_max = y;
                else y_min = y;
            }
            SPLINE_TIME[i] = coef * ((1.0f - y) * P1 + y * P2) + y * y * y;
        }
        SPLINE_POSITION[NB_SAMPLES] = SPLINE_TIME[NB_SAMPLES] = 1.0f;

        flingValueAnimator = new ValueAnimator();
        flingValueAnimator.setInterpolator(new LinearInterpolator());
        flingValueAnimator.removeAllUpdateListeners();
        flingValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int currTime = (int) animation.getAnimatedValue();
                //float p = interpolator.getInterpolation((currTime / mFlingDuration));
                int dis = getCurrDistance( currTime);
                ll.scrollBy(0, (int) -(dis - mLast));
                Log.d("TAG", "dis:" + (dis));
                //记录本次动画期间上一次总的滑动距离
                mLast = dis;
            }
        });
    }


    private void fling(float velocityX, final float velocityY) {
        if (flingValueAnimator.isRunning()) {
            flingValueAnimator.cancel();
        }
        mLast = 0;
        mVelocityY = velocityY;
        final int duration = getSplineFlingDuration((int) velocityY);
        final double flingDistance = getSplineFlingDistance((int) velocityY);
        flingValueAnimator.setIntValues(0, duration);
        flingValueAnimator.setDuration(duration);
        mFlingDistance = (int) flingDistance;
        mFlingDuration = duration;
        flingValueAnimator.start();

        Log.d("TAG","getSplineFlingDistance:"+flingDistance);
        Log.d("TAG","setDuration:"+duration);
    }

    private int getCurrDistance(int currentTime){
        final float t = (float) currentTime / mFlingDuration;
        final int index = (int) (NB_SAMPLES * t);
        float distanceCoef = 1.f;
        float velocityCoef = 0.f;
        if (index < NB_SAMPLES) {
            final float t_inf = (float) index / NB_SAMPLES;
            final float t_sup = (float) (index + 1) / NB_SAMPLES;
            final float d_inf = SPLINE_POSITION[index];
            final float d_sup = SPLINE_POSITION[index + 1];
            velocityCoef = (d_sup - d_inf) / (t_sup - t_inf);
            distanceCoef = d_inf + (t - t_inf) * velocityCoef;
        }

        return (int) (distanceCoef * mFlingDistance);
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
