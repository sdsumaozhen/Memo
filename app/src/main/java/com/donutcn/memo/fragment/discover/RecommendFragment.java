package com.donutcn.memo.fragment.discover;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.donutcn.memo.R;
import com.donutcn.memo.activity.ArticlePage;
import com.donutcn.memo.activity.MainActivity;
import com.donutcn.memo.activity.SearchActivity;
import com.donutcn.memo.adapter.HaoYeAdapter;
import com.donutcn.memo.base.BaseScrollFragment;
import com.donutcn.memo.entity.ArrayResponse;
import com.donutcn.memo.entity.BriefContent;
import com.donutcn.memo.event.ReceiveNewMessagesEvent;
import com.donutcn.memo.event.RequestRefreshEvent;
import com.donutcn.memo.listener.OnItemClickListener;
import com.donutcn.memo.type.ItemLayoutType;
import com.donutcn.memo.utils.HttpUtils;
import com.donutcn.memo.view.ListViewDecoration;
import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.scwang.smartrefresh.layout.api.RefreshLayout;
import com.scwang.smartrefresh.layout.listener.OnLoadmoreListener;
import com.scwang.smartrefresh.layout.listener.OnRefreshListener;
import com.yanzhenjie.recyclerview.swipe.Closeable;
import com.yanzhenjie.recyclerview.swipe.OnSwipeMenuItemClickListener;
import com.yanzhenjie.recyclerview.swipe.SwipeMenu;
import com.yanzhenjie.recyclerview.swipe.SwipeMenuCreator;
import com.yanzhenjie.recyclerview.swipe.SwipeMenuItem;
import com.yanzhenjie.recyclerview.swipe.SwipeMenuRecyclerView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RecommendFragment extends BaseScrollFragment implements View.OnClickListener {

    private SwipeMenuRecyclerView mHaoYe_rv;
    private SmartRefreshLayout mRefreshLayout;
    private TextView mSearch_tv;

    private HaoYeAdapter adapter;
    private ArrayList<BriefContent> list;
    private Context mContext;

    private int page = 1;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recommend, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mHaoYe_rv = (SwipeMenuRecyclerView) view.findViewById(R.id.recycler_view);
        setRecyclerView(mHaoYe_rv);
        mRefreshLayout = (SmartRefreshLayout) view.findViewById(R.id.swipe_layout);
        mSearch_tv = (TextView) view.findViewById(R.id.recommend_search);
        mRefreshLayout.setOnRefreshListener(mRefreshListener);
        mRefreshLayout.setOnLoadmoreListener(mLoadmoreListener);
        mSearch_tv.setOnClickListener(this);

        mHaoYe_rv.setLayoutManager(new LinearLayoutManager(mContext));
        mHaoYe_rv.addItemDecoration(new ListViewDecoration(getContext(),
                R.dimen.item_decoration_height, 84, 8));

        // set up swipe menu.
        mHaoYe_rv.setSwipeMenuCreator(mSwipeMenuCreator);
        mHaoYe_rv.setSwipeMenuItemClickListener(mHaoYeItemClickListener);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        EventBus.getDefault().register(this);
        page = ((MainActivity)getActivity()).mRecommendPage;
        list = new ArrayList<>();
        adapter = new HaoYeAdapter(mContext, list, ItemLayoutType.AVATAR_IMG);
        adapter.setOnItemClickListener(mOnItemClickListener);
        mHaoYe_rv.setAdapter(adapter);
        Refresh();
    }

    public void Refresh() {
        HttpUtils.getRecommendContent(1).enqueue(new Callback<ArrayResponse>() {
            @Override
            public void onResponse(Call<ArrayResponse> call, Response<ArrayResponse> response) {
                if(response.body().isOk()){
                    list.addAll(response.body().getData());
                    adapter.notifyDataSetChanged();
                }else {
                    Toast.makeText(getContext(), "已经到底部了", Toast.LENGTH_SHORT).show();
                }
                mRefreshLayout.finishRefresh();
            }

            @Override
            public void onFailure(Call<ArrayResponse> call, Throwable t) {
                t.printStackTrace();
                Toast.makeText(getContext(), "推荐连接失败", Toast.LENGTH_SHORT).show();
                mRefreshLayout.finishRefresh();
            }
        });
    }

    public void LoadMore() {
        page = ((MainActivity)getActivity()).mRecommendPage;
        HttpUtils.getRecommendContent(page).enqueue(new Callback<ArrayResponse>() {
            @Override
            public void onResponse(Call<ArrayResponse> call, Response<ArrayResponse> response) {
                if(response.body().isOk()){
                    list.addAll(10 * (page - 1), response.body().getData());
                    adapter.notifyDataSetChanged();
                    ((MainActivity)getActivity()).mRecommendPage++;
                }else {
                    Toast.makeText(getContext(), "已经到底部了", Toast.LENGTH_SHORT).show();
                }
                mRefreshLayout.finishLoadmore();
            }

            @Override
            public void onFailure(Call<ArrayResponse> call, Throwable t) {
                t.printStackTrace();
                Toast.makeText(getContext(), "推荐连接失败", Toast.LENGTH_SHORT).show();
                mRefreshLayout.finishLoadmore();
            }
        });
    }

    private OnRefreshListener mRefreshListener = new OnRefreshListener() {
        @Override
        public void onRefresh(RefreshLayout refreshlayout) {
            Refresh();
        }
    };

    private OnLoadmoreListener mLoadmoreListener = new OnLoadmoreListener() {
        @Override
        public void onLoadmore(RefreshLayout refreshlayout) {
            LoadMore();
        }
    };

    /**
     * Menu creator. Call when creates the menu.
     */
    private SwipeMenuCreator mSwipeMenuCreator = new SwipeMenuCreator() {
        @Override
        public void onCreateMenu(SwipeMenu swipeLeftMenu, SwipeMenu swipeRightMenu, int viewType) {
            int width = getResources().getDimensionPixelSize(R.dimen.swipe_menu_item_width);
            int height = ViewGroup.LayoutParams.MATCH_PARENT;

            // Add the swipe menu on the right
            {
                SwipeMenuItem editItem = new SwipeMenuItem(mContext)
                        .setBackgroundDrawable(R.drawable.selector_gray)
                        .setText(getResources().getString(R.string.btn_swipe_share))
                        .setTextColor(Color.WHITE)
                        .setTextSize(16)
                        .setWidth(width)
                        .setHeight(height);
                swipeRightMenu.addMenuItem(editItem);
            }
        }
    };

    private OnItemClickListener mOnItemClickListener = new OnItemClickListener() {
        @Override
        public void onItemClick(int position) {
            EventBus.getDefault().post(new ReceiveNewMessagesEvent(2, position));
            Intent intent = new Intent(getContext(), ArticlePage.class);
            intent.putExtra("contentId", list.get(position).getId());
            startActivity(intent);
        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.recommend_search:
                startActivity(new Intent(getContext(), SearchActivity.class));
                break;
        }
    }

    /**
     * Menu onClickListener
     */
    private OnSwipeMenuItemClickListener mHaoYeItemClickListener
            = new OnSwipeMenuItemClickListener() {
        /**
         * @param closeable       Used for close the menu
         * @param adapterPosition position of recyclerView item
         * @param menuPosition    position of swipe menu item
         * @param direction       left swipe menu,value：{@link SwipeMenuRecyclerView#LEFT_DIRECTION}，
         *                        right swipe menu,value：{@link SwipeMenuRecyclerView#RIGHT_DIRECTION}.
         */
        @Override
        public void onItemClick(Closeable closeable, int adapterPosition, int menuPosition, int direction) {
            // close the swipe menu
            closeable.smoothCloseMenu();

            if (direction == SwipeMenuRecyclerView.RIGHT_DIRECTION) {
                Toast.makeText(mContext, "list第" + adapterPosition + "; 右侧菜单第" + menuPosition,
                        Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe
    public void onRequestRefreshEvent(RequestRefreshEvent event){
        if(event.getRefreshPosition() == 2){
            mHaoYe_rv.scrollToPosition(0);
            mRefreshLayout.autoRefresh(0);
        }
    }
}
