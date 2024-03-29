package com.zone5cloud.retrofit.core.apis;

import java.util.Map;

import com.zone5cloud.core.annotations.Unauthenticated;
import com.zone5cloud.core.users.LoginRequest;
import com.zone5cloud.core.users.LoginResponse;
import com.zone5cloud.core.users.NewPassword;
import com.zone5cloud.core.users.RefreshRequest;
import com.zone5cloud.core.users.RegisterUser;
import com.zone5cloud.core.users.TestPasswordRequest;
import com.zone5cloud.core.users.TestPasswordResponse;
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
    @Unauthenticated
    Observable<Response<LoginResponse>> login(@Body LoginRequest input);
	
    /** Logout - this will invalidate any active JSESSION and will also invalidate your bearer token */
    @GET(Users.LOGOUT)
    Observable<Response<Boolean>> logout();

    /** Test if an email address is already registered in the system - true if the email already exists in the system */
    @GET(Users.EMAIL_EXISTS)
    @Unauthenticated
    Observable<Response<Boolean>> isEmailRegistered(@Query("email") String email);
    
    /** Returns the status of email verification for the given user */
    @GET(Users.EMAIL_STATUS)
    @Unauthenticated
    Observable<Response<Map<String,Boolean>>> getEmailVerification(@Query("email") String email);
    
    /** Request a password reset email - ie get a magic link to reset a user's password */
    @GET(Users.PASSWORD_RESET)
    @Unauthenticated
    Observable<Response<Boolean>> resetPassword(@Query("email") String email);
    
    /** 
     *  Change a user's password - requires both new and old password. Ensure the new password meets complexity requirements.
     *  @deprecated - please use setPassword
     **/
    @Deprecated
    @POST(Users.SET_PASSWORD)
    Observable<Response<Void>> changePasswordSpecialized(@Body NewPassword input);
    
    /** 
     *  Change a user's password - old password may be required depending on clientId. 
     *  Ensure the new password meets complexity requirements. 
     **/
    @POST(Users.SET_PASSWORD)
    Observable<Response<Void>> setPassword(@Body NewPassword input);
    
    /** Update user */
    @POST(Users.SET_USER)
    Observable<Response<Void>> updateUser(@Body User input);
	
    /** 
     * <p>Refresh a bearer token. Input can include accept terms and billingCountry.</p> 
     * 
     * <p>Response includes updated terms if the clientID supports Terms enforcement on refresh.</p>
     * 
     * <p>note that refresh is automatically applied by the OkHttpClientInterceptor_Authorization so explicitly calling
	 * this endpoint is not necessary.</p>
	 */
    @POST(Users.REFRESH_TOKEN)
    @Unauthenticated
    Observable<Response<LoginResponse>> refreshToken(@Body RefreshRequest input);

    /**
     * Returns password complexity on a Get request.
     * This Retrofit api needs these two ConverterFactories to work correctly.
        * ScalarsConverterFactory
        * GsonConverterFactory
     * @return ^(?=.*\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[a-zA-Z]).{8,}$ as String
     */
    @GET(Users.PASSWORD_COMPLEXITY)
    @Unauthenticated
    Observable<Response<String>> passwordComplexity();

    /** Reconfirm email */
    @GET(Users.RECONFIRM)
    @Unauthenticated
    Observable<Response<Void>> reconfirm(@Query("email") String email);
    
    
    /** Test a password to see if it meets complexity requirements */
    @POST(Users.TEST_PASSWORD)
    @Unauthenticated
    Observable<Response<TestPasswordResponse>> testPassword(@Body TestPasswordRequest password);
}
