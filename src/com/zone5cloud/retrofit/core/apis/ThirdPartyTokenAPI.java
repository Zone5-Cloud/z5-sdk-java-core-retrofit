package com.zone5cloud.retrofit.core.apis;

import com.zone5cloud.core.enums.UserConnectionsType;
import com.zone5cloud.core.thirdpartyconnections.ThirdPartyToken;
import com.zone5cloud.core.thirdpartyconnections.ThirdPartyTokenResponse;
import com.zone5cloud.core.users.Users;

import io.reactivex.Observable;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;


public interface ThirdPartyTokenAPI {
	
	/** Set a 3rd party OAuth token - returns a success: true|false */
    @POST(Users.SET_THIRD_PARTY_CONNECTION)
    Observable<Response<ThirdPartyTokenResponse>> setThirdPartyToken(@Body ThirdPartyToken token, @Query("service_name") UserConnectionsType type);

    /** Delete a 3rd party OAuth token - returns a success: true|false */
    @POST(Users.REM_THIRD_PARTY_CONNECTION)
    Observable<Response<ThirdPartyTokenResponse>> removeThirdPartyToken(@Query("service_name") UserConnectionsType type);
    
    /** Returns details of the current 3rd party token (if any) - returns a available: true|false, token: {...} */
    @GET(Users.HAS_THIRD_PARTY_CONNECTION)
    Observable<Response<ThirdPartyTokenResponse>> hasThirdPartyToken(@Query("service_name") UserConnectionsType type);

}
