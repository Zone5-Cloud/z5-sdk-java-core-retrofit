package com.zone5ventures.retrofit.core.apis;

import com.zone5ventures.core.oauth.OAuthTokenAlt;
import com.zone5ventures.core.users.LoginRequest;
import com.zone5ventures.core.users.LoginResponse;
import com.zone5ventures.core.users.NewPassword;
import com.zone5ventures.core.users.RegisterUser;
import com.zone5ventures.core.users.User;
import com.zone5ventures.core.users.Users;

import io.reactivex.Observable;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;


public interface UserAPI {
	
    @GET(Users.ME)
    Observable<Response<User>> me();
    
    /** Register a new user account */
    @POST(Users.REGISTER_USER)
    Observable<Response<User>> register(@Body RegisterUser input);
    
    /** Delete a user account */
    @GET(Users.DELETE_USER)
    Observable<Response<User>> delete(@Path("userId") long userId);
    
    /** Login as a user and obtain a bearer token */
    @POST(Users.LOGIN)
    Observable<Response<LoginResponse>> login(@Body LoginRequest input);
	
    /** Logout - this will invalidate any active JSESSION and will also invalidate your bearer token */
    @GET(Users.LOGOUT)
    Observable<Response<Boolean>> logout();

    /** Test if an email address is already registered in the system - true if the email already exists in the system */
    @POST(Users.EMAIL_EXISTS)
    Observable<Response<Boolean>> isEmailRegistered(@Body String email);
    
    /** Request a password reset email - ie get a magic link to reset a user's password */
    @GET(Users.PASSWORD_RESET)
    Observable<Response<Boolean>> resetPassword(@Query("email") String email);
    
    /** Change a user's password (Specialized) - requires both new and old password. Ensure the new password meets complexity requirements! */
    @POST(Users.CHANGE_PASSWORD_SPECIALIZED)
    Observable<Response<Void>> changePasswordSpecialized(@Body NewPassword input);
    
    /** Change a user's password - set the password attribute in the input */
    @POST(Users.SET_USER)
    Observable<Response<Void>> updateUser(@Body User input);
	
    /** Refresh a bearer token - get a new token if the current one is nearing expiry */
    @GET(Users.REFRESH_TOKEN)
    Observable<Response<OAuthTokenAlt>> refreshToken();
}
