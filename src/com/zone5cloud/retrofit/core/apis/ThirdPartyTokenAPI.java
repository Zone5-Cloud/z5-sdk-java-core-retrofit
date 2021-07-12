package com.zone5cloud.retrofit.core.apis;

import com.zone5cloud.core.enums.UserConnectionsType;
import com.zone5cloud.core.thirdpartyconnections.*;
import com.zone5cloud.core.thirdpartyconnections.connections.ConnectionInitResponse;
import com.zone5cloud.core.thirdpartyconnections.connections.ConnectionsResponse;
import com.zone5cloud.core.users.Users;

import io.reactivex.Observable;
import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.http.*;

import java.util.List;
import java.util.Map;


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

    /**
     * The name of the service that will be connected.
     * @param service The name of the service that will be connected.
     * @param emptyObject an empty body
     * @return On a successful request returns  {@link ConnectionInitResponse} containing
     * the endpoint returns an OAuth URL allowing you to log into and authorize the third-party service.
     */
    @POST(ThirdParty.INIT_CONNECTION_PAIRING)
    Observable<Response<ConnectionInitResponse>> initConnectionPairing(@Path("connectionType") UserConnectionsType connectionType, @Body Object emptyObject);

    /**
     * Confirm a connection to a third-party service, such as Garmin Connect or Strava.
     * @param connectionType
     * @param queries The query string returned by the third-party when you initiated the connection.
     * @return
     */
    @GET(ThirdParty.CONFIRM_CONNECTION_PAIRING)
    Observable<Response<ResponseBody>> confirmConnectionPairing(@Path("connectionType") UserConnectionsType connectionType, @QueryMap Map<String, String> queries);

    /**
     * Get a list and detailed summary of all user connections to services supported by Zone 5 Cloud
     * @return On a successful request, the endpoint returns a detailed list of all supported third-party services,
     * noting where the user has enabled access for their account.
     */
    @GET(ThirdParty.GET_CONNECTIONS)
    Observable<Response<List<ConnectionsResponse>>> getConnections();

    /**
     * Revoke an existing connection to a third-party service identified by connectionType.
     * @param connectionType third-party service identifier
     * @return On a successful request, the endpoint returns true with HTTP status code 200 OK.
     */
    @GET(ThirdParty.REVOKE_CONNECTION)
    Observable<Response<Boolean>> removeConnection(@Path("connectionType") UserConnectionsType connectionType);
}
