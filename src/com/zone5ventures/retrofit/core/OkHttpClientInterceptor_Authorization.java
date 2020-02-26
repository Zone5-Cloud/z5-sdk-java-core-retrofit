package com.zone5ventures.retrofit.core;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class OkHttpClientInterceptor_Authorization implements Interceptor {
	
	final String bearer;
	
	public OkHttpClientInterceptor_Authorization(String token) {
		this.bearer = String.format("Bearer %s", token);
	}
	
	@Override
	public Response intercept(Chain chain) throws IOException {
		Request originalRequest = chain.request();
		Request.Builder builder = originalRequest.newBuilder();
		if (bearer != null)
			builder = builder.header("Authorization", bearer);
        Request newRequest = builder.build();
        return chain.proceed(newRequest);
	}

}
