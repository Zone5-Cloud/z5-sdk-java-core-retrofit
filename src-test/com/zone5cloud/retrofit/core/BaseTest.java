package com.zone5cloud.retrofit.core;

public abstract class BaseTest {
	
	/* SET YOUR TEST EMAIL HERE */
	protected String TEST_EMAIL = "enter-your-email-here@todaysplan.com.au";
	protected String TEST_PASSWORD = "enter-your-password-here";
	protected String TEST_BIKE_UUID = null; // jean SBC staging: "7aaf952e-e213-42c3-aee7-e4231fdb1ff4", jean SBC staging: "71fea48c-a1c4-4477-b5f4-cbc313420f9c", andrew: "d584c5cb-e81f-4fbe-bc0d-667e9bcd2c4c"
	
	/* SET YOUR SERVER ENDPOINT HERE */
	protected String server = "staging.todaysplan.com.au"; // "api-sp-staging.todaysplan.com.au";
	// This is your allocated clientId and secret - these can be set to null for S-Digital environments
	protected String clientID = null; // "<your OAuth clientId issued by Zone5>";
	protected String clientSecret = null; // "<your OAuth secret issued by Zone5>";
	
	public String getBaseEndpoint() {
		if (server.startsWith("127.0.0.1") || server.endsWith("8080"))
			return String.format("http://%s", server);
		return String.format("https://%s", server);
	}

}
