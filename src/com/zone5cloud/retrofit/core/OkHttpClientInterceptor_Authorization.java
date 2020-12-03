package com.zone5cloud.retrofit.core;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import com.zone5cloud.core.Endpoints;
import com.zone5cloud.core.Types;
import com.zone5cloud.core.Z5AuthorizationDelegate;
import com.zone5cloud.core.enums.GrantType;
import com.zone5cloud.core.enums.Z5HttpHeader;
import com.zone5cloud.core.oauth.AuthToken;
import com.zone5cloud.core.oauth.OAuthToken;
import com.zone5cloud.core.users.LoginResponse;
import com.zone5cloud.core.users.Users;
import com.zone5cloud.core.utils.GsonManager;

import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * OKHttp Interceptor which:
 *  * Does a refresh if the authorization token is expired or nearing expiry
 *  * Sets the Authorization header if required
 *  * Sets the API-Key and API-Key-Secret headers if applicable
 *  
 *  Pass in a delegate if you want to be notified of updates to the auth token,
 *  for instance if you wish to save the token to persistent storage.
 *  
 * @author jean
 *
 */
public class OkHttpClientInterceptor_Authorization implements Interceptor {
	private final AtomicReference<AuthToken> token = new AtomicReference<>(null);
	private String clientID;
	private String clientSecret;
	protected final Set<Z5AuthorizationDelegate> delegates = new HashSet<>();
	private final Object setTokenLock = new Object();
	private final Object fetchTokenLock = new Object();
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
	 * @param token - the initial authorization token to be used in the Authorization header for all requests which require it. 
	 * token may be null and can be set later via setToken(), or automatically set with a login request
	 * @param clientID - Authentication service API key.
	 * @param clientSecret - Authentication service API secret, Cognito only. Set to null for Gigya keys.
	 * @param delegates - delegates to receive callbacks whenever the token is updated.
	 */
	public OkHttpClientInterceptor_Authorization(AuthToken token, String clientID, String clientSecret, Z5AuthorizationDelegate ...delegates) {
		this.token.set(token);
		this.clientID = clientID;
		this.clientSecret = clientSecret;
		
		for (Z5AuthorizationDelegate delegate: delegates) {
			this.delegates.add(delegate);
		}
	}
	
	/** Set the Authentication service API key and secret. Set clientSecret to null for Gigya keys */
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
				delegateExecutor.execute(new Runnable() {
					@Override
					public void run() {
						for(Z5AuthorizationDelegate delegate: delegates) {
							delegate.onAuthTokenUpdated(token);
						}
					}
				});
			}
		}
	}
	
	/** Get the Auth token */
	public AuthToken getToken() {
		return this.token.get();
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
		Request.Builder builder = originalRequest.newBuilder();
		
		AuthToken token = this.token.get();
		if (token != null && token.getBearer() != null && Endpoints.requiresAuth(path)) {
			refreshIfRequired(chain, getBaseUrl(originalRequest.url()));
			
			// refetch token after potential refresh
			token = this.token.get();
			if (token != null && token.getBearer() != null) {
				builder = builder.header(Z5HttpHeader.AUTHORIZATION.toString(), token.getBearer());
			}
		}
		
		// APIKey headers go on unauthenticated requests too
		String clientID = this.clientID;
		if (clientID != null) {
			builder = builder.header(Z5HttpHeader.API_KEY.toString(), clientID);
		}
		
		String clientSecret = this.clientSecret;
		if (clientSecret != null) {
			builder = builder.header(Z5HttpHeader.API_KEY_SECRET.toString(), clientSecret);
		}
		
        Request newRequest = builder.build();
        Response response = chain.proceed(newRequest);
        
        String newTokenBody = saveNewToken(path, response);
        
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
	 * @param baseUrl - The base hose url from the request, used to issue the refresh request
	 **/
	private void refreshIfRequired(Chain chain, String baseUrl) {
		// check the token for expiry
		// note that refresh itself does not require auth so will not end up cyclicly back here
		AuthToken token = this.token.get();
		if (token != null && token.getRefreshToken() != null && token.isExpired() && chain != null && baseUrl != null && !baseUrl.isEmpty()) {
			// because requests can be concurrent, we need to synchronize so that we only do one refresh for an expired token
			// and all of the requests dependent on that refresh are queued until the token is refreshed
			synchronized(fetchTokenLock) {
				// once we are inside the mutex block we need to re-test our token because it might have
				// been refreshed while we were waiting
				token = this.token.get();
				if (token != null && token.isExpired()) {
					try {
						if (token.getRefreshToken() != null) {
							// do a Cognito refresh
							String username = token.extractUsername();
							if (username != null) {
								RequestBody body = new FormBody.Builder().add("client_id", clientID)
																		 .add("client_secret", clientSecret)
																		 .add("grant_type", GrantType.REFRESH_TOKEN.toString())
																		 .add("username", username)
																		 .add("refresh_token", token.getRefreshToken()).build();
								String url = baseUrl + Users.NEW_ACCESS_TOKEN;
								Request authRequest = new Request.Builder().url(url).post(body).build();
								Response response = chain.proceed(authRequest);
								saveNewToken(Users.NEW_ACCESS_TOKEN, response);
								response.close();
							}
						} else {
							// do a Gigya refresh
							// note: I put a check for getRefreshToken() into the entry if block
							// which will exclude this case from being hit. If we want to support Gigya token refresh
							// remove the getRefreshToken() != null check from the entry if statement at the start of this method
							String url = baseUrl + Users.REFRESH_TOKEN;
							Request authRequest = new Request.Builder().url(url).header(Z5HttpHeader.AUTHORIZATION.toString(), token.getBearer()).get().build();
							Response response = chain.proceed(authRequest);
							saveNewToken(Users.REFRESH_TOKEN, response);
							response.close();
						}
						
					} catch(Exception e) {
						// could not refresh. Continue as normal and the caller should receive their own 401 response
					}
				}
			}
		}
	}
	
	/** 
	 * Capture any returned new tokens and save 
	 * @param path - The request path, used to determine response type
	 * @param response - The request's response that we need to parse auth token from if applicable
	 **/
	private String saveNewToken(String path, Response response) {
		String body = null;
		// capture token
		try {
	        if (response.isSuccessful()) {
	        	switch(path) {
		        	case Users.LOGIN:
		        		body = response.body().string();
		        		LoginResponse login = GsonManager.getInstance().fromJson(body, Types.LOGIN_RESPONSE);
		        		if (login != null) {
		        			if (login.getExpiresIn() != null) {
		        				login.setTokenExp(System.currentTimeMillis() + (login.getExpiresIn() * 1000));
		        			}
		        			setToken(new OAuthToken(login));
		        			body = GsonManager.getInstance().toJson(login, Types.LOGIN_RESPONSE);
		        		}
		        		return body;
		        	case Users.REFRESH_TOKEN:
		        		body = response.body().string();
		        		OAuthToken alt = GsonManager.getInstance().fromJson(body, Types.OAUTHTOKEN);
		        		if (alt != null) {
		        			setToken(alt);
		        			body = GsonManager.getInstance().toJson(alt, Types.OAUTHTOKEN);
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
		        			body = GsonManager.getInstance().toJson(newToken, Types.OAUTHTOKEN);
		        		}
		        		return body;
		        	case Users.LOGOUT:
		        		setToken((AuthToken)null);
		        		break;
	        		default:
	        			break;
	        	}
	        }
		} catch(Exception e) {
			// could not decode new token
		}
		
		// no change
		return body;
	}
}
