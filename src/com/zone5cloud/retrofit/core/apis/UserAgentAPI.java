package com.zone5cloud.retrofit.core.apis;

import com.zone5cloud.core.thirdpartyconnections.UpgradeAvailableResponse;
import com.zone5cloud.core.users.Users;

import io.reactivex.Observable;
import retrofit2.Response;
import retrofit2.http.GET;

public interface UserAgentAPI {
	/**
	 * Query whether an upgrade is available for the current user agent (client app). 
	 * @returns UpgradeAvailableResponse - object containing isUpdateAvailable: Boolean
	 */
    @GET(Users.GET_DEPRECATED)
    Observable<Response<UpgradeAvailableResponse>> getDeprecated();
}
