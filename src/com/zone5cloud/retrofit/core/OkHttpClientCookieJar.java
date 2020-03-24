package com.zone5cloud.retrofit.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

public class OkHttpClientCookieJar implements CookieJar {
	
	private Cookie jsession = null;
	private Cookie elb = null;
		
	@Override
	public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
		if (cookies != null) {
			for(Cookie c : cookies) {
				if (c.name().equals("JSESSIONID"))
					jsession = c;
				
				else if (c.name().equals("AWSELB") || c.name().equals("AWSALB"))
					elb = c;
			}
		}
	}

	@Override
	public List<Cookie> loadForRequest(HttpUrl url) {
		if (jsession != null && elb != null)
			return Arrays.asList(jsession, elb);
		else if (jsession != null)
			return Arrays.asList(jsession);
		else if (elb != null)
			return Arrays.asList(elb);
		return Collections.emptyList();
	}
}
