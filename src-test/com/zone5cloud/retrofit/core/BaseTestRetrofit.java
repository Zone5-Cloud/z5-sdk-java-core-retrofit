package com.zone5cloud.retrofit.core;

import static org.junit.Assert.assertTrue;

import org.junit.Before;

import com.google.gson.Gson;
import com.zone5cloud.core.Z5AuthorizationDelegate;
import com.zone5cloud.core.oauth.AuthToken;
import com.zone5cloud.core.users.LoginRequest;
import com.zone5cloud.core.users.LoginResponse;
import com.zone5cloud.core.utils.GsonManager;
import com.zone5cloud.retrofit.core.apis.ActivitiesAPI;
import com.zone5cloud.retrofit.core.apis.MetricsAPI;
import com.zone5cloud.retrofit.core.apis.OAuthAPI;
import com.zone5cloud.retrofit.core.apis.ThirdPartyTokenAPI;
import com.zone5cloud.retrofit.core.apis.UserAPI;
import com.zone5cloud.retrofit.core.apis.UserAgentAPI;

import okhttp3.OkHttpClient;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class BaseTestRetrofit extends BaseTest {
	
	protected UserAPI userApi = null;
	protected ActivitiesAPI activitiesApi = null;
	protected MetricsAPI metricsApi = null;
	protected ThirdPartyTokenAPI thirdPartyApi = null;
	protected OAuthAPI authApi = null;
	protected UserAgentAPI agentApi = null;
	protected OkHttpClientInterceptor_Authorization auth = null;
	
//	protected AuthToken authToken = null;
	protected Z5AuthorizationDelegate delegate = new Z5AuthorizationDelegate() {
		@Override
		public void onAuthTokenUpdated(AuthToken token) {
//			authToken = token;
			clientConfig.setToken(token);
		}
	};
	
	@Before
	public void init() {
		OkHttpClientInterceptor_NoDecorate nodecorate = new OkHttpClientInterceptor_NoDecorate();
		auth = new OkHttpClientInterceptor_Authorization(clientConfig, delegate);
		OkHttpClientInterceptor_UserAgent agent = new OkHttpClientInterceptor_UserAgent("ride-iOS/3.6.4 (1)");
		
        Gson gson = GsonManager.getInstance();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(getBaseEndpoint())
                .client(new OkHttpClient.Builder().cookieJar(new OkHttpClientCookieJar()).addInterceptor(nodecorate).addInterceptor(agent).addInterceptor(auth).build())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();

        userApi = retrofit.create(UserAPI.class);
        activitiesApi = retrofit.create(ActivitiesAPI.class);
        metricsApi = retrofit.create(MetricsAPI.class);
        thirdPartyApi = retrofit.create(ThirdPartyTokenAPI.class);
        authApi = retrofit.create(OAuthAPI.class);
        agentApi = retrofit.create(UserAgentAPI.class);
    }
	
	public boolean isSpecialized() {
		return getBaseEndpoint() != null && (getBaseEndpoint().equals("https://api-sp.todaysplan.com.au")
				|| getBaseEndpoint().equals("https://api-sp-staging.todaysplan.com.au"));
	}
	
	protected LoginResponse login() {
		Response<LoginResponse> response = userApi.login(new LoginRequest(TEST_EMAIL, TEST_PASSWORD,
				clientConfig.getClientID(), clientConfig.getClientSecret())).blockingFirst();
		assertTrue("Failed to login - please check configuration in BaseTest.java", response.isSuccessful());
		return response.body();
	}
}
