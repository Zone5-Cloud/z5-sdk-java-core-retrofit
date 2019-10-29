package com.zone5ventures.retrofit.core;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.zone5ventures.core.users.User;

public class TestUsersAPI extends BaseTestRetrofit {

	@Test
	public void testMe() {
		User user = userApi.me().blockingFirst();
		assertNotNull(user.getId());
		assertNotNull(user.getFirstname());
		assertNotNull(user.getLastname());
		assertNotNull(user.getEmail());
	}

}
