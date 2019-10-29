package com.zone5ventures.retrofit.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.zone5ventures.core.users.User;

import retrofit2.Response;

public class TestUsersAPI extends BaseTestRetrofit {

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
