package com.zone5cloud.retrofit.core.apis;

import com.zone5cloud.core.annotations.Unauthenticated;
import com.zone5cloud.core.enums.GrantType;
import com.zone5cloud.core.oauth.OAuthToken;
import com.zone5cloud.core.users.Users;

import io.reactivex.Observable;
import retrofit2.Response;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface OAuthAPI {

	/** 
	 * Get a new auth token
	 *  
	 * <p>note that refresh is automatically applied by the OkHttpClientInterceptor_Authorization so explicitly calling
	 * this endpoint is not necessary in normal flow.</p>
	 * 
	 * <p>If you want to pass terms acceptance or receive terms updated information then use UserAPI.refreshToken instead.</p>
	 * 
	 * @param clientId - not required if OkHttpClientInterceptor_Authorization has been configured with a clientId. Required otherwise.
	 * @param secret - not required if OkHttpClientInterceptor_Authorization has been configured with a secret. Required otherwise.
	 * @param username - username (email) of user requesting token refresh. required.
	 * @param type - must be "refresh_token".
	 * @param refreshToken - valid refresh token for the user. required.
	 */
	@FormUrlEncoded
	@POST(Users.NEW_ACCESS_TOKEN)
	@Unauthenticated
	Observable<Response<OAuthToken>> refreshAccessToken(@Field("client_id") String clientId, @Field("client_secret") String secret, @Field("username") String username, @Field("grant_type") GrantType type, @Field("refresh_token") String refreshToken);
  
	/**
	 *  <p>Get a new auth token</p> 
	 *  
	 *  @param clientId - not required if OkHttpClientInterceptor_Authorization has been configured with a clientId. Required otherwise.
	 *  @param secret - not required if OkHttpClientInterceptor_Authorization has been configured with a secret. Required otherwise.
	 *  @param username - username (email) of user requesting token refresh. required.
	 *  @param type - must be "password".
	 *  @param password - required.
	 *  
	 *  <p>UserAPI.login provides an alternate way to authenticate with the server, with the ability to pass terms acceptance.</p>
	 **/
	@FormUrlEncoded
	@Unauthenticated
	@POST(Users.NEW_ACCESS_TOKEN)
	Observable<Response<OAuthToken>> newAccessToken(@Field("client_id") String clientId, @Field("client_secret") String secret, @Field("username") String username, @Field("grant_type") GrantType type, @Field("password") String password, @Field("redirect_uri") String redirect_uri);

	/**
	 *  <p>Get a new auth token</p> 
	 *  
	 *  @param clientId - not required if OkHttpClientInterceptor_Authorization has been configured with a clientId. Required otherwise.
	 *  @param secret - not required if OkHttpClientInterceptor_Authorization has been configured with a secret. Required otherwise.
	 *  @param username - username (email) of user requesting token refresh. required.
	 *  @param type - must be "password".
	 *  @param password - required.
	 *  
	 *  <p>UserAPI.login provides an alternate way to authenticate with the server, with the ability to pass terms acceptance.</p>
	 **/
	@FormUrlEncoded
	@Unauthenticated
	@POST(Users.NEW_ACCESS_TOKEN)
	Observable<Response<OAuthToken>> newAccessToken(@Field("client_id") String clientId, @Field("client_secret") String secret, @Field("username") String username, @Field("grant_type") GrantType type, @Field("password") String password);

	/** 
	 * Get a new adhoc auth token on behalf of another app 
	 * @param clientId - the clientId of the other app
	 **/
	@GET(Users.NEW_ADHOC_ACCESS_TOKEN)
	Observable<Response<OAuthToken>> adhocAccessToken(@Path("clientId") String clientId);
}
