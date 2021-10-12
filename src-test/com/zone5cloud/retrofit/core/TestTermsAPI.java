package com.zone5cloud.retrofit.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import com.zone5cloud.core.Z5Error;
import com.zone5cloud.core.terms.TermsAndConditions;
import com.zone5cloud.core.users.LoginRequest;
import com.zone5cloud.core.users.LoginResponse;
import com.zone5cloud.core.users.RegisterUser;
import com.zone5cloud.core.users.User;
import com.zone5cloud.retrofit.core.utilities.Z5Utilities;

import retrofit2.Response;

public class TestTermsAPI extends BaseTestRetrofit {

	@Test
	public void testAccept() {
		LoginResponse response = login();
		// this will only exercise the accept terms if this user is set up with older acceptance
		if (response.getUpdatedTerms() != null) {
			response.getUpdatedTerms().forEach(t -> assertTrue(termsApi.accept(t.getTermsId()).blockingFirst().isSuccessful()));
		}
		
		response = login();
		assertNull(response.getUpdatedTerms());
	}
	
	@Test
	public void testRequiredAndDownload() {
		// will only exercise if there are required terms on your clientID
		List<TermsAndConditions> terms = termsApi.required().blockingFirst().body();
		AtomicBoolean success = new AtomicBoolean(false);
		if (!terms.isEmpty()) {
			termsApi.download(terms.get(0).getTermsId()).blockingSubscribe(
					r -> {
						// will only succeed if there is content for the terms
						// some only have a url and will fail
						success.set(r.isSuccessful());
						assertNotNull(toFile(r.body()));
					},
					e -> { 
						success.set(false);
					});
		}
		
		assertTrue(success.get());
	}

	@Test
	public void testAcceptance() throws Exception {
			
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
			
			// don't add terms acceptance
			
			Response<User> registration = userApi.register(register).blockingFirst();
			assertTrue(registration.isSuccessful());
			Long id = registration.body().getId();
			
			// Note - for some clientIDs, the user will need to validate their email before they can login...
			if (requiresEmailVerification()) {
				System.out.println("Waiting for confirmation that you have verified your email address ... press Enter when done");
				System.in.read();
			}
			
			List<TermsAndConditions> terms = termsApi.required().blockingFirst().body();
			
			// Login
			LoginRequest loginRequest = new LoginRequest(email, password, clientConfig.getClientID(), clientConfig.getClientSecret());
			Response<LoginResponse> loginResponse = userApi.login(loginRequest).blockingFirst();
			assertEquals(terms.isEmpty(), loginResponse.isSuccessful());
			
			if (!terms.isEmpty()) {
				// terms failure will only get triggered if you are testing with a clientId that has required terms
				Z5Error error = Z5Utilities.parseErrorResponse(loginResponse);
				assertEquals(403205, error.getErrorItem(0).getCode().intValue());
				assertEquals("USER_NOT_ACCEPTED_TERMS", error.getErrorItem(0).getMessage());
				assertNotNull(error.getErrorItem(0).getMetadata().getRequiredTerms());
				
				loginRequest.setAccept(error.getErrorItem(0).getMetadata().getRequiredTerms());
				loginResponse = userApi.login(loginRequest).blockingFirst();
			}
			
			assertTrue(loginResponse.isSuccessful());
			assertEquals(204, userApi.delete(id).blockingFirst().code());
	}
}
