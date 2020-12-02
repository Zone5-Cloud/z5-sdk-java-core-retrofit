package com.zone5cloud.retrofit.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Before;
import org.junit.Test;

import com.zone5cloud.core.Z5AuthorizationDelegate;
import com.zone5cloud.core.enums.GrantType;
import com.zone5cloud.core.oauth.AuthToken;
import com.zone5cloud.core.oauth.OAuthToken;
import com.zone5cloud.core.users.LoginResponse;
import com.zone5cloud.core.users.User;

import okhttp3.ResponseBody;
import retrofit2.Response;

public class TestOAuthAPI extends BaseTestRetrofit {
	private String email = "";
	private Long id = null;
	
	@Before
	public void setup() throws IOException {
		LoginResponse response = login();
		id = response.getUser().getId();
		email = response.getUser().getEmail();
	}

	@Test
	public void testManualRefresh() {
		
		// Exercise the refresh access token
		if (isSpecialized()) {
			OAuthToken alt = userApi.refreshToken().blockingFirst().body();
			assertNotNull(alt.getToken());
			assertNotNull(alt.getTokenExp());
			assertTrue(alt.getTokenExp() > System.currentTimeMillis() + 30000);
		} else {
			Response<OAuthToken> response = authApi.refreshAccessToken(clientID, clientSecret, email, GrantType.REFRESH_TOKEN, auth.getToken().getRefreshToken()).blockingFirst();
			OAuthToken tok = response.body();
			assertNotNull(tok.getToken());
			assertNotNull(tok.getTokenExp());
			assertNotNull(tok.getRefreshToken());
			assertTrue(tok.getTokenExp() > System.currentTimeMillis() + 30000);
		}
		
		// check access after refresh
		User me = userApi.me().blockingFirst().body();
		assertEquals(me.getId(), id);
	}
	
	@Test
	public void testAutoRefresh() {
		AuthToken currentToken = auth.getToken();
		
		// expire the token to force the refresh sequence
		OAuthToken expiredToken = new OAuthToken();
		expiredToken.setToken(currentToken.getToken());
		expiredToken.setRefreshToken(currentToken.getRefreshToken());
		expiredToken.setTokenExp(System.currentTimeMillis());
		auth.setToken(expiredToken);
		
		User me = userApi.me().blockingFirst().body();
		assertEquals(me.getId(), id);
		
		// check token has been updated
		AuthToken newToken = auth.getToken();
		
		assertNotEquals(currentToken.getToken(), newToken.getToken());
		assertEquals(currentToken.getRefreshToken(), newToken.getRefreshToken());
		assertTrue(newToken.getTokenExp() > System.currentTimeMillis() + 30000);
	}
	
	@Test
	public void testDelegate() throws InterruptedException {
		final AtomicBoolean d1 = new AtomicBoolean(false);
		final AtomicBoolean d2 = new AtomicBoolean(false);
		final Semaphore s1 = new Semaphore(0);
		final Semaphore s2 = new Semaphore(0);
		final AtomicBoolean shouldTrigger = new AtomicBoolean(false);
		
		Z5AuthorizationDelegate delegate1 = new Z5AuthorizationDelegate() {
			
			@Override
			public void onAuthTokenUpdated(AuthToken token) {
				d1.set(true);
				s1.release();
			}
		};
		
		Z5AuthorizationDelegate delegate2 = new Z5AuthorizationDelegate() {
			
			@Override
			public void onAuthTokenUpdated(AuthToken token) {
				d2.set(true);
				if (!shouldTrigger.get()) {
					assertFalse("delegate should not trigger", true);
				}
				s2.release();
			}
		};
		
		OkHttpClientInterceptor_Authorization interceptor = new OkHttpClientInterceptor_Authorization(null, "123", null, delegate1, delegate2);
		
		assertEquals(2, interceptor.delegates.size());
		
		interceptor.unsubscribe(delegate2);
		
		assertEquals(1, interceptor.delegates.size());
		assertTrue(interceptor.delegates.contains(delegate1));
		interceptor.setToken(new OAuthToken());
		s1.acquire();
		
		assertTrue(d1.get());
		assertFalse(d2.get());
		// reset for next test
		d1.set(false);
		
		interceptor.subscribe(delegate2);
		assertEquals(2, interceptor.delegates.size());
		shouldTrigger.set(true);
		assertTrue(interceptor.delegates.contains(delegate1));
		assertTrue(interceptor.delegates.contains(delegate2));
		
		assertFalse(d1.get());
		assertFalse(d2.get());
		interceptor.setToken(null);
		s1.acquire();
		s2.acquire();
		assertTrue(d1.get());
		assertTrue(d2.get());
		
	}
	
	@Test
	public void testErrors() throws Exception {
		Response<OAuthToken> response = null;
		if (authToken.getRefreshToken() != null) {
			response = authApi.refreshAccessToken("bogus client id", "bogus secret", TEST_EMAIL, GrantType.REFRESH_TOKEN, authToken.getRefreshToken()).blockingFirst();
		} else {
			auth.setClientIDAndSecret("bogus clientid", "bogus email");
			response = userApi.refreshToken().blockingFirst();
		}
		assertFalse(response.isSuccessful());
		assertEquals(401, response.code());
		
		ResponseBody body = response.errorBody();
		System.out.println(body.string());
	}
}
