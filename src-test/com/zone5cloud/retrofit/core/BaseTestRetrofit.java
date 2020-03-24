package com.zone5cloud.retrofit.core;

import org.junit.Before;

import com.google.gson.Gson;
import com.zone5cloud.retrofit.core.OkHttpClientCookieJar;
import com.zone5cloud.retrofit.core.OkHttpClientInterceptor_Authorization;
import com.zone5cloud.retrofit.core.OkHttpClientInterceptor_NoDecorate;
import com.zone5cloud.retrofit.core.apis.ActivitiesAPI;
import com.zone5cloud.retrofit.core.apis.UserAPI;
import com.zone5cloud.core.utils.GsonManager;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
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
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();

        userApi = retrofit.create(UserAPI.class);
        activitiesApi = retrofit.create(ActivitiesAPI.class);
    }
	
	public boolean isSpecialized() {
		return getBaseEndpoint() != null && (getBaseEndpoint().equals("https://api-sp.todaysplan.com.au") || getBaseEndpoint().equals("https://api-sp-staging.todaysplan.com.au"));
	}
	
	protected void setToken(String token) {
		super.token = token;
		init();
	}
}
