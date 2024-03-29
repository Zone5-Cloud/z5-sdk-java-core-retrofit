package com.zone5cloud.retrofit.core;

import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import com.zone5cloud.core.ClientConfig;
import com.zone5cloud.core.Endpoints;
import com.zone5cloud.core.Types;
import com.zone5cloud.core.Z5AuthorizationDelegate;
import com.zone5cloud.core.Z5Error;
import com.zone5cloud.core.Z5ErrorItem;
import com.zone5cloud.core.annotations.Unauthenticated;
import com.zone5cloud.core.enums.Z5HttpHeader;
import com.zone5cloud.core.oauth.AuthToken;
import com.zone5cloud.core.oauth.OAuthToken;
import com.zone5cloud.core.terms.TermsAndConditions;
import com.zone5cloud.core.users.LoginRequest;
import com.zone5cloud.core.users.LoginResponse;
import com.zone5cloud.core.users.RefreshRequest;
import com.zone5cloud.core.users.Users;
import com.zone5cloud.core.utils.DefaultLogger;
import com.zone5cloud.core.utils.GsonManager;
import com.zone5cloud.core.utils.ILogger;

import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import retrofit2.Invocation;

/**
 * OKHttp Interceptor which:
 *  * Does a refresh if the authorization token is expired or nearing expiry
 *  * Sets the Authorization header if required
 *  * Sets the API-Key and API-Key-Secret headers if applicable
 *  
 *  Pass in a delegate if you want to be notified of updates to the auth token,
 *  for instance if you wish to save the token to persistent storage.
 *
 *  Please Note :
 *  This Retrofit instance needs these two ConverterFactories to work correctly.
          * ScalarsConverterFactory
          * GsonConverterFactory
 *
 * @author jean
 *
 */
public class OkHttpClientInterceptor_Authorization implements Interceptor {
	private final AtomicReference<AuthToken> token = new AtomicReference<>(null);
	private String clientID;
	private String clientSecret;
	private String userName;
	private final URL zone5BaseUrl;
	private final ILogger log;

	protected final Set<Z5AuthorizationDelegate> delegates = new HashSet<>();
	private final Object setTokenLock = new Object();
	private final Object refreshLock = new Object();
	// create a thread pool to handle delegate callbacks. It needs to be single threaded so that changes are reported sequentially in order.
	private final ExecutorService delegateExecutor = Executors.newSingleThreadExecutor();

	/**
	 * OKHttp Interceptor which:
	 *  * Does a refresh if the authorization token is expired or nearing expiry
	 *  * Sets the Authorization header if required
	 *  * Sets the API-Key and API-Key-Secret headers if applicable
	 *  
	 *  Pass in a delegate if you want to be notified of updates to the auth token,
	 *  for instance if you wish to save the token to persistent storage.
	 *
	 * @param clientConfig (gets and sets token, client key , secret and username
	 * @param delegates - delegates to receive callbacks whenever the token is updated.
	 */
	public OkHttpClientInterceptor_Authorization(ClientConfig clientConfig, Z5AuthorizationDelegate ...delegates) {
		if (clientConfig == null) {
			throw new IllegalArgumentException("Missing configuration");
		}
		
		this.token.set(clientConfig.getToken());
		this.clientID = clientConfig.getClientID();
		this.clientSecret = clientConfig.getClientSecret();
		this.zone5BaseUrl = clientConfig.getZone5BaseUrl();
		this.userName = clientConfig.getUserName();
		
		if (delegates != null) {
			for (Z5AuthorizationDelegate delegate: delegates) {
				this.delegates.add(delegate);
			}
		}
		
		this.log = clientConfig.getLogger() != null ? clientConfig.getLogger() : new DefaultLogger();
	}

	public ClientConfig getConfig() {
		ClientConfig config = new ClientConfig();
		config.setClientID(this.clientID);
		config.setClientSecret(this.clientSecret);
		config.setToken(this.getToken());
		config.setUserName(this.userName);
		config.setZone5BaseUrl(this.zone5BaseUrl);
		config.setLogger(this.log);
		
		return config;
	}
	
	/** Set the Authentication service API key and optional secret. */
	public void setClientIDAndSecret(String clientID, String clientSecret) {
		this.clientID = clientID;
		this.clientSecret = clientSecret;
	}
	
	/** Set the Auth token */
	public void setToken(final AuthToken token) {
		// This is a safe synchronized block with no blocking calls or long running calls. 
		// It tests the token for equality and schedules tasks onto an executor and returns immediately.
		// The synchronized ensures that token changes are reported on the delegate serially and in order.
		synchronized(setTokenLock) {
			AuthToken previousValue = this.token.getAndSet(token);
			// only call delegates if the value has changed
			if ((token == null && previousValue != null) || (token != null && !token.equals(previousValue))) {
				// this only schedules the execution. This call returns immediately and exits the lock. 
				// Delegates are executed asynchronously but serially, in order.
				delegateExecutor.execute(() -> {
					for(Z5AuthorizationDelegate delegate: delegates) {
						delegate.onAuthTokenUpdated(token);
					}
				});
			}
		}
	}
	
	/** Get the Auth token */
	public AuthToken getToken() {
		return this.token.get();
	}
	
	public void setUserName(String userName){
		this.userName = userName;
	}
	
	public String getUserName() {
		return this.userName;
	}
	
	/** Fire delegate callbacks for updated terms */
	private void updatedTerms(List<TermsAndConditions> updatedTerms) {
		if (updatedTerms == null || updatedTerms.isEmpty()) {
			// don't fire for empty terms
			return;
		}
		
		delegateExecutor.execute(() -> {
			for(Z5AuthorizationDelegate delegate: delegates) {
				delegate.onTermsUpdated(updatedTerms);
			}
		});
	}

	public void subscribe(Z5AuthorizationDelegate delegate) {
		delegates.add(delegate);
	}
	
	public void unsubscribe(Z5AuthorizationDelegate delegate) {
		delegates.remove(delegate);
	}
	
	/** Construct a host base url from the given request url e.g. "https://todaysplan.com.au:80 */
	private String getBaseUrl(HttpUrl url) {
		if (url == null) {
			return "";
		}
		
		return url.scheme() + "://" + url.host() + ":" + url.port();
	}
	
	private Request.Builder addAPIHeaders(Request.Builder builder) {
		// APIKey headers go on all requests
		String cID = this.clientID;
		if (cID != null) {
			builder = builder.header(Z5HttpHeader.API_KEY.toString(), cID);
		}

		String secret = this.clientSecret;
		if (secret != null) {
			builder = builder.header(Z5HttpHeader.API_KEY_SECRET.toString(), secret);
		}
		
		return builder;
	}
	
	/**
	 * Intercept the http request.
	 * * Refresh the auth token if it is expired or near expiry
	 * * Add the auth token to the Authorization header
	 * * Add the client ID and client secret headers
	 */
	@Override
	public Response intercept(Chain chain) throws IOException {
		Request originalRequest = chain.request();
		String path = originalRequest.url().encodedPath();
		String originalRequestUrl =  getBaseUrl(originalRequest.url());
		Request.Builder builder = originalRequest.newBuilder();
				
		if (requiresAuth(originalRequest)) {

			refreshIfRequired(chain);

			// fetch token after potential refresh
			AuthToken authToken = this.token.get();
			if (authToken != null && authToken.getBearer() != null) {
				builder = builder.header(Z5HttpHeader.AUTHORIZATION.toString(), authToken.getBearer());
			}
		}
		// add the key and secret only if it is zone5 server url
		if(this.zone5BaseUrl != null && originalRequestUrl.contains(
				this.zone5BaseUrl.toString())) {
			builder = addAPIHeaders(builder);
		}
		
        Request newRequest = builder.build();
        Response response = chain.proceed(newRequest);
        
        String newTokenBody = saveNewToken(path, newRequest, response);
        
        if (newTokenBody != null) {
        	// the interceptor has changed the token body and we need to wrap this as response
        	Response.Builder newResponse = response.newBuilder();
            newResponse.body(ResponseBody.create(MediaType.parse("application/json"), newTokenBody));
            return newResponse.build();
        }
        
        
        return response;
	}
	
	/** 
	 * If we have a refreshable token and it is expired or near expiry, do a token refresh 
	 * @param chain - The intercepted chain
	 * @param zone5Url - The base hose url from the request, used to issue the refresh request
	 **/
	protected void refreshIfRequired(Chain chain) {
		URL zone5Url = this.zone5BaseUrl;
		
		// check the token for expiry
		// note that refresh itself does not require auth so will not end up cyclically back here
		AuthToken authToken = this.token.get();
		if (authToken != null && authToken.requiresRefresh()) {
			// because requests can be concurrent, we need to synchronize so that we only do one refresh for an expired token
			// and all of the requests dependent on that refresh are queued until the token is refreshed
			synchronized(refreshLock) {
				// once we are inside the mutex block we need to re-test our token because it might have
				// been refreshed while we were waiting
				authToken = this.token.get();
				if (authToken != null && authToken.requiresRefresh()) {
					
					try {
						if (chain == null || zone5Url == null) {
							throw new IOException("Could not send to host. " + (chain == null ? "Retrofit chain is null, " : "") + "Zone5 URL is " + (zone5Url == null ? "null" : zone5Url));
						}
						
						RefreshRequest refreshRequest = new RefreshRequest(this.userName, authToken.getRefreshToken(), this.clientID, this.clientSecret);
						byte[] data = GsonManager.getInstance().toJson(refreshRequest, Types.REFRESH_REQUEST).getBytes();
						this.log.v(this.getClass().getSimpleName(), "Performing cognito refresh");
						RequestBody body = RequestBody.create(MediaType.get("application/json"), data);
						
						// create a refresh request
						String url = zone5Url + Users.REFRESH_TOKEN;
						Request.Builder builder = new Request.Builder().url(url).post(body);
						addAPIHeaders(builder);
						
						Request authRequest = builder.build();
						Response response = chain.proceed(authRequest);
						
						// save the refreshed token, now we can continue with the original request
						saveNewToken(Users.REFRESH_TOKEN, authRequest, response);

						response.close();
					} catch(Exception e) {
						// could not refresh. Continue as normal and the caller should receive their own 401 response
						this.log.e(this.getClass().getSimpleName(), "refresh request failed to send", e);
					}
				}
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private <T> T decodeRequestBody(Request request, Class<T> decodeTo) {
		try {
			if(request != null && request.body() != null) {
				if (FormBody.class.equals(decodeTo)) {
					if (request.body() instanceof FormBody) {
						return (T)request.body();
					} else {
						return null;
					}
				}
				else {
					final Buffer buffer = new Buffer();
					request.body().writeTo(buffer);
					String body = buffer.readUtf8();
					return GsonManager.getInstance().fromJson(body, decodeTo);
				}
			}
		}
		catch (final IOException e) {
			log.e(this.getClass().getSimpleName(), "Failed to decode request to ascertain username", e);
		}

		return null;
	}
	
	/** 
	 * Capture any returned new tokens and save 
	 * @param path - The request path, used to determine response type
	 * @param response - The request's response that we need to parse auth token from if applicable
	 **/
	@SuppressWarnings("squid:S3776") // cognitive complexity
	private String saveNewToken(String path, Request request, Response response) {
		String body = null;
		// capture token
		try {
			if (response.isSuccessful()) {
				switch(path) {
					case Users.REFRESH_TOKEN:
					case Users.LOGIN:
						body = response.body().string();
						LoginResponse login = GsonManager.getInstance().fromJson(body, Types.LOGIN_RESPONSE);
						if (login != null) {
							if (login.getExpiresIn() != null) {
								login.setTokenExp(System.currentTimeMillis() + (login.getExpiresIn() * 1000));
							}
							
							// set the username
							if(login.getUser() != null && login.getUser().getEmail() != null){
								// use username out of response where possible
								setUserName(login.getUser().getEmail());
							} else {
								// if not in response, try the username extracted from the request
								LoginRequest authRequest = decodeRequestBody(request, LoginRequest.class);
								if (authRequest != null && authRequest.getUsername() != null) {
									setUserName(authRequest.getUsername());
								}
							}
							
							setToken(new OAuthToken(login));
							updatedTerms(login.getUpdatedTerms());
							
							body = GsonManager.getInstance().toJson(login, Types.LOGIN_RESPONSE);
						}
						return body;
					case Users.NEW_ACCESS_TOKEN:
						body = response.body().string();
						OAuthToken newToken = GsonManager.getInstance().fromJson(body, Types.OAUTHTOKEN);
						if (newToken != null) {
							if (newToken.getExpiresIn() != null) {
								// calculate expiry based on system clock and expiresIn seconds
								newToken.setTokenExp(System.currentTimeMillis() + (newToken.getExpiresIn() * 1000));
							}
							setToken(newToken);
							
							// attempt to extract the username from the request so that we can update it in the config
							FormBody accessTokenRequest = decodeRequestBody(request, FormBody.class);
							if (accessTokenRequest != null) {
								for (int i = 0; i < accessTokenRequest.size(); i++) {
									String name = accessTokenRequest.encodedName(i);
									if ("username".equals(name)) {
										String username = accessTokenRequest.value(i);
										if (username != null) {
											setUserName(username);
											break;
										}
									}
								}
							}
							
							body = GsonManager.getInstance().toJson(newToken, Types.OAUTHTOKEN);
						}
						return body;
					case Users.LOGOUT:
						setToken((AuthToken)null);
						setUserName(null);
						break;
					default:
						break;
				}
			} else {
				return logHttpError(path, response);
			}
		} catch(Exception e) {
			// could not decode
			log.e(this.getClass().getSimpleName(), "Failed to decode http " + path + " response", e);
		}

		// no change
		return body;
	}
	
	private String logHttpError(String path, Response response) {
		String body = null;
		String message = response.message();

		if (response.body() != null) {
			try {
				body = response.body().string();
				Z5Error error = GsonManager.getInstance().fromJson(body, Types.ERROR);
				StringBuilder builder = new StringBuilder(error.getMessage());
				if (error.getErrors() != null) {
					for (Z5ErrorItem item: error.getErrors()) {
						builder.append(". " + item.getCode() + ": " + item.getMessage() + " (" + item.getField() + ")");
					}
				}
				message = builder.toString();
			}
			catch(Exception e) {
				// could not derive more detailed information from error message. Using response.message();
			}
		}

		log.httpError(this.getClass().getSimpleName(), path, response.code(), message);
		if (Users.NEW_ACCESS_TOKEN.equals(path) || Users.REFRESH_TOKEN.equals(path)) {
			log.refreshError(this.getClass().getSimpleName(), path, response.code(), message);
		}
		
		return body;
	}
	
	protected boolean requiresAuth(Request request) {
		AuthToken authToken = this.token.get();
		
		if (request == null || authToken == null || authToken.getToken() == null) {
			// can't authenticate even if we wanted to
			return false;
		}
		
		Invocation i = request.tag(Invocation.class);
		if (i != null && i.method() != null) {
			return !i.method().isAnnotationPresent(Unauthenticated.class);
		}
		
		// fallback
		return Endpoints.requiresAuth(request.url().encodedPath());
	}
}
