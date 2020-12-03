package com.zone5cloud.retrofit.core;

import java.io.IOException;

import com.zone5cloud.core.enums.Z5HttpHeader;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class OkHttpClientInterceptor_NoDecorate implements Interceptor {
		
	@Override
	public Response intercept(Chain chain) throws IOException {
		Request originalRequest = chain.request();
        Request.Builder builder = originalRequest.newBuilder().header(Z5HttpHeader.TP_NO_DECORATE.toString(), "true");
        Request newRequest = builder.build();
        return chain.proceed(newRequest);
	}

}
