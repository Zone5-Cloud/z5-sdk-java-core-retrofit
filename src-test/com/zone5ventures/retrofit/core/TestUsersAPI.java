package com.zone5ventures.retrofit.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.zone5ventures.core.oauth.OAuthTokenAlt;
import com.zone5ventures.core.users.LoginRequest;
import com.zone5ventures.core.users.LoginResponse;
import com.zone5ventures.core.users.NewPassword;
import com.zone5ventures.core.users.RegisterUser;
import com.zone5ventures.core.users.User;
import com.zone5ventures.core.utils.GsonManager;

import retrofit2.Response;

public class TestUsersAPI extends BaseTestRetrofit {

	// This is your allocated clientId and secret - these can be set to null for S-Digital environments
	String clientId = null; 	// "<your OAuth clientId issued by Zone5>";
	String clientSecret = null; // "<your OAuth secret issued by Zone5>";
		
	/** To run this test you need a valid clientId & secret */
	@Test
	public void testRegistrationLoginDelete() throws Exception {
		
		// You should set this to an email you control ...
		String email = String.format("andrew+%d@todaysplan.com.au", System.currentTimeMillis());
		String password = "superS3cretStu55";
		String firstname = "Andrew";
		String lastname = "Hall";
		
		RegisterUser register = new RegisterUser();
		register.setEmail(email);
		register.setPassword(password);
		register.setFirstname(firstname);
		register.setLastname(lastname);
		
		// For S-Digital registrations (optional)
		//register.setParams(new HashMap<String, String>(2));
		//register.getParams().put("regoSource", "Rider Hub");
		//register.getParams().put("regoKey", "<alternate GIGYA ACCESS KEY>");
		
		// optional - set weight, thresholds, dob, gender etc
		register.setWeight(80.1d);
		
		// check that this user does not yet exist in the system
		assertFalse(userApi.isEmailRegistered(email).blockingFirst().body());
		
		User user = userApi.register(register).blockingFirst().body();
		assertNotNull(user.getId()); // our unique userId
		assertEquals(email, user.getEmail());
		
		// Note - in S-Digital, the user will need to validate their email before they can login...
		if (isSpecialized()) {
			System.out.println("Waiting for confirmation that you have verified your email address ... press Enter when done");
			System.in.read();
		}
		
		// Login and set our bearer token
		LoginRequest login = new LoginRequest(email, password, clientId, clientSecret);
		System.out.println(GsonManager.getInstance(true).toJson(login));
		
		LoginResponse r = userApi.login(login).blockingFirst().body();
		assertNotNull(r.getToken());
		setToken(r.getToken());
		
		// Try it out!
		User me = userApi.me().blockingFirst().body();
		assertEquals(me.getId(), user.getId());
		
		// check that this user is now considered registered
		assertTrue(userApi.isEmailRegistered(email).blockingFirst().body());
		assertTrue(userApi.logout().blockingFirst().body());
		setToken(null);
		assertTrue(userApi.isEmailRegistered(email).blockingFirst().body());
		
		// Oops I forgot my password - send me an email with a magic link
		assertTrue(userApi.resetPassword(email).blockingFirst().body());
		
		// Log back in
		r = userApi.login(new LoginRequest(email, password, clientId, clientSecret)).blockingFirst().body();
		assertNotNull(r.getToken());
	
		setToken(r.getToken());
		me = userApi.me().blockingFirst().body();
		assertEquals(me.getId(), user.getId());
		
		// Change my password and try it out
		assertEquals(200, userApi.changePasswordSpecialized(new NewPassword(password, "myNewPassword123!!")).blockingFirst().code());
		assertTrue(userApi.logout().blockingFirst().body());
		
		r = userApi.login(new LoginRequest(email, "myNewPassword123!!", clientId, clientSecret)).blockingFirst().body();
		assertNotNull(r.getToken());
		setToken(r.getToken());
		
		// Exercise the refresh access token
		OAuthTokenAlt alt = userApi.refreshToken().blockingFirst().body();
		assertNotNull(alt.getToken());
		assertNotNull(alt.getTokenExp());
		
		// S-Digital Needs to be deleted via GIGYA
		if (!isSpecialized()) {
			// Delete this account
			assertEquals(204, userApi.delete(me.getId()).blockingFirst().code());
			
			// We are no longer valid!
			assertEquals(401, userApi.me().blockingFirst().code());
			setToken(null);
			
			assertEquals(401, userApi.login(new LoginRequest(email, password, clientId, clientSecret)).blockingFirst().code());
		}
	}
		
	@Test
	public void testMe() {
		Response<User> rsp = userApi.me().blockingFirst();
		assertEquals(200, rsp.code());
		User user = rsp.body();
		assertNotNull(user.getId());
		assertNotNull(user.getFirstname());
		assertNotNull(user.getLastname());
		assertNotNull(user.getEmail());
	}

}
