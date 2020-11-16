package com.zone5cloud.retrofit.core;

import org.junit.Before;

import com.google.gson.Gson;
import com.zone5cloud.core.utils.GsonManager;
import com.zone5cloud.retrofit.core.apis.ActivitiesAPI;
import com.zone5cloud.retrofit.core.apis.MetricsAPI;
import com.zone5cloud.retrofit.core.apis.ThirdPartyTokenAPI;
import com.zone5cloud.retrofit.core.apis.UserAPI;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class BaseTestRetrofit extends BaseTest {
	
	protected UserAPI userApi = null;
	protected ActivitiesAPI activitiesApi = null;
	protected MetricsAPI metricsApi = null;
	protected ThirdPartyTokenAPI thirdPartyAPI = null;
	
	@Before
	public void init() {
		String authToken = super.token;
		
		OkHttpClientInterceptor_NoDecorate nodecorate = new OkHttpClientInterceptor_NoDecorate();
		OkHttpClientInterceptor_Authorization auth = new OkHttpClientInterceptor_Authorization(authToken);
		OkHttpClientInterceptor_UserAgent agent = new OkHttpClientInterceptor_UserAgent("ride-iOS/3.6.4 (1320)");
		
        Gson gson = GsonManager.getInstance();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(getBaseEndpoint())
                .client(new OkHttpClient().newBuilder().cookieJar(new OkHttpClientCookieJar()).addInterceptor(nodecorate).addInterceptor(auth).addInterceptor(agent).build())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();

        userApi = retrofit.create(UserAPI.class);
        activitiesApi = retrofit.create(ActivitiesAPI.class);
        metricsApi = retrofit.create(MetricsAPI.class);
        thirdPartyAPI = retrofit.create(ThirdPartyTokenAPI.class);
    }
	
	public boolean isSpecialized() {
		return getBaseEndpoint() != null && (getBaseEndpoint().equals("https://api-sp.todaysplan.com.au") || getBaseEndpoint().equals("https://api-sp-staging.todaysplan.com.au"));
	}
	
	protected void setToken(String token) {
		super.token = token;
		init();
	}
}
