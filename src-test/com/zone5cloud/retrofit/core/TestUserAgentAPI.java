package com.zone5cloud.retrofit.core;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.zone5cloud.core.thirdpartyconnections.UpgradeAvailableResponse;

import retrofit2.Response;

public class TestUserAgentAPI extends BaseTestRetrofit {
	@Before
	public void setup() {
		login();
	}
	
	@Test
	public void testGetDeprecated() {
		Response<UpgradeAvailableResponse> rsp = agentApi.getDeprecated().blockingFirst();
		assertTrue(rsp.code() >= 200 && rsp.code() < 300);
		UpgradeAvailableResponse response = rsp.body();
		assertNotNull(response.getIsUpgradeAvailable());
		
		assertTrue(response.getIsUpgradeAvailable());
	}
}
