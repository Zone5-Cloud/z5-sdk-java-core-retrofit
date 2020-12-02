package com.zone5cloud.retrofit.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.zone5cloud.core.enums.UserConnectionsType;
import com.zone5cloud.core.thirdpartyconnections.PushRegistration;
import com.zone5cloud.core.thirdpartyconnections.PushRegistrationResponse;
import com.zone5cloud.core.thirdpartyconnections.ThirdPartyToken;
import com.zone5cloud.core.thirdpartyconnections.ThirdPartyTokenResponse;

import retrofit2.Response;

public class TestThirdPartyConnections extends BaseTestRetrofit {
	@Before
	public void setup() {
		login();
	}
	
	@Test
	public void testThirdPartyTokenCrud() throws Exception {
		
		ThirdPartyTokenResponse rsp = thirdPartyApi.hasThirdPartyToken(UserConnectionsType.strava).blockingFirst().body();
		assertFalse(rsp.getAvailable());
		
		rsp = thirdPartyApi.setThirdPartyToken(new ThirdPartyToken("abc123", "refreshme", "notmuch", 3600), UserConnectionsType.strava).blockingFirst().body();
		assertTrue(rsp.getSuccess());
		
		rsp = thirdPartyApi.hasThirdPartyToken(UserConnectionsType.strava).blockingFirst().body();
		assertTrue(rsp.getAvailable());
		assertEquals("abc123", rsp.getToken().getToken());
		assertEquals("refreshme", rsp.getToken().getRefresh_token());
		assertEquals("notmuch", rsp.getToken().getScope());
		assertNotNull(rsp.getToken().getExpires_in());
		
		rsp = thirdPartyApi.removeThirdPartyToken(UserConnectionsType.strava).blockingFirst().body();
		assertTrue(rsp.getSuccess());
		
		rsp = thirdPartyApi.hasThirdPartyToken(UserConnectionsType.strava).blockingFirst().body();
		assertFalse(rsp.getAvailable());
	}
	
	@Test
	public void testPushRegistrations() {
		Response<PushRegistrationResponse> rsp = thirdPartyApi.registerDeviceWithThirdParty(new PushRegistration("tokenABC", "android", "device123")).blockingFirst();
		assertTrue(rsp.code() >= 200 && rsp.code() < 300);
		PushRegistrationResponse response = rsp.body();
		assertNotNull(response.getToken());
		
		// save for comparison
		Long token = response.getToken();
		
		// verify that a re-request returns the same value (already existing, doesn't create a new one)
		rsp = thirdPartyApi.registerDeviceWithThirdParty(new PushRegistration("tokenABC", "android", "device123")).blockingFirst();
		assertTrue(rsp.code() >= 200 && rsp.code() < 300);
		response = rsp.body();
		assertNotNull(response.getToken());
		assertEquals(token, response.getToken());
		
		// delete it
		Response<Void> rsp2 = thirdPartyApi.deregisterDeviceWithThirdParty("tokenABC").blockingFirst();
		assertTrue(rsp2.code() >= 200 && rsp2.code() < 300);
		
		// verify that new request is a different value (the delete successfully deleted the old one)
		rsp = thirdPartyApi.registerDeviceWithThirdParty(new PushRegistration("tokenABC", "android", "device123")).blockingFirst();
		assertTrue(rsp.code() >= 200 && rsp.code() < 300);
		response = rsp.body();
		assertNotNull(response.getToken());
		assertNotEquals(token, response.getToken());
		
		// clean up
		rsp2 = thirdPartyApi.deregisterDeviceWithThirdParty("tokenABC").blockingFirst();
		assertTrue(rsp.code() >= 200 && rsp.code() < 300);
	}
}
