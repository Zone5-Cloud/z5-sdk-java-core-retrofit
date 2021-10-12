package com.zone5cloud.retrofit.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.Test;

import com.zone5cloud.core.Z5Error;
import com.zone5cloud.core.enums.UnitMeasurement;
import com.zone5cloud.core.oauth.OAuthToken;
import com.zone5cloud.core.terms.TermsAndConditions;
import com.zone5cloud.core.users.LoginRequest;
import com.zone5cloud.core.users.LoginResponse;
import com.zone5cloud.core.users.NewPassword;
import com.zone5cloud.core.users.RefreshRequest;
import com.zone5cloud.core.users.RegisterUser;
import com.zone5cloud.core.users.TestPasswordRequest;
import com.zone5cloud.core.users.User;
import com.zone5cloud.core.users.UserPreferences;
import com.zone5cloud.core.utils.GsonManager;
import com.zone5cloud.retrofit.core.apis.UserAPI;
import com.zone5cloud.retrofit.core.utilities.Z5Utilities;

import io.reactivex.Observable;
import retrofit2.Response;

public class TestUsersAPI extends BaseTestRetrofit {
	@Test
	public void getEmailExists() throws Exception {
		Response<Boolean> responseExists = userApi.isEmailRegistered(TEST_EMAIL).blockingFirst();
		assertTrue(responseExists.isSuccessful());
		assertTrue(responseExists.body());
	}
	
	@Test
	public void testRegistrationLoggedIn() throws Exception {
		login();
		
		assertNotNull(clientConfig.getToken());
		testRegistrationLoginDelete();
		
	}
	
	@Test
	public void testRegistrationNotLoggedIn() throws Exception {
		assertNull(clientConfig.getToken());
		testRegistrationLoginDelete();
	}

	/** To run this test you need a valid clientId & secret */
	//@Test - being run from above with 2 different config scenarios
	public void testRegistrationLoginDelete() throws Exception {
		
		// You should set this to an email you control ...
		String emailParts[] = TEST_EMAIL.split("@");
		String email = String.format("%s%s%d@%s", emailParts[0], (emailParts[0].contains("+") ? "" : "+"), System.currentTimeMillis(), emailParts[1]);
		String password = "superS3cretStu55";
		String firstname = "Test";
		String lastname = "User";
		
		RegisterUser register = new RegisterUser();
		register.setEmail(email);
		register.setPassword(password);
		register.setFirstname(firstname);
		register.setLastname(lastname);
		
		// add all required terms
		List<TermsAndConditions> terms = termsApi.required().blockingFirst().body();
		register.setAccept(new ArrayList<>(terms.size()));
		terms.forEach(t -> register.getAccept().add(t.getTermsId()));
		
		// optional - set weight, thresholds, dob, gender etc
		register.setWeight(80.1d);
		
		// check that this user does not yet exist in the system
		assertFalse(userApi.isEmailRegistered(email).blockingFirst().body());
		
		Response<User> r = userApi.register(register).blockingFirst(); 
		if (!r.isSuccessful()) {
			assertTrue(Z5Utilities.parseErrorResponse(r).getMessage(), false);
		}
		
		User user = r.body();
		assertNotNull(user.getId()); // our unique userId
		assertEquals(Locale.getDefault().toString().toLowerCase(), user.getLocale().toLowerCase());
		assertEquals(email, user.getEmail());
		
		// Note - in S-Digital, the user will need to validate their email before they can login...
		if (requiresEmailVerification()) {
			System.out.println("Waiting for confirmation that you have verified your email address ... press Enter when done");
			System.in.read();
		}
		
		// Login and set our bearer token
		LoginRequest login = new LoginRequest(email, password);

		System.out.println(GsonManager.getInstance(true).toJson(login));
		
		Response<LoginResponse> responseAuth = userApi.login(login).blockingFirst();
		assertTrue(responseAuth.isSuccessful());
		LoginResponse loginResponse = responseAuth.body();
		assertNotNull(loginResponse.getToken());
		assertTrue(loginResponse.getTokenExp() > System.currentTimeMillis() + 30000);
		assertNotNull(clientConfig.getToken());
		
		// Try it out!
		User me = userApi.me().blockingFirst().body();
		assertEquals(me.getId(), user.getId());
		
		// check that this user is now considered registered
		assertTrue(userApi.isEmailRegistered(email).blockingFirst().body());
		assertNotNull(clientConfig.getToken());
		assertTrue(userApi.logout().blockingFirst().body());
		assertNull(clientConfig.getToken());
		
		assertTrue(userApi.isEmailRegistered(email).blockingFirst().body());
		
		// Oops I forgot my password - send me an email with a magic link
		assertTrue(userApi.resetPassword(email).blockingFirst().body());

		// Log back in
		clientConfig.setUserName(email);
		loginResponse = userApi.login(new LoginRequest(email, password, clientConfig.getClientID(), clientConfig.getClientSecret())).blockingFirst().body();
		assertNotNull(loginResponse.getToken());
		assertTrue(loginResponse.getTokenExp() > System.currentTimeMillis() + 30000);
		
		assertEquals(Locale.getDefault().toString().toLowerCase(), loginResponse.getUser().getLocale().toLowerCase());
		me = userApi.me().blockingFirst().body();
		assertEquals(me.getId(), user.getId());

		// Change my password and try it out
		assertEquals(200, userApi.setPassword(new NewPassword(password, "myNewPassword123!!")).blockingFirst().code());
		assertTrue(userApi.logout().blockingFirst().body());

		
		loginResponse = userApi.login(new LoginRequest(email, "myNewPassword123!!", clientConfig.getClientID(), clientConfig.getClientSecret())).blockingFirst().body();
		assertNotNull(loginResponse.getToken());
		assertTrue(loginResponse.getTokenExp() > System.currentTimeMillis() + 30000);
		
		// Exercise the refresh access token
		if (loginResponse.getRefresh() != null) {
			OAuthToken token = userApi.refreshToken(new RefreshRequest(email, loginResponse.getRefresh())).blockingFirst().body().getOAuthToken();
			assertNotNull(token.getToken());
			assertNotNull(token.getTokenExp());
			assertTrue(token.getTokenExp() > System.currentTimeMillis() + 30000);
		}
		
		// Delete this account
		assertEquals(204, userApi.delete(me.getId()).blockingFirst().code());
			
		// We are no longer valid!
		assertEquals(401, userApi.me().blockingFirst().code());
			
		assertEquals(401, userApi.login(new LoginRequest(email, password, clientConfig.getClientID(), clientConfig.getClientSecret())).blockingFirst().code());
	}
		
	@Test
	public void testMe() {
		login();
		
		Response<User> rsp = userApi.me().blockingFirst();
		assertEquals(200, rsp.code());
		User user = rsp.body();
		assertNotNull(user.getId());
		assertNotNull(user.getFirstname());
		assertNotNull(user.getLastname());
		assertNotNull(user.getEmail());
	}
	
	@Test
	public void testFail() {
		Response<User> rsp = userApi.me().blockingFirst();
		assertEquals(401, rsp.code());
		Z5Error error = Z5Utilities.parseErrorResponse(rsp);
		assertNotNull(error);
		assertNull(rsp.body());
	}
	
	@Test
	public void testUserPreferences() {
		login();
		
		long userId = userApi.me().blockingFirst().body().getId();
		
		UserPreferences p = userApi.getPreferences(userId).blockingFirst().body();
		assertNotNull(p.getMetric());
		
		p.setMetric(UnitMeasurement.imperial);
		assertTrue(userApi.setPreferences(p).blockingFirst().body());
		assertEquals(UnitMeasurement.imperial, userApi.getPreferences(userId).blockingFirst().body().getMetric());
		
		p.setMetric(UnitMeasurement.metric);
		assertTrue(userApi.setPreferences(p).blockingFirst().body());
		assertEquals(UnitMeasurement.metric, userApi.getPreferences(userId).blockingFirst().body().getMetric());
	}

	@Test
	public void testPasswordComplexity(){
		String passwordFormat = "^(?=.[\\d])(?=.[a-z])(?=.[A-Z])(?=.[a-zA-Z]).{8,}$";
		UserAPI userAPI = mock(UserAPI.class);
		when(userAPI.passwordComplexity()).thenReturn(Observable.just(Response.success(passwordFormat)));
		Response<String> response = userAPI.passwordComplexity().blockingFirst();
		assertEquals(response.body(),passwordFormat);
	}

	@Test
	public void testReconfirmEmail(){
		String email = "sometest@email.com";
		UserAPI userAPI = mock(UserAPI.class);
		when(userAPI.reconfirm(email)).thenReturn(Observable.just(Response.success(200,null)));
		Response<Void> response =  userAPI.reconfirm(email).blockingFirst();
		assertEquals(200,response.code());
	}

	@Test
	public void testReconfirm() {
		Response<Void> response = userApi.reconfirm(TEST_EMAIL).blockingFirst();
		assertTrue(response.isSuccessful());
	}

	@Test
	public void testPasswordComplexityApi() {
		Response<String> response = userApi.passwordComplexity().blockingFirst();
		assertNotNull(response.body());
		assertEquals("^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[a-zA-Z]).{8,}$",response.body());
	}
	
	@Test
	public void testTestPassword() {
		assertTrue(userApi.testPassword(new TestPasswordRequest(TEST_EMAIL, "pass")).blockingFirst().body().getError());
		assertFalse(userApi.testPassword(new TestPasswordRequest(TEST_EMAIL, "aksdf3r2398KJHLG#$%&")).blockingFirst().body().getError());
	}
}
