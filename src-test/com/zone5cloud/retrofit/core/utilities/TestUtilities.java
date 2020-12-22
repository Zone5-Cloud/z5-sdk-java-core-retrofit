package com.zone5cloud.retrofit.core.utilities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.zone5cloud.core.ClientConfig;
import org.junit.Test;

import com.zone5cloud.core.Z5Error;
import com.zone5cloud.core.users.LoginRequest;
import com.zone5cloud.core.users.LoginResponse;
import com.zone5cloud.retrofit.core.BaseTestRetrofit;

import retrofit2.Response;

public class TestUtilities extends BaseTestRetrofit {
	private ClientConfig clientConfig = new ClientConfig();
	@Test
	public void testErrors() throws Exception {
		auth.setClientIDAndSecret("bogus clientid", "bogus email");
		Response<LoginResponse> response = userApi.login(new LoginRequest(TEST_EMAIL,TEST_PASSWORD, clientConfig.getClientID(), clientConfig.getClientSecret())).blockingSingle();
		
		assertFalse(response.isSuccessful());
		assertEquals(401, response.code());
		
		Z5Error error = Z5Utilities.parseErrorResponse(response);
		assertNotNull(error);
		assertNotNull(error.getMessage());
		assertNotNull(error.getErrors());
		assertNotNull(error.getError());
		
		assertEquals(1, error.getErrors().size());
		assertEquals("true", error.getError());
		assertEquals("Token can not be issued - unsupported client_id", error.getMessage());
		assertEquals("clientId", error.getErrorItem(0).getField());
		assertEquals(401105, error.getErrorItem(0).getCode().intValue());
		assertEquals("INVALID_CLIENT_ID_OR_SECRET", error.getErrorItem(0).getMessage());
		
	}

}
