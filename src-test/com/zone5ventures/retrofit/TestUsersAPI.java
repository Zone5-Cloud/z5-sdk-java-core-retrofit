package com.zone5ventures.retrofit;

import org.junit.Test;
import static org.junit.Assert.*;

import com.zone5ventures.common.users.User;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TestUsersAPI extends BaseTestRetrofit {

	@Test
	public void testMe() {
		Call<User> call = userApi.me();
		
        call.enqueue(new Callback<User>() {
			
			@Override
			public void onResponse(Call<User> call, Response<User> response) {
				assertEquals(200, response.code());
				assertNotNull(response.body().getId());
				assertNotNull(response.body().getFirstname());
				assertNotNull(response.body().getLastname());
				assertNotNull(response.body().getEmail());
			}
			
			@Override
			public void onFailure(Call<User> call, Throwable t) {
				t.printStackTrace();
				assertTrue(false);
			}
		});
	}

}
