package com.mike.gesture.home;


import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mike.gesture.R;
import com.mike.gesture.base.BaseActivity;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;


public class MainActivity extends BaseActivity {

    @BindView(R.id.recyclerview)
    RecyclerView recyclerView;

    List<ItemDataBean> mDataList = new ArrayList<>();

    @Override
    public int getContentViewId() {
        return R.layout.activity_main;
    }

    @Override
    public void afterSetContentView() {
        initDataList();
        LinearLayoutManager llm = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        recyclerView.setLayoutManager(llm);
        HomeListAdapter homeListAdapter = new HomeListAdapter(this, mDataList);
        recyclerView.setAdapter(homeListAdapter);

        View v = findViewById(R.id.recyclerview);
    }

    private void initDataList() {
        mDataList.add(new ItemDataBean("SimpleGesture",
                com.mike.gesture.simplegesture.MainActivity.class, ColorGenerator.getInstance().getColor()));
    }


}
