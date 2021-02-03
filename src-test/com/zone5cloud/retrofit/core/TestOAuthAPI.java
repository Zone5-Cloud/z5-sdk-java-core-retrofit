package com.zone5cloud.retrofit.core;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import com.zone5cloud.core.ClientConfig;
import com.zone5cloud.core.Z5AuthorizationDelegate;
import com.zone5cloud.core.Z5Error;
import com.zone5cloud.core.enums.GrantType;
import com.zone5cloud.core.oauth.AuthToken;
import com.zone5cloud.core.oauth.OAuthToken;
import com.zone5cloud.core.users.LoginResponse;
import com.zone5cloud.core.users.User;
import com.zone5cloud.retrofit.core.apis.UserAPI;
import com.zone5cloud.retrofit.core.utilities.Z5Utilities;

import retrofit2.Response;
import retrofit2.Retrofit;

public class TestOAuthAPI extends BaseTestRetrofit {
	private String email = "";
	private Long id = null;
	
	public void setup() throws IOException {
		LoginResponse response = login();
		if (response != null) {
			id = response.getUser().getId();
			email = response.getUser().getEmail();
		}
	}

	@Test
	public void testManualRefresh() throws IOException {
		setup();
		
		// Exercise the refresh access token
		if (isSpecialized() && clientConfig.getToken().getRefreshToken() == null) {
			// gigya
			OAuthToken alt = userApi.refreshToken().blockingFirst().body();
			assertNotNull(alt.getToken());
			assertNotNull(alt.getTokenExp());
			assertTrue(alt.getTokenExp() > System.currentTimeMillis() + 30000);
		} else if (clientConfig.getToken().getRefreshToken() != null){
			// cognito
			Response<OAuthToken> response = authApi.refreshAccessToken(clientConfig.getClientID(), clientConfig.getClientSecret(), email, GrantType.REFRESH_TOKEN, clientConfig.getToken().getRefreshToken()).blockingFirst();
			OAuthToken tok = response.body();
			assertNotNull(tok.getToken());
			assertNotNull(tok.getTokenExp());
			assertNotNull(tok.getRefreshToken());
			assertTrue(tok.getTokenExp() > System.currentTimeMillis() + 30000);
		} else {
			// legacy tp token with no refresh
			Response<OAuthToken> response = authApi.newAccessToken(clientConfig.getClientID(), clientConfig.getClientSecret(), email, GrantType.USERNAME_PASSWORD, TEST_PASSWORD).blockingFirst();
			OAuthToken tok = response.body();
			assertNotNull(tok.getToken());
			assertNotNull(tok.getTokenExp());
		}
		
		// check access after refresh
		User me = userApi.me().blockingFirst().body();
		assertEquals(me.getId(), id);
	}
	
	@Test
	public void testAutoRefresh() throws IOException {
		setup();
		
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

		if (clientConfig.getToken().getRefreshToken() != null) {
			assertNotEquals(currentToken.getToken(), newToken.getToken());
			assertEquals(currentToken.getRefreshToken(), newToken.getRefreshToken());
			assertTrue(newToken.getTokenExp() > System.currentTimeMillis() + 30000);
		}
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
		
		OkHttpClientInterceptor_Authorization interceptor = new OkHttpClientInterceptor_Authorization(clientConfig, delegate1, delegate2);
		
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
	public void testDelegateOrder() throws InterruptedException, ExecutionException {
		final ConcurrentHashMap<String, ConcurrentLinkedQueue<Long>> changes = new ConcurrentHashMap<>();
		final Semaphore semaphore = new Semaphore(-39);
		
		Z5AuthorizationDelegate delegate = new Z5AuthorizationDelegate() {
			
			@Override
			public void onAuthTokenUpdated(AuthToken token) {
				String[] sender = token.getToken().split(":");
				changes.putIfAbsent(sender[0], new ConcurrentLinkedQueue<Long>());
				changes.get(sender[0]).add(Long.decode(sender[1]));
				System.out.println(sender[0] + ":" + sender[1]);
				semaphore.release();
			}
		};

		final OkHttpClientInterceptor_Authorization interceptor = new OkHttpClientInterceptor_Authorization(clientConfig, delegate);
		
		
		class Run implements Callable<String> {
			private final String name;
			
			Run(String name) {
				this.name = name;
			}
			
			@Override
			public String call() {
				for (int i = 0; i < 10; i++) {
					OAuthToken token = new OAuthToken();
					token.setToken(name + ":" + i);
					interceptor.setToken(token);
				}
				return name;
			}
		}
		
		ExecutorService executor = Executors.newFixedThreadPool(5);
		Set<Run> tasks = new HashSet<>();
		tasks.add(new Run("a"));
		tasks.add(new Run("b"));
		tasks.add(new Run("c"));
		tasks.add(new Run("d"));
		
		executor.invokeAll(tasks);
		semaphore.acquire();
		
		
		for (Run r: tasks) {
			ConcurrentLinkedQueue<Long> list = changes.get(r.name);
			Long previous = -1l;
			for (Long l: list) {
				System.out.println(r.name + ": " + l);
				assertTrue(l > previous);
				previous = l;
			}
		}
	}
	
	@Test
	public void testAdhocToken() {
		login();
		
		// only applicable on SBC servers
		if (isSpecialized()) {
			Response<OAuthToken> response = authApi.adhocAccessToken("wahooride").blockingSingle();
			String errorString = "";
			if (!response.isSuccessful()) {
				Z5Error error = Z5Utilities.parseErrorResponse(response);
				errorString = error.getMessage();
			}
			assertTrue(errorString, response.isSuccessful());
			OAuthToken token = response.body();
			assertNotNull("Returned token should not be null", token);
			assertNotNull("Token should be valid", token.getToken());
			assertNotNull("Token should have an expiry", token.getExpiresIn());
			assertNotNull("Token should havea  scope", token.getScope());
		}
	}

	@Test
	public void testZone5Server_HasClientIdAndKey(){
		Response<User> rsp = userApi.me().blockingFirst();
		String clientId = rsp.raw().request().header("Api-Key");
		String clientSecret = rsp.raw().request().header("Api-Key-Secret");

		assertNotNull(clientId);
		assertNotNull(clientSecret);
	}

	@Test
	public void testNonZone5Server_HasNoClientIdAndKey() throws Exception {
		// configure with a bogus zone5baseUrl so that clientid and secret are not added
		ClientConfig config = new ClientConfig();
		config.setZone5BaseUrl(new URL("https://testserver.com.au"));
		config.setClientID("testclientid");
		config.setClientSecret("testsecret");
		Retrofit retrofit = buildRetrofit(config);
		UserAPI userApi = retrofit.create(UserAPI.class);

		Response<User> rsp = userApi.me().blockingFirst();
		String clientId = rsp.raw().request().header("Api-Key");
		String clientSecret = rsp.raw().request().header("Api-Key-Secret");

		assertNull(clientId);
		assertNull(clientSecret);
	}
}
