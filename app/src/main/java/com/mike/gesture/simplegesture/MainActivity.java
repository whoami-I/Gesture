package com.mike.gesture.simplegesture;

import android.annotation.SuppressLint;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import com.mike.gesture.R;
import com.mike.gesture.base.BaseActivity;

import butterknife.BindView;

public class MainActivity extends BaseActivity {

    private static final String TAG = "MainActivity";

    @BindView(R.id.img)
    ImageView img;

    @Override
    public int getContentViewId() {
        return R.layout.activity_simplegesture;
    }


    public float getCurrentScale() {
        float[] values = new float[9];
        matrix.getValues(values);
        return values[Matrix.MSCALE_X];
    }

    public float getCurrentScale(Matrix matrix) {
        float[] values = new float[9];
        matrix.getValues(values);
        return values[Matrix.MSCALE_X];
    }

    Matrix matrix = new Matrix();
    float mInitScale = 0.0f;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void afterSetContentView() {

        img.setOnTouchListener(new View.OnTouchListener() {
            Point origFirstPoint = new Point();
            Point oriSecondPoint = new Point();
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //以index为0和1的手指进行缩放
                int action = event.getActionMasked();
                int x = (int) event.getX();
                int y = (int) event.getY();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        img.setScaleType(ImageView.ScaleType.MATRIX);
                        break;
                    case MotionEvent.ACTION_POINTER_DOWN:
                        origFirstPoint.set(x, y);
                        x = (int) event.getX(1);
                        y = (int) event.getY(1);
                        oriSecondPoint.set(x, y);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        Log.d(TAG, "scale --> " + getCurrentScale());
                        if (event.getPointerCount() >= 2) {
                            int x1 = (int) event.getX(1);
                            int y1 = (int) event.getY(1);
                            int dis = getDistance(x, y, x1, y1);
                            int distance = getDistance(origFirstPoint.x, origFirstPoint.y, oriSecondPoint.x, oriSecondPoint.y);
                            float scale = dis * 1.0f / distance;
                            //scale = scale / preScale;
                            //preScale = scale;
//                            Log.d(TAG, "x:" + x + ";y:" + y + ";x1:" + x1 + ";y1:" + y1 + ";distance" +
//                                    distance + ";dis:" + dis + ";scale:" + scale);
                            matrix.set(img.getImageMatrix());
                            matrix.postScale(scale, scale, (x + x1) / 2, (y + y1) / 2);
                            img.setImageMatrix(matrix);
                            origFirstPoint.set(x, y);
                            oriSecondPoint.set(x1, y1);
                        }
                        break;
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_UP:
                        restoreCenter();
                        break;
                    default:
                        break;
                }
                return false;
            }
        });


        img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, ""+img.getDrawable().getIntrinsicWidth()+";"+img.getDrawable().getIntrinsicHeight());
                Log.d(TAG, ""+img.getWidth()+";"+img.getHeight());

//                img.setScaleType(ImageView.ScaleType.MATRIX);
//
//
//                matrix.set(img.getImageMatrix());
//
//
//                float currentScale = getCurrentScale();
//                float scale = mInitScale/currentScale;
//                matrix.postScale(scale, scale, (img.getWidth()) / 2, (img.getHeight()) / 2);
//                img.setImageMatrix(matrix);


//                matrix.set(img.getImageMatrix());
                //matrix.postTranslate(20, 0);
//                matrix.postScale(0.9f, 0.9f);
//                img.setImageMatrix(matrix);

//                Matrix matrix1 = new Matrix();
//                matrix1.set(img.getImageMatrix());
//                matrix1.postScale(0.8f,0.8f);
//                img.setImageMatrix(matrix1);
            }
        });


    }

    private void restoreCenter(){
        Matrix m = img.getImageMatrix();
        RectF rectF = new RectF(0, 0, img.getDrawable().getIntrinsicWidth(), img.getDrawable().getIntrinsicHeight());
        m.mapRect(rectF);
        int postX = (int) (img.getWidth() / 2 - (rectF.right + rectF.left) / 2);
        int postY = (int) (img.getHeight() / 2 - (rectF.bottom + rectF.top) / 2);
        matrix.set(m);
        matrix.postTranslate(postX, postY);
        img.setImageMatrix(matrix);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        mInitScale = getCurrentScale(img.getImageMatrix());
    }

    int getDistance(int x1, int y1, int x2, int y2) {
        return (int) Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
    }


}
