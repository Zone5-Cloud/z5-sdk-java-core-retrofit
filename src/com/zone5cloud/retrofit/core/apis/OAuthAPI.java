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

	/** Get a new auth token */
	@FormUrlEncoded
	@POST(Users.NEW_ACCESS_TOKEN)
	Observable<Response<OAuthToken>> refreshAccessToken(@Field("client_id") String clientId, @Field("client_secret") String secret, @Field("username") String username, @Field("grant_type") GrantType type, @Field("refresh_token") String refreshToken);
  
	/** Get a new auth token */
	@FormUrlEncoded
	@POST(Users.NEW_ACCESS_TOKEN)
	Observable<Response<OAuthToken>> newAccessToken(@Field("client_id") String clientId, @Field("client_secret") String secret, @Field("username") String username, @Field("grant_type") GrantType type, @Field("password") String password);

	/** Get a new adhoc auth token - this may be on behalf of another app */
	@GET(Users.NEW_ADHOC_ACCESS_TOKEN)
	Observable<Response<OAuthToken>> adhocAccessToken(@Path("clientId") String clientId);
}
