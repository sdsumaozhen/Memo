package com.donutcn.memo.utils;

import android.content.Context;

import com.donutcn.memo.listener.OnUploadAllListener;
import com.franmontiel.persistentcookiejar.ClearableCookieJar;
import com.franmontiel.persistentcookiejar.PersistentCookieJar;
import com.franmontiel.persistentcookiejar.cache.SetCookieCache;
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.UpCompletionHandler;
import com.qiniu.android.storage.UpProgressHandler;
import com.qiniu.android.storage.UploadManager;
import com.qiniu.android.storage.UploadOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * com.donutcn.memo.utils
 * Created by 73958 on 2017/7/27.
 */

public class HttpUtils {

    public static final String SERVER_HOST = "ascexz.320.io/GoodPage/API";

    private static final String PATH = "http://" + SERVER_HOST + "/";

    public static final int SUCCESS = 200;

    public static final int FAIL = 400;

    private static String uploadToken = "4AXvKBpu_OIE9RE_18fQnzY8ux-CA9rEnL8HaQ79:II7VbOh0bMKhOMtzpOk6IExUxX0=:eyJzY29wZSI6InJlbnJlbnBhZ2UiLCJkZWFkbGluZSI6MTUwMTMwODc1MX0=";

    private static AtomicInteger uploadCount = new AtomicInteger(0);

    private static Retrofit instance;

    private static ClearableCookieJar cookieJar;

    private static UploadManager uploadManager;

    private static List<String> fileKeys;

    public static synchronized void create(Context context) {
        if (instance == null) {
            cookieJar = new PersistentCookieJar(
                    new SetCookieCache(), new SharedPrefsCookiePersistor(context));
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .cookieJar(cookieJar)
                    .build();
            instance = new Retrofit.Builder()
                    .client(okHttpClient)
                    .baseUrl(PATH)
                    .build();
        }
    }

    private static synchronized RRPageService create() {
        return instance.create(RRPageService.class);
    }

    public static void clearCookies(String phoneNumber){
        cookieJar.clear();
//        logout(phoneNumber).enqueue(null);
    }

    private static UploadManager getUpload(){
        if(uploadManager == null){
            uploadManager = new UploadManager();
        }
        return uploadManager;
    }

    /**
     * get the response state code.
     * @param str response string
     */
    public static int stateCode(String str){
        try {
            JSONObject json = new JSONObject(str);
            return json.getInt("code");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return FAIL;
    }

    /**
     * get the response message.
     * @param str response string
     */
    public static String message(String str){
        try {
            JSONObject json = new JSONObject(str);
            return json.getString("message");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return "null";
    }

    public interface RRPageService {

        /**
         * user login.
         * @param user user info
         */
        @POST(APIPath.LOGIN)
        Call<ResponseBody> login(@Body RequestBody user);

        /**
         * request for message Verification Code.
         * @param phone phone number
         */
        @POST(APIPath.GET_AUTH_CODE)
        Call<ResponseBody> getVerifiedCode(@Body RequestBody phone);

        /**
         * user register.
         * @param phone user info
         */
        @POST(APIPath.REGISTER)
        Call<ResponseBody> register(@Body RequestBody phone);

        /**
         * modify user password
         * @param phone user info
         */
        @POST(APIPath.MODIFY_PASSWORD)
        Call<ResponseBody> modifyPassword(@Body RequestBody phone);

        /**
         * user logout
         * @param phone phone number
         */
        @POST(APIPath.LOGOUT)
        Call<ResponseBody> logout(@Body RequestBody phone);

        @POST("article_api/ceshi")
        Call<ResponseBody> test();
    }

    private class APIPath{
        private static final String LOGIN = "login_api";

        private static final String GET_AUTH_CODE = "login_api/authcode_api";

        private static final String REGISTER = "login_api/register";

        private static final String MODIFY_PASSWORD = "login_api/edituser_api";

        private static final String LOGOUT = "login_api/login_out_api";
    }

    public static Call<ResponseBody> login(String username, String password){
        String str = "{\"username\":\"" + username + "\"," +
                "\"password\":\"" + password + "\"}";
        RequestBody request = RequestBody
                .create(okhttp3.MediaType.parse("application/json; charset=utf-8"), str);
        return create().login(request);
    }

    public static Call<ResponseBody> getVerifiedCode(String phoneNumber, String action){
        String str = "{\"tel_number\":\"" + phoneNumber + "\"," +
                "\"action\":\"" + action + "\"}";
        RequestBody request = RequestBody
                .create(okhttp3.MediaType.parse("application/json; charset=utf-8"), str);
        return create().getVerifiedCode(request);
    }

    public static Call<ResponseBody> modifyUser(String phoneNumber, String authCode,
                                              String password, int action){
        String str = "{\"tel_number\":\"" + phoneNumber + "\"," +
                        "\"authcode\":\"" + authCode + "\"," +
                        "\"password\":\"" + password + "\"}";
        RequestBody request = RequestBody
                .create(okhttp3.MediaType.parse("application/json; charset=utf-8"), str);
        if(action == 0)
            return create().register(request);
        else
            return create().modifyPassword(request);
    }

    public static Call<ResponseBody> logout(String phoneNumber){
        String str = "{\"username\":\"" + phoneNumber  + "\"}";
        RequestBody request = RequestBody
                .create(okhttp3.MediaType.parse("application/json; charset=utf-8"), str);
        return create().logout(request);
    }

    public static Call<ResponseBody> test(){
        return create().test();
    }

    public static void upLoadImages(final List<String> paths, final OnUploadAllListener listener){
        fileKeys = Collections.synchronizedList(new ArrayList<String>());
        for(String path : paths){
            getUpload().put(path, null, uploadToken,
                    new UpCompletionHandler() {
                        @Override
                        public void complete(String key, ResponseInfo info, JSONObject response) {
                            uploadCount.addAndGet(1);
                            // store the file key.
                            fileKeys.add(key);
                            if(uploadCount.get() == paths.size()){
                                listener.uploadAll(fileKeys);
                                // reset uploadCount.
                                uploadCount.getAndSet(0);
                            }
                        }

                    }, new UploadOptions(null, null, false, new UpProgressHandler() {
                        @Override
                        public void progress(String key, double percent) {

                        }
                    }, null));
        }
    }
}