package com.donutcn.memo.fragment.discover;

import android.app.ProgressDialog;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.donutcn.memo.App;
import com.donutcn.memo.R;
import com.donutcn.memo.adapter.FriendListAdapter;
import com.donutcn.memo.base.BaseScrollFragment;
import com.donutcn.memo.entity.ArrayResponse;
import com.donutcn.memo.entity.Contact;
import com.donutcn.memo.ContactDao;
import com.donutcn.memo.DaoSession;
import com.donutcn.memo.event.RequestRefreshEvent;
import com.donutcn.memo.listener.OnItemClickListener;
import com.donutcn.memo.listener.OnQueryListener;
import com.donutcn.memo.listener.UploadCallback;
import com.donutcn.memo.utils.HttpUtils;
import com.donutcn.memo.utils.PermissionCheck;
import com.donutcn.memo.utils.SpfsUtils;
import com.donutcn.memo.utils.StringUtil;
import com.donutcn.memo.utils.ToastUtil;
import com.donutcn.memo.utils.UserStatus;
import com.donutcn.memo.view.ListViewDecoration;
import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.scwang.smartrefresh.layout.api.RefreshLayout;
import com.scwang.smartrefresh.layout.listener.OnRefreshListener;
import com.yanzhenjie.recyclerview.swipe.SwipeMenuRecyclerView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FriendsFragment extends BaseScrollFragment {

    private SwipeMenuRecyclerView mHaoYe_rv;
    private SmartRefreshLayout mRefreshLayout;
    private View mContainer, mNoMatch;

    private FriendListAdapter mAdapter;
    private static ArrayList<Contact> mTempList;
    private static List<Contact> mContactsList;
    private Context mContext;
    private ContactDao mContactDao;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
        DaoSession daoSession = ((App)mContext.getApplicationContext()).getDaoSession();
        mContactDao = daoSession.getContactDao();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_friends, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        view.findViewById(R.id.start_match).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startMatch();
            }
        });
        mContainer = view.findViewById(R.id.unmatch_container);
        mNoMatch = view.findViewById(R.id.no_matched_contact);

        mHaoYe_rv = (SwipeMenuRecyclerView) view.findViewById(R.id.recycler_view);
        setRecyclerView(mHaoYe_rv);
        mRefreshLayout = (SmartRefreshLayout) view.findViewById(R.id.swipe_layout);
        mRefreshLayout.setOnRefreshListener(mRefreshListener);
        mRefreshLayout.setEnableLoadmore(false);

        mHaoYe_rv.setLayoutManager(new LinearLayoutManager(mContext));
        mHaoYe_rv.addItemDecoration(new ListViewDecoration(getContext(), R.dimen.item_decoration_height));
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mContactsList = new ArrayList<>();
        // check the local database first.
        if(SpfsUtils.readBoolean(mContext, SpfsUtils.USER, "match_contacts", false)){
            mContactsList = mContactDao
                    .queryBuilder()
                    .orderAsc(ContactDao.Properties.SortKey)
                    .build().list();
            mAdapter = new FriendListAdapter(mContext, mContactsList);
            mAdapter.setOnItemClickListener(mOnItemClickListener);
            mHaoYe_rv.setAdapter(mAdapter);
            if(mContactsList.size() > 0){
                mRefreshLayout.setVisibility(View.VISIBLE);
                mNoMatch.setVisibility(View.GONE);
                mContainer.setVisibility(View.GONE);
            }else {
                mRefreshLayout.setVisibility(View.GONE);
                mNoMatch.setVisibility(View.VISIBLE);
                mContainer.setVisibility(View.GONE);
            }
        }
    }

    public void Refresh() {
        mAdapter = new FriendListAdapter(mContext, mTempList);
        mAdapter.setOnItemClickListener(mOnItemClickListener);

        mHaoYe_rv.setAdapter(mAdapter);
    }

    private OnRefreshListener mRefreshListener = new OnRefreshListener() {
        @Override
        public void onRefresh(RefreshLayout refreshlayout) {
            refreshlayout.finishRefresh(1000);
        }
    };

    private OnItemClickListener mOnItemClickListener = new OnItemClickListener() {
        @Override
        public void onItemClick(int position) {

        }
    };

    public void startMatch(){
        if(PermissionCheck.checkContactsPermission(this)){
            final ProgressDialog dialog = new ProgressDialog(mContext);
            dialog.setTitle("正在匹配联系人");
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();
            getContactsAsync(new UploadCallback<Contact>() {
                @Override
                public void uploadProgress(int progress, int total) {
                    dialog.setMax(total);
                    dialog.setProgress(progress);
                }

                @Override
                public void uploadAll(List<Contact> list) {
                    mAdapter = new FriendListAdapter(mContext, mContactsList);
                    mAdapter.setOnItemClickListener(mOnItemClickListener);
                    mHaoYe_rv.setAdapter(mAdapter);
                    if(mContactsList.size() > 0){
                        mRefreshLayout.setVisibility(View.VISIBLE);
                        mNoMatch.setVisibility(View.GONE);
                        mContainer.setVisibility(View.GONE);
                    }else {
                        mRefreshLayout.setVisibility(View.GONE);
                        mNoMatch.setVisibility(View.VISIBLE);
                        mContainer.setVisibility(View.GONE);
                    }
                    dialog.dismiss();
                    SpfsUtils.write(mContext, SpfsUtils.USER, "match_contacts", true);
                    ToastUtil.show(mContext, "匹配完成");
                    // insert matched contact into database.
                    for(Contact contact : mContactsList) {
                        Contact findContact = mContactDao
                                .queryBuilder()
                                .where(ContactDao.Properties.ContactId.eq(contact.getContactId()))
                                .build().unique();
                        if(findContact != null){
                            mContactDao.deleteByKey(findContact.getContactId());
                        }
                        mContactDao.insert(contact);
                    }
                }

                @Override
                public void uploadFail(String error) {
                }
            });
        }else {
            ToastUtil.show(mContext, "未授权读取联系人");
        }
    }

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
        if(event.getRefreshPosition() == 3){
            mHaoYe_rv.scrollToPosition(0);
            mRefreshLayout.autoRefresh(0);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionCheck.PERMISSION_READ_CONTACTS) {

        }
    }

    private void getContactsAsync(final UploadCallback<Contact> listener) {
        ContactsQueryHandler qh = new ContactsQueryHandler(getContext().getContentResolver(),
                new OnQueryListener<Contact>() {
                    @Override
                    public void onQueryProgress(int progress, int total) {
                    }

                    @Override
                    public void onQueryComplete(List<Contact> list) {
                        uploadSignatureCode(list, listener);
                    }
                });
        // query field
        String[] projection = {ContactsContract.CommonDataKinds.Phone._ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.DATA1, "sort_key",
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY};
        qh.startQuery(0, null, ContactsContract.CommonDataKinds.Phone.CONTENT_URI, projection, null, null,
                "sort_key COLLATE LOCALIZED asc");
    }

    // upload signature code for matching friends.
    private void uploadSignatureCode(List<Contact> list, final UploadCallback<Contact> listener){
        List<String> codes = new ArrayList<>();
        int count = 0, record = 0;
        int  currentTime = 0;
        final int times = list.size() / 20 + 1;
        for(int i = 0; i < list.size(); i++){
            String code = StringUtil.getMD5(list.get(i).getPhoneNum());
            codes.add(code);
            count++;
            record++;
            // upload 20 contacts at a time.
            if(count == 20 || i == list.size() - 1){
                listener.uploadProgress(record, list.size());
                count = 0;
                currentTime++;
                final int finalCurrentTime = currentTime;
                HttpUtils.matchContacts(codes).enqueue(new Callback<ArrayResponse<Contact>>() {
                    @Override
                    public void onResponse(Call<ArrayResponse<Contact>> call,
                                           Response<ArrayResponse<Contact>> response) {
                        if(response.body().isOk()){
                            int length = response.body().size();
                            for (int i = 0; i < length; i++){
                                mContactsList.add(response.body().getData().get(i));
                            }
                            if(finalCurrentTime == times){
                                listener.uploadAll(mContactsList);
                            }
                        }else {
                            Log.e("match_code", "匹配失败");
                        }
                    }

                    @Override
                    public void onFailure(Call<ArrayResponse<Contact>> call, Throwable t) {
                        t.printStackTrace();
                    }
                });
            }
        }
    }

    static class ContactsQueryHandler extends AsyncQueryHandler {

        private OnQueryListener<Contact> listener;

        public ContactsQueryHandler(ContentResolver cr, OnQueryListener<Contact> listener) {
            super(cr);
            this.listener = listener;
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            if (cursor != null && cursor.getCount() > 0) {
                Map<String, Contact> contactIdMap = new HashMap<>();
                mTempList = new ArrayList<>();
                cursor.moveToFirst();
                int size = cursor.getCount();
                for (int i = 0; i < cursor.getCount(); i++) {
                    cursor.moveToPosition(i);
                    String name = cursor.getString(1);
                    String number = cursor.getString(2);
                    number = number.replace(" ", "");
                    String sortKey = cursor.getString(3);
                    int contactId = cursor.getInt(4);
                    String lookUpKey = cursor.getString(5);

                    String username = UserStatus.getCurrentUser().getUsername();
                    // remove duplicate numbers and your own number.
                    if (contactIdMap.containsKey(number)
                            || (username != null && contactIdMap.containsKey(username))) {
                        // do nothing
                    } else {
                        // create a contact object.
                        Contact contact = new Contact();
                        contact.setDisplayName(name);
                        contact.setPhoneNum(number);
                        contact.setSortKey(sortKey);
                        contact.setContactId(contactId);
                        contact.setLookUpKey(lookUpKey);
                        mTempList.add(contact);
                        contactIdMap.put(number, contact);
                    }
                    listener.onQueryProgress(i / size * 100, size);
                }
                cursor.close();
                listener.onQueryComplete(mTempList);
            }
            super.onQueryComplete(token, cookie, cursor);
        }
    }
}
