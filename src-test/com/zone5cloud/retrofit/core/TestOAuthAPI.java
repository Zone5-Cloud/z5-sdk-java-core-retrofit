package com.zone5cloud.retrofit.core;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
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
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.zone5cloud.core.ClientConfig;
import com.zone5cloud.core.Endpoints;
import com.zone5cloud.core.Z5AuthorizationDelegate;
import com.zone5cloud.core.Z5Error;
import com.zone5cloud.core.enums.GrantType;
import com.zone5cloud.core.oauth.AuthToken;
import com.zone5cloud.core.oauth.OAuthToken;
import com.zone5cloud.core.terms.Terms;
import com.zone5cloud.core.terms.TermsAndConditions;
import com.zone5cloud.core.users.LoginRequest;
import com.zone5cloud.core.users.LoginResponse;
import com.zone5cloud.core.users.NewPassword;
import com.zone5cloud.core.users.RefreshRequest;
import com.zone5cloud.core.users.RegisterUser;
import com.zone5cloud.core.users.TestPasswordRequest;
import com.zone5cloud.core.users.User;
import com.zone5cloud.core.users.UserPreferences;
import com.zone5cloud.core.users.Users;
import com.zone5cloud.core.utils.GsonManager;
import com.zone5cloud.retrofit.core.apis.OAuthAPI;
import com.zone5cloud.retrofit.core.apis.TermsAPI;
import com.zone5cloud.retrofit.core.apis.UserAPI;
import com.zone5cloud.retrofit.core.utilities.Z5Utilities;

import okhttp3.Interceptor.Chain;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response.Builder;
import okhttp3.ResponseBody;
import okhttp3.internal.http.RealResponseBody;
import okio.BufferedSource;
import okio.ByteString;
import retrofit2.Invocation;
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
		if (clientConfig.getToken().getRefreshToken() != null) {
			// refresh endpoint
			LoginResponse loginResponse = userApi.refreshToken(new RefreshRequest(email, clientConfig.getToken().getRefreshToken())).blockingFirst().body();
			OAuthToken token = loginResponse.getOAuthToken();
			assertNotNull(token.getToken());
			assertNotNull(token.getTokenExp());
			assertTrue(token.getTokenExp() > System.currentTimeMillis() + 30000);
		
			// oauth refresh
			Response<OAuthToken> response = authApi.refreshAccessToken(clientConfig.getClientID(), clientConfig.getClientSecret(), email, GrantType.REFRESH_TOKEN, clientConfig.getToken().getRefreshToken()).blockingFirst();
			if (clientConfig.getClientSecret() != null) {
				token = response.body();
				assertNotNull(token.getToken());
				assertNotNull(token.getTokenExp());
				assertNotNull(token.getRefreshToken());
				assertTrue(token.getTokenExp() > System.currentTimeMillis() + 30000);
			} else {
				// when no clientsecret is configured this will fail
				assertFalse(response.isSuccessful());
			}
		} 
		
		if (clientConfig.getClientSecret() != null) {
			Response<OAuthToken> response = authApi.newAccessToken(clientConfig.getClientID(), clientConfig.getClientSecret(), email, GrantType.USERNAME_PASSWORD, TEST_PASSWORD).blockingFirst();
			OAuthToken tok = response.body();
			assertNotNull(tok.getToken());
			assertNotNull(tok.getTokenExp());
			
			response = authApi.newAccessToken(clientConfig.getClientID(), clientConfig.getClientSecret(), email, GrantType.USERNAME_PASSWORD, TEST_PASSWORD, "https://localhost").blockingFirst();
			tok = response.body();
			assertNotNull(tok.getToken());
			assertNotNull(tok.getTokenExp());
		}
		
		// check access after refresh
		User me = userApi.me().blockingFirst().body();
		assertEquals(me.getId(), id);
	}
	
	@Test
	public void testRequiresRefresh() throws IOException {
		// this is a mocked unit test - it does not hit the server. See testAutoRefreshIntegration for integration test.
		ClientConfig config = new ClientConfig();
		config.setZone5BaseUrl(new URL("https://test.server.com"));
		config.setClientID("testclientid");
		OAuthToken token1 = new OAuthToken("firstToken", "refreshToken", 1L);
		config.setToken(token1);
		config.setUserName("initialusername");
		
		Z5AuthorizationDelegate delegate = Mockito.mock(Z5AuthorizationDelegate.class);
		OkHttpClientInterceptor_Authorization auth = new OkHttpClientInterceptor_Authorization(config, delegate);
		
		Chain mockChain = Mockito.mock(Chain.class);
		
		long expiresAt = System.currentTimeMillis() + 600000;
		Mockito.when(mockChain.proceed(Mockito.any(Request.class))).then(new Answer<okhttp3.Response>() {

			@Override
			public okhttp3.Response answer(InvocationOnMock invocation) throws Throwable {
				Request request = (Request)invocation.getArguments()[0];
				assertEquals("test.server.com", request.url().host());
				assertEquals(MediaType.get("application/json"), request.body().contentType());
				assertEquals(config.getClientID(), request.headers("Api-Key").get(0));
				
				LoginResponse loginResponse = new LoginResponse();
				loginResponse.setToken("secondToken");
				loginResponse.setRefresh("refreshToken");
				loginResponse.setTokenExp(expiresAt);
				TermsAndConditions terms = new TermsAndConditions();
				terms.setTermsId("testterms");
				terms.setAlias("has an alias");
				terms.setName("Test Terms");
				terms.setDisplayVersion("v1");
				terms.setCompanyId("acompany");
				terms.setVersion(2);
				loginResponse.setUpdatedTerms(Arrays.asList(terms));
				
				BufferedSource mockSource = Mockito.mock(BufferedSource.class);
				Mockito.when(mockSource.rangeEquals(0, ByteString.decodeHex("efbbbf"))).thenReturn(true);
				Mockito.when(mockSource.readString(Mockito.any(Charset.class))).thenReturn(GsonManager.getInstance().toJson(loginResponse, LoginResponse.class));
				ResponseBody mockBody = new RealResponseBody("application/json", 10, mockSource);
				
				Builder responseBuilder = new Builder().body(mockBody).protocol(Protocol.HTTP_1_1).code(200).message("OK").request(request);
				return responseBuilder.build();
			}
		});
		
		auth.refreshIfRequired(mockChain);
		
		Mockito.verify(mockChain).proceed(Mockito.any(Request.class));
		
		assertEquals("secondToken", auth.getToken().getToken());
		assertEquals(expiresAt, auth.getToken().getTokenExp().longValue());
		
		// subsequent call should not go through because token is no longer expired
		auth.refreshIfRequired(mockChain);
		Mockito.verifyNoMoreInteractions(mockChain);
		
		Mockito.verify(delegate, Mockito.times(1)).onAuthTokenUpdated(auth.getToken());
		Mockito.verify(delegate, Mockito.times(1)).onTermsUpdated(Mockito.anyList());
	}
	
	private Request createRequest(Class<?> cls, String method, Object ...params) throws NoSuchMethodException, SecurityException {
		Class<?>[] types = new Class<?>[params.length]; 
		for(int i = 0; i < params.length; i++) {
			types[i] = params[i].getClass();
			if (types[i] == Long.class) types[i] = long.class;
		}
		Request.Builder builder = new Request.Builder().url("https://" + method).tag(Invocation.class, Invocation.of(cls.getMethod(method, types), Arrays.asList(params)));
		return builder.build();
	}
	
	@Test
	public void testRequiresAuth() throws NoSuchMethodException, SecurityException {
		auth.setToken(new OAuthToken("testtoken", "testrefresh", System.currentTimeMillis() + 60000));
		assertTrue(auth.requiresAuth(createRequest(TermsAPI.class, "accept", "termsId")) && Endpoints.requiresAuth(Terms.ACCEPT));
		assertFalse(auth.requiresAuth(createRequest(TermsAPI.class, "required")) || Endpoints.requiresAuth(Terms.REQUIRED));
		assertFalse(auth.requiresAuth(createRequest(TermsAPI.class, "download", "testTerms")) || Endpoints.requiresAuth(Terms.DOWNLOAD));
		
		assertFalse(auth.requiresAuth(createRequest(OAuthAPI.class, "refreshAccessToken", "client", "secret", "user", GrantType.REFRESH_TOKEN, "refresh")) || Endpoints.requiresAuth(Users.NEW_ACCESS_TOKEN));
		assertFalse(auth.requiresAuth(createRequest(OAuthAPI.class, "newAccessToken", "client", "secret", "user", GrantType.USERNAME_PASSWORD, "password")));
		assertTrue(auth.requiresAuth(createRequest(OAuthAPI.class, "adhocAccessToken", "client")) && Endpoints.requiresAuth(Users.NEW_ADHOC_ACCESS_TOKEN));
		
		assertTrue(auth.requiresAuth(createRequest(UserAPI.class, "getPreferences", 1234L)) && Endpoints.requiresAuth(Users.GET_USER_PREFERENCES));
		assertTrue(auth.requiresAuth(createRequest(UserAPI.class, "setPreferences", new UserPreferences())) && Endpoints.requiresAuth(Users.SET_USER_PREFERENCES));
		assertTrue(auth.requiresAuth(createRequest(UserAPI.class, "register", new RegisterUser())) && Endpoints.requiresAuth(Users.REGISTER_USER));
		assertTrue(auth.requiresAuth(createRequest(UserAPI.class, "delete", 1234l)) && Endpoints.requiresAuth(Users.DELETE_USER));
		assertTrue(auth.requiresAuth(createRequest(UserAPI.class, "logout")) && Endpoints.requiresAuth(Users.LOGOUT));
		assertTrue(auth.requiresAuth(createRequest(UserAPI.class, "setPassword", new NewPassword())) && Endpoints.requiresAuth(Users.SET_PASSWORD));
		assertTrue(auth.requiresAuth(createRequest(UserAPI.class, "updateUser", new User())) && Endpoints.requiresAuth(Users.SET_USER));
		
		assertFalse(auth.requiresAuth(createRequest(UserAPI.class, "login", new LoginRequest())) || Endpoints.requiresAuth(Users.LOGIN));
		assertFalse(auth.requiresAuth(createRequest(UserAPI.class, "isEmailRegistered", "email")) || Endpoints.requiresAuth(Users.EMAIL_EXISTS));
		assertFalse(auth.requiresAuth(createRequest(UserAPI.class, "getEmailVerification", "email")) || Endpoints.requiresAuth(Users.EMAIL_STATUS));
		assertFalse(auth.requiresAuth(createRequest(UserAPI.class, "resetPassword", "email")) || Endpoints.requiresAuth(Users.PASSWORD_RESET));
		assertFalse(auth.requiresAuth(createRequest(UserAPI.class, "refreshToken", new RefreshRequest())) || Endpoints.requiresAuth(Users.REFRESH_TOKEN));
		assertFalse(auth.requiresAuth(createRequest(UserAPI.class, "passwordComplexity")) || Endpoints.requiresAuth(Users.PASSWORD_COMPLEXITY));
		assertFalse(auth.requiresAuth(createRequest(UserAPI.class, "reconfirm", "email")) || Endpoints.requiresAuth(Users.RECONFIRM));
		assertFalse(auth.requiresAuth(createRequest(UserAPI.class, "testPassword", new TestPasswordRequest())) || Endpoints.requiresAuth(Users.TEST_PASSWORD));
		
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
	public void testAutoRefreshIntegrationFailure() throws IOException {
		setup();
		
		AuthToken currentToken = auth.getToken();

		// expire the token to force the refresh sequence
		OAuthToken expiredToken = new OAuthToken();
		expiredToken.setToken(currentToken.getToken());
		expiredToken.setRefreshToken(currentToken.getRefreshToken());
		expiredToken.setTokenExp(System.currentTimeMillis());
		auth.setToken(expiredToken);
		auth.setClientIDAndSecret(null, null);
		
		User me = userApi.me().blockingFirst().body();
		assertEquals(me.getId(), id);
		
		// should be unchanged because the refresh should have failed
		assertEquals(currentToken.getToken(), auth.getToken().getToken());
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
			
			@Override
			public void onTermsUpdated(List<TermsAndConditions> updatedTerms) {
				
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
			
			@Override
			public void onTermsUpdated(List<TermsAndConditions> updatedTerms) {
				
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
			
			@Override
			public void onTermsUpdated(List<TermsAndConditions> updatedTerms) {
				
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
		assertEquals(this.clientConfig.getClientID(), clientId);
		assertEquals(this.clientConfig.getClientSecret(), clientSecret);
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
