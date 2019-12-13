package com.mike.gesture.gesture;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.core.view.GestureDetectorCompat;
import androidx.core.view.ViewConfigurationCompat;

import com.mike.gesture.R;
import com.mike.gesture.base.BaseActivity;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

import butterknife.BindView;

public class MainActivity extends BaseActivity {

    private static final String TAG = "MainActivity";
    @BindView(R.id.img)
    ImageView img;

    @Override
    public int getContentViewId() {
        return R.layout.activity_gesture;
    }

    BroadcastReceiver mScreenOffReceiver;
    Matrix matrix = new Matrix();

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void afterSetContentView() {

        final GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            public boolean onScroll(MotionEvent e1, MotionEvent e2,
                                    float distanceX, float distanceY) {
                Log.d("HAHA", "distanceX -> " + distanceX + "; distanceY" + distanceY);
                img.scrollBy(0, (int) distanceY);
                return true;
            }


            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                                   float velocityY) {
                Log.d("HAHA", "velocityX -> " + velocityX + "; velocityY" + velocityY);
                //img.scrollTo(0,0);

                return true;
            }

        });

        final VelocityTracker velocityTracker = VelocityTracker.obtain();

        img.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //gestureDetector.onTouchEvent(event);
                velocityTracker.addMovement(event);
                int action = event.getAction();
                if(action == MotionEvent.ACTION_UP){
                    velocityTracker.computeCurrentVelocity(100, ViewConfiguration.get(MainActivity.this).getScaledMaximumFlingVelocity());
                    Log.d("YYY",velocityTracker.getXVelocity()+";   "+velocityTracker.getYVelocity());
                }
                return true;
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mScreenOffReceiver != null) {
            unregisterReceiver(mScreenOffReceiver);
        }
    }
}
