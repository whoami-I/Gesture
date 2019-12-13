package com.mike.gesture.fling.scroller_version;

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
import android.widget.TextView;

import com.mike.gesture.R;
import com.mike.gesture.base.BaseActivity;

import butterknife.BindView;

public class MainActivity extends BaseActivity {

    private static final String TAG = "MainActivity";
    @BindView(R.id.ll)
    MLinearLayout ll;

    @Override
    public int getContentViewId() {
        return R.layout.activity_fling;
    }


    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void afterSetContentView() {
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < 100; i++) {
            View view = inflater.inflate(R.layout.item_home_list, ll, false);
            ((TextView) view.findViewById(R.id.tv_item_name)).setText("" + i);
            ll.addView(view);
        }

        final GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            public boolean onScroll(MotionEvent e1, MotionEvent e2,
                                    float distanceX, float distanceY) {


                View firstItem = ll.getChildAt(0);
                Log.d("haha", "getY->" + firstItem.getY() + "; getTop-->" + firstItem.getTop() + ";getTranslationY-->" + firstItem.getTranslationY());

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
                return true;
            }


            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                                   float velocityY) {
//                Log.d("HAHA", "velocityX -> " + velocityX + "; velocityY" + velocityY);
//                Log.d("HAHA", "getHeight -> " + ll.getHeight());

                ll.startScrollerFling((int) velocityY);
                return true;
            }

        });


        ll.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                gestureDetector.onTouchEvent(event);
                int action = event.getAction();
                if(action == MotionEvent.ACTION_DOWN){
                    ll.getmScroller().forceFinished(true);
                }
                return true;
            }
        });
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
