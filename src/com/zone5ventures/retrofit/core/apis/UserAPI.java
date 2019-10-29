package com.zone5ventures.retrofit.core.apis;

import com.zone5ventures.core.users.User;
import com.zone5ventures.core.users.Users;

import retrofit2.http.GET;
import rx.Observable;

public interface UserAPI {
	
    @GET(Users.ME)
    Observable<User> me();
}
