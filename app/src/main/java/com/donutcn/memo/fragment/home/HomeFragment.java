package com.donutcn.memo.fragment.home;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.donutcn.memo.R;
import com.donutcn.memo.activity.PersonalCenterActivity;
import com.donutcn.memo.adapter.TabFragmentPagerAdapter;
import com.donutcn.memo.listener.OnReceiveNewMessagesListener;
import com.flyco.tablayout.SlidingTabLayout;
import com.flyco.tablayout.listener.OnTabSelectListener;

public class HomeFragment extends Fragment implements OnTabSelectListener, OnReceiveNewMessagesListener {

    private TabFragmentPagerAdapter mPagerAdapter;
    private ViewPager mViewPager;
    private SlidingTabLayout mTabLayout;
    private ImageView mUserCenter_iv;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        mUserCenter_iv = (ImageView) view.findViewById(R.id.user_center);
        mUserCenter_iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getContext(), PersonalCenterActivity.class));
            }
        });

        mPagerAdapter = new TabFragmentPagerAdapter(getActivity().getSupportFragmentManager(), getContext(), 0);
        mPagerAdapter.setOnReceiveNewMessages(this);
        mViewPager = (ViewPager) view.findViewById(R.id.home_viewpager);
        mViewPager.setAdapter(mPagerAdapter);
        mTabLayout = (SlidingTabLayout) view.findViewById(R.id.sliding_tabs);
        mTabLayout.setViewPager(mViewPager);
        mTabLayout.setOnTabSelectListener(this);
        mTabLayout.showMsg(0, 2);
        mTabLayout.showDot(1);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Refresh();
    }

    @Override
    public void onReceiveNewMessage(int msgCount, int msgType) {
        switch (msgType) {
            case 0:
                mTabLayout.showMsg(0, msgCount);
                break;
            case 1:
                mTabLayout.showMsg(1, msgCount);
                break;
            default:
                break;
        }
    }

    @Override
    public void onTabSelect(int position) {
        mTabLayout.hideMsg(position);
    }

    @Override
    public void onTabReselect(int position) {
        mTabLayout.hideMsg(position);
    }

    public void update() {
        Refresh();
    }

    public void Refresh() {

    }

    @Override
    public void onResume() {
        super.onResume();
        Refresh();
    }
}
