package com.zone5cloud.retrofit.core.apis;

import java.util.List;
import java.util.Map;

import com.zone5cloud.core.enums.UserConnectionsType;
import com.zone5cloud.core.thirdpartyconnections.PushRegistration;
import com.zone5cloud.core.thirdpartyconnections.PushRegistrationResponse;
import com.zone5cloud.core.thirdpartyconnections.ThirdParty;
import com.zone5cloud.core.thirdpartyconnections.ThirdPartyToken;
import com.zone5cloud.core.thirdpartyconnections.ThirdPartyTokenResponse;
import com.zone5cloud.core.thirdpartyconnections.connections.ConnectionInitResponse;
import com.zone5cloud.core.thirdpartyconnections.connections.ConnectionsResponse;

import io.reactivex.Observable;
import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;


public interface ThirdPartyTokenAPI {
	
	/** Set a 3rd party OAuth token - returns a success: true|false */
	@Deprecated
    @POST(ThirdParty.SET_THIRD_PARTY_CONNECTION)
    Observable<Response<ThirdPartyTokenResponse>> setThirdPartyToken(@Body ThirdPartyToken token, @Query("service_name") UserConnectionsType type);

    /** Delete a 3rd party OAuth token - returns a success: true|false */
	@Deprecated
    @POST(ThirdParty.REM_THIRD_PARTY_CONNECTION)
    Observable<Response<ThirdPartyTokenResponse>> removeThirdPartyToken(@Query("service_name") UserConnectionsType type);
    
    /** Returns details of the current 3rd party token (if any) - returns a available: true|false, token: {...} */
	@Deprecated
    @GET(ThirdParty.HAS_THIRD_PARTY_CONNECTION)
    Observable<Response<ThirdPartyTokenResponse>> hasThirdPartyToken(@Query("service_name") UserConnectionsType type);
    
    /** 
	 * Register a push token for a device
	 * @param PushRegistration (token, platform, deviceId)
     * @returns PushRegistrationResponse
     */
    @POST(ThirdParty.REGISTER_DEVICE_PUSH_NOTIFICATION)
    Observable<Response<PushRegistrationResponse>> registerDeviceWithThirdParty(@Body PushRegistration input);

    /** 
     * Deregister a push token for a device
     * @param push token to deregister
     * @returns Void response. It response status is OK then the registration was deleted.
     * Response will be 400 if the registration does not exist or if the user does not have permission to delete it.
     */
    @DELETE(ThirdParty.DEREGISTER_DEVICE_PUSH_NOTIFICATION)
    Observable<Response<Void>> deregisterDeviceWithThirdParty(@Path("token") String token);

    /**
     * Fetch the Third Party Connect URL
     * This method is deprecated. Use pairConnection
     * @param service The name of the service that will be connected.
     * @param emptyObject an empty body
     * @return On a successful request returns  {@link ConnectionInitResponse} containing
     * the endpoint returns an OAuth URL allowing you to log into and authorize the third-party service.
     */
    @Deprecated()
    @POST(ThirdParty.INIT_CONNECTION_PAIRING)
    Observable<Response<ConnectionInitResponse>> initConnectionPairing(@Path("connectionType") UserConnectionsType connectionType, @Body Object emptyObject);

    /**
     * Confirm a connection to a third-party service, such as Garmin Connect or Strava.
     * This method is deprecated. Use pairConnection
     * @param connectionType
     * @param queries The query string returned by the third-party when you initiated the connection.
     * @return
     */
    @Deprecated()
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
     * Get the Connect Service URL. Open this URL in a browser window. It will prompt for Third Party Authentication and return
     * to the passed in callback redirect
     */
    @GET(ThirdParty.PAIR_CONNECTION)
    Observable<Response<String>> pairConnection(@Path("connectionType") UserConnectionsType connectionType, @Query("redirect-uri") String callbackRedirect);
    
    /**
     * Revoke an existing connection to a third-party service identified by connectionType.
     * @param connectionType third-party service identifier
     * @return On a successful request, the endpoint returns true with HTTP status code 200 OK.
     */
    @GET(ThirdParty.REVOKE_CONNECTION)
    Observable<Response<Boolean>> removeConnection(@Path("connectionType") UserConnectionsType connectionType);
}
