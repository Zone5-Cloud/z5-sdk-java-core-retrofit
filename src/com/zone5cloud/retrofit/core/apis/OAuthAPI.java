package com.zone5cloud.retrofit.core.apis;

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
	 * <p>clientId and secret are optional if and only if the OkHttpClientInterceptor_Authorization has been configured with a clientId and secret.
	 * grant_type must be "refresh_token"
	 * username and refreshToken are required fields, their absence will result in a 401 from the server.</p>
	 * 
	 * <p>note that refresh is automatically applied by the OkHttpClientInterceptor_Authorization so explicitly calling
	 * this endpoint is not necessary in normal flow.</p>
	 * 
	 * <p>If you want to pass terms acceptance or receive terms updated information then use UserAPI.refreshToken instead.</p>
	 * 
	 * @param clientId - not required if OkHttpClientInterceptor_Authorization has been configured with a clientId. Required otherwise.
	 * @param secret - not required if OkHttpClientInterceptor_Authorization has been configured with a secret. Required otherwise.
	 * @param username - username (email) of user requesting token refresh. required.
	 * @param type - must be "refresh_token"
	 * @param refreshToken - valid refresh token for the user
	 */
	@FormUrlEncoded
	@POST(Users.NEW_ACCESS_TOKEN)
	Observable<Response<OAuthToken>> refreshAccessToken(@Field("client_id") String clientId, @Field("client_secret") String secret, @Field("username") String username, @Field("grant_type") GrantType type, @Field("refresh_token") String refreshToken);
  
	/**
	 *  Get a new auth token 
	 *  
	 *  clientId and secret are optional if and only if the OkHttpClientInterceptor_Authorization has been configured with a clientId and secret.
	 *	grant_type must be "password"
	 *	username and password are required fields
	 **/
	@FormUrlEncoded
	@POST(Users.NEW_ACCESS_TOKEN)
	Observable<Response<OAuthToken>> newAccessToken(@Field("client_id") String clientId, @Field("client_secret") String secret, @Field("username") String username, @Field("grant_type") GrantType type, @Field("password") String password);

	/** Get a new adhoc auth token - this may be on behalf of another app */
	@GET(Users.NEW_ADHOC_ACCESS_TOKEN)
	Observable<Response<OAuthToken>> adhocAccessToken(@Path("clientId") String clientId);
}
