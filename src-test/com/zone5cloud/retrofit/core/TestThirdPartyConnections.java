package com.zone5cloud.retrofit.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.zone5cloud.core.enums.UserConnectionsType;
import com.zone5cloud.core.thirdpartyconnections.PushRegistration;
import com.zone5cloud.core.thirdpartyconnections.PushRegistrationResponse;
import com.zone5cloud.core.thirdpartyconnections.ThirdPartyToken;
import com.zone5cloud.core.thirdpartyconnections.ThirdPartyTokenResponse;
import com.zone5cloud.core.thirdpartyconnections.connections.ConnectionInitResponse;
import com.zone5cloud.core.thirdpartyconnections.connections.ConnectionsResponse;
import com.zone5cloud.retrofit.core.utilities.Z5Utilities;

import retrofit2.Response;


public class TestThirdPartyConnections extends BaseTestRetrofit {
	@Before
	public void setup() {
		login();
	}
	
	@SuppressWarnings("deprecation")
	@Test
	public void testThirdPartyTokenCrudLegacy() throws Exception {
		thirdPartyApi.removeThirdPartyToken(UserConnectionsType.strava).blockingFirst().body();
		
		ThirdPartyTokenResponse rsp = thirdPartyApi.hasThirdPartyToken(UserConnectionsType.strava).blockingFirst().body();
		assertFalse(rsp.getAvailable());
		
		rsp = thirdPartyApi.setThirdPartyToken(new ThirdPartyToken("abc123", "refreshme", "notmuch", 3600), UserConnectionsType.strava).blockingFirst().body();
		assertTrue(rsp.getSuccess());
		
		rsp = thirdPartyApi.hasThirdPartyToken(UserConnectionsType.strava).blockingFirst().body();
		assertTrue(rsp.getAvailable());
		assertEquals("abc123", rsp.getToken().getToken());
		assertEquals("refreshme", rsp.getToken().getRefreshToken());
		assertEquals("notmuch", rsp.getToken().getScope());
		assertNotNull(rsp.getToken().getExpiresIn());
		
		rsp = thirdPartyApi.removeThirdPartyToken(UserConnectionsType.strava).blockingFirst().body();
		assertTrue(rsp.getSuccess());
		
		rsp = thirdPartyApi.hasThirdPartyToken(UserConnectionsType.strava).blockingFirst().body();
		assertFalse(rsp.getAvailable());
	}
	
	@Test
	public void testConnectService() throws Exception {
		List<UserConnectionsType> types = Arrays.asList(UserConnectionsType.strava, UserConnectionsType.wahoo, UserConnectionsType.garminconnect, UserConnectionsType.garmintraining);
		
		for(UserConnectionsType type: types) {
			// connect
			Response<String> rsp = thirdPartyApi.pairConnection(type, "appname://callback.test.com").blockingFirst();
			if (rsp.isSuccessful()) {
				String connectService = rsp.body();
				
				URL url = new URL(connectService);
				assertTrue(url.getHost().startsWith("connect") && (url.getHost().endsWith("zone5cloud.com") || (url.getHost().endsWith("specialized.com"))));
				assertTrue(url.getQuery().contains("redirectUrl=appname://callback.test.com"));
				assertTrue(url.getQuery().contains("type=" + type.name()));
			} else {
				String errorMsg = Z5Utilities.parseErrorResponse(rsp).getMessage();
				assertTrue("response failed: " + errorMsg, false);
			}
		}
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

    @Test
    public void testConnectionInit() {
        @SuppressWarnings("deprecation") Response<ConnectionInitResponse> rsp = thirdPartyApi.initConnectionPairing(UserConnectionsType.garminconnect, new Object()).blockingFirst();
        assertTrue(rsp.code() >= 200 && rsp.code() < 300);
        ConnectionInitResponse response = rsp.body();
        assertNotNull(response);
        assertNotNull(response.getConfirmationUrl());
    }

    @Test
    public void testGetConnections() {
        Response<List<ConnectionsResponse>> rsp = thirdPartyApi.getConnections().blockingFirst();
        assertTrue(rsp.code() >= 200 && rsp.code() < 300);
        List<ConnectionsResponse> connections = rsp.body();
        assertNotNull(connections);
        assertNotEquals(connections.size(), 0);
    }

    @Test
    public void testRevokeConnection() {
        Response<Boolean> rsp = thirdPartyApi.removeConnection(UserConnectionsType.garminconnect).blockingFirst();
        assertTrue(rsp.code() >= 200 && rsp.code() < 300);
        assertNotNull(rsp.body());
        assertTrue(rsp.body());
    }
}
