package com.zone5cloud.retrofit.core;

import java.io.IOException;

import com.zone5cloud.core.enums.Z5HttpHeader;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class OkHttpClientInterceptor_UserAgent implements Interceptor {
	
	final String agent;
	
	public OkHttpClientInterceptor_UserAgent(String agent) {
		this.agent = agent;
	}
	
	@Override
	public Response intercept(Chain chain) throws IOException {
		Request originalRequest = chain.request();
		Request.Builder builder = originalRequest.newBuilder();
		if (agent != null)
			builder = builder.header(Z5HttpHeader.USER_AGENT.toString(), agent);
        Request newRequest = builder.build();
        return chain.proceed(newRequest);
	}

}
