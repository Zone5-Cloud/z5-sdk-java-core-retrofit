package com.zone5cloud.retrofit.core;

import com.zone5cloud.core.Z5AuthorizationDelegate;
import com.zone5cloud.core.oauth.AuthToken;

import okhttp3.OkHttpClient;

public class Z5Factory {

	public static OkHttpClient createNewClient(AuthToken token, String clientID, String clientSecret, Z5AuthorizationDelegate ...delegates) {
		OkHttpClientInterceptor_NoDecorate nodecorate = new OkHttpClientInterceptor_NoDecorate();
		OkHttpClientInterceptor_Authorization auth = new OkHttpClientInterceptor_Authorization(token, clientID, clientSecret, delegates);
		OkHttpClientInterceptor_UserAgent agent = new OkHttpClientInterceptor_UserAgent("ride-iOS/3.6.4 (1320)");
		
		return new OkHttpClient.Builder().cookieJar(new OkHttpClientCookieJar()).addInterceptor(nodecorate).addInterceptor(agent).addInterceptor(auth).build();
	}
	
	
}
