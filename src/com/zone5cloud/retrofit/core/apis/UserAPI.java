package com.zone5cloud.retrofit.core.apis;

import java.util.Map;

import com.zone5cloud.core.oauth.OAuthToken;
import com.zone5cloud.core.users.LoginRequest;
import com.zone5cloud.core.users.LoginResponse;
import com.zone5cloud.core.users.NewPassword;
import com.zone5cloud.core.users.RegisterUser;
import com.zone5cloud.core.users.User;
import com.zone5cloud.core.users.UserPreferences;
import com.zone5cloud.core.users.Users;

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
    
    /** Get user's metric/imperial preference */
    @GET(Users.GET_USER_PREFERENCES)
    Observable<Response<UserPreferences>> getPreferences(@Path("userId") long userId);
    
    /** Set user's metric/imperial preference */
    @POST(Users.SET_USER_PREFERENCES)
    Observable<Response<Boolean>> setPreferences(@Body UserPreferences input);
    
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
    
    /** Returns the status of email verification for the given user */
    @GET(Users.EMAIL_STATUS)
    Observable<Response<Map<String,Boolean>>> getEmailVerification(@Query("email") String email);
    
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
    Observable<Response<OAuthToken>> refreshToken();

    /**
     * Returns password complexity on a Get request.
     * This Retrofit api needs these two ConverterFactories to work correctly.
        * ScalarsConverterFactory
        * GsonConverterFactory
     * @return ^(?=.*\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[a-zA-Z]).{8,}$ as String
     */
    @GET(Users.PASSWORD_COMPLEXITY)
    Observable<Response<String>> passwordComplexity();

    /** Reconfirm email */
    @GET(Users.RECONFIRM)
    Observable<Response<Void>> reconfirm(@Query("email") String email);
}
