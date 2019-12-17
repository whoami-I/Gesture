package com.mike.gesture.zoomimageview;

import com.mike.gesture.R;
import com.mike.gesture.base.BaseActivity;

import butterknife.BindView;

public class MainActivity extends BaseActivity {

    private static final String TAG = "MainActivity";

    @BindView(R.id.img)
    ZoomImageView img;

    @Override
    public int getContentViewId() {
        return R.layout.activity_zoomimageview;
    }

    @Override
    public void afterSetContentView() {

    }
}
