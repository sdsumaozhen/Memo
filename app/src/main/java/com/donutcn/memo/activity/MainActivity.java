package com.donutcn.memo.activity;

import android.content.Intent;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;

import com.donutcn.memo.R;
import com.donutcn.memo.adapter.ViewPagerAdapter;
import com.donutcn.memo.entity.SimpleResponse;
import com.donutcn.memo.event.LoginStateEvent;
import com.donutcn.memo.event.RequestRefreshEvent;
import com.donutcn.memo.fragment.SplashFragment;
import com.donutcn.memo.fragment.discover.DiscoverFragment;
import com.donutcn.memo.fragment.home.HomeFragment;
import com.donutcn.memo.helper.LoginHelper;
import com.donutcn.memo.utils.HttpUtils;
import com.donutcn.memo.utils.LogUtil;
import com.donutcn.memo.utils.ToastUtil;
import com.donutcn.memo.utils.UserStatus;
import com.donutcn.memo.utils.WindowUtils;
import com.donutcn.widgetlib.widget.CheckableImageButton;
import com.tencent.android.tpush.XGIOperateCallback;
import com.tencent.android.tpush.XGPushManager;
import com.umeng.socialize.UMShareAPI;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private ViewPager mViewPager;
    private ViewPagerAdapter mAdapter;
    private CheckableImageButton mHome, mDiscover, mPublish;
    public SplashFragment splashFragment;
    private HomeFragment mHomeFragment;
    private DiscoverFragment mDiscoverFragment;

    private Intent mServiceIntent;

    private long mExitTime = 0;
    private int mDefaultItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        WindowUtils.setStatusBarColor(this, R.color.colorPrimary, true);

        if (getIntent().getBooleanExtra("unlogin", false)) {
            mDefaultItem = 1;
        } else {
            // sync user info.
            getWindow().getDecorView().postDelayed(new Runnable() {
                @Override
                public void run() {
                    HttpUtils.syncUserInfo().enqueue(new Callback<SimpleResponse>() {
                        @Override
                        public void onResponse(Call<SimpleResponse> call, Response<SimpleResponse> response) {
                            if (response.body() != null) {
                                LogUtil.d("sync", response.body().toString());
                                if(response.body().isOk()){
                                    LoginHelper.syncUserInfo(MainActivity.this, response.body().getData());
                                } else if (response.body().unAuthorized()) {
                                    // check if the cookie is out of date.
                                    LogUtil.e("unAuthorized", response.body().toString());
                                    ToastUtil.show(MainActivity.this, "登录授权过期，请重新登录");
                                    LoginHelper.logout(MainActivity.this);
                                }
                            } else {
                                ToastUtil.show(MainActivity.this, "连接失败，服务器未知错误");
                            }
                        }

                        @Override
                        public void onFailure(Call<SimpleResponse> call, Throwable t) {
                            t.printStackTrace();
                            ToastUtil.show(MainActivity.this, "连接失败，请检查你的网络连接");
                        }
                    });
                }
            }, 1000);
        }

        mViewPager = (ViewPager) findViewById(R.id.main_viewpager);
        mHome = (CheckableImageButton) findViewById(R.id.main_bottom_home);
        mPublish = (CheckableImageButton) findViewById(R.id.main_bottom_pub);
        mDiscover = (CheckableImageButton) findViewById(R.id.main_bottom_dis);

        mHome.setOnClickListener(this);
        mPublish.setOnClickListener(this);
        mDiscover.setOnClickListener(this);

        initViewPager();
    }

    private void initViewPager() {
        mAdapter = new ViewPagerAdapter(getSupportFragmentManager(), this);
        mHomeFragment = new HomeFragment();
        mDiscoverFragment = new DiscoverFragment();
        mAdapter.addFragment(mHomeFragment);
        mAdapter.addFragment(mDiscoverFragment);
        mViewPager.setAdapter(mAdapter);
        if(mDefaultItem != 0){
            mHome.setChecked(false);
            mDiscover.setChecked(true);
            mViewPager.setCurrentItem(mDefaultItem, false);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.main_bottom_home:
                if(mHome.isChecked()){
                    EventBus.getDefault().post(new RequestRefreshEvent(
                            mHomeFragment.getCurrentPagePosition()));
                }else {
                    mHome.setChecked(true);
                    mDiscover.setChecked(false);
                    mViewPager.setCurrentItem(0, false);
                }
                break;
            case R.id.main_bottom_dis:
                if(mDiscover.isChecked()){
                    EventBus.getDefault().post(new RequestRefreshEvent(
                            mDiscoverFragment.getCurrentPagePosition() + 2));
                }else {
                    mHome.setChecked(false);
                    mDiscover.setChecked(true);
                    mViewPager.setCurrentItem(1, false);
                }
                break;
            case R.id.main_bottom_pub:
                if(LoginHelper.ifRequestLogin(this, "请先登录")){
                    return;
                }
                startActivity(new Intent(this, PublishActivity.class));
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        UMShareAPI.get(this).onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
        if(splashFragment == null){
            super.onBackPressed();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK
                && event.getAction() == KeyEvent.ACTION_DOWN
                && splashFragment == null) {
            if ((System.currentTimeMillis() - mExitTime) > 2000) {
                ToastUtil.show(MainActivity.this, getString(R.string.toast_exit_double_click));
                mExitTime = System.currentTimeMillis();
            } else {
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.setAction("ExitApp");
                startActivity(intent);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Subscribe(sticky = true)
    public void onLoginStateEvent(LoginStateEvent event) {
        if (event.isLogin()) {
            // register push service
            XGPushManager.registerPush(getApplicationContext(),
                    UserStatus.getCurrentUser().getUserId(), new XGIOperateCallback() {
                @Override
                public void onSuccess(Object o, int i) {
                    LogUtil.d("XGPush", "注册成功，设备token为：" + o);
                }

                @Override
                public void onFail(Object o, int i, String s) {
                    LogUtil.d("XGPush", "注册失败，错误码：" + i + ",错误信息：" + s);
                }
            });
        }
    }
}
