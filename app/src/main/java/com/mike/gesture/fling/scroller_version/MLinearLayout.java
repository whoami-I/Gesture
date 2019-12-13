package com.mike.gesture.fling.scroller_version;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.OverScroller;

import androidx.annotation.Nullable;

public class MLinearLayout extends LinearLayout {
    private OverScroller mScroller;

    public MLinearLayout(Context context) {
        this(context,null);
    }

    public MLinearLayout(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    public MLinearLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mScroller = new OverScroller(this.getContext());

    }

    public OverScroller getmScroller(){
        return mScroller;
    }


    @Override
    public void computeScroll() {
        //动画未完成
        if (mScroller.computeScrollOffset()) {
            scrollTo(0, mScroller.getCurrY());
            //触发draw()
            postInvalidate();
        }
    }

    public void startScrollerFling(int velocityY) {
        mScroller.fling(getLeft(), getScrollY(), 0, -velocityY, 0, 0, 0, 120000+getScrollY());
        //invalidate();
    }
}
