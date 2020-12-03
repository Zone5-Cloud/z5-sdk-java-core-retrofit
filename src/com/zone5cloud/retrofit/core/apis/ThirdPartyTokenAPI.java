package com.zone5cloud.retrofit.core.apis;

import com.zone5cloud.core.enums.UserConnectionsType;
import com.zone5cloud.core.thirdpartyconnections.PushRegistration;
import com.zone5cloud.core.thirdpartyconnections.PushRegistrationResponse;
import com.zone5cloud.core.thirdpartyconnections.ThirdPartyToken;
import com.zone5cloud.core.thirdpartyconnections.ThirdPartyTokenResponse;
import com.zone5cloud.core.users.Users;

import io.reactivex.Observable;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
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
    
    /** 
	 * Register a push token for a device with a 3rd party
	 * @param PushRegistration (token, platform, deviceId)
     * @returns PushRegistrationResponse
     */
    @POST(Users.REGISTER_DEVICE_THIRD_PARTY_CONNECTION)
    Observable<Response<PushRegistrationResponse>> registerDeviceWithThirdParty(@Body PushRegistration input);

    /** 
     * Deregister a push token for a device with a 3rd party
     * @param 3rd party push token to deregister
     * @returns Void response. It response status is OK then the registration was deleted.
     * Response will be 400 if the registration does not exist or if the user does not have permission to delete it.
     */
    @DELETE(Users.DEREGISTER_DEVICE_THIRD_PARTY_CONNECTION)
    Observable<Response<Void>> deregisterDeviceWithThirdParty(@Path("token") String token);

}
