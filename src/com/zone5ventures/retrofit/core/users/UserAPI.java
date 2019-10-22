package com.zone5ventures.retrofit.core.users;

import com.zone5ventures.common.users.User;
import com.zone5ventures.common.users.Users;

import retrofit2.Call;
import retrofit2.http.GET;

public interface UserAPI {
	
    @GET(Users.ME)
    Call<User> me();
}
