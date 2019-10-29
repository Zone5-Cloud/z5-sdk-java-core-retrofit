package com.zone5ventures.retrofit.core;

import org.junit.Before;

import com.google.gson.Gson;
import com.zone5ventures.core.utils.GsonManager;
import com.zone5ventures.retrofit.core.OkHttpClientCookieJar;
import com.zone5ventures.retrofit.core.OkHttpClientInterceptor_Authorization;
import com.zone5ventures.retrofit.core.OkHttpClientInterceptor_NoDecorate;
import com.zone5ventures.retrofit.core.apis.ActivitiesAPI;
import com.zone5ventures.retrofit.core.apis.UserAPI;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class BaseTestRetrofit extends BaseTest {
	
	protected UserAPI userApi = null;
	protected ActivitiesAPI activitiesApi = null;
	
	@Before
	public void init() {
		String authToken = super.token;
		
		OkHttpClientInterceptor_NoDecorate nodecorate = new OkHttpClientInterceptor_NoDecorate();
		OkHttpClientInterceptor_Authorization auth = new OkHttpClientInterceptor_Authorization(authToken);
		
        Gson gson = GsonManager.getInstance();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(getBaseEndpoint())
                .client(new OkHttpClient().newBuilder().cookieJar(new OkHttpClientCookieJar()).addInterceptor(nodecorate).addInterceptor(auth).build())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .build();

        userApi = retrofit.create(UserAPI.class);
        activitiesApi = retrofit.create(ActivitiesAPI.class);
    }
}
