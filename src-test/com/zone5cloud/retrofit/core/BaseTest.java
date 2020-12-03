package com.zone5cloud.retrofit.core;

public abstract class BaseTest {
	
	public static final String SBC_NO_VERIFICATION_GIGYA = "3_GoZ3q9P513xf8qjJuTkCQcLikOlWesA3lzES8cfPoGpXQfqrbONzu4pniGcNssqr";
	public static final String TP_COGNITO_KEY = "1er3227s1mia3pkqrngntl4sv6";
	public static final String TP_COGNITO_SECRET = "19re5046mf15n5m38klrmnr9sjtcia4sdv4hpn0ivoshm1tu72cp";
	public static final String TP_STAGING = "staging.todaysplan.com.au";
	public static final String SBC_STAGING = "api-sp-staging.todaysplan.com.au";
	public static final String JEANS_LOCAL = "192.168.1.17:8080";
	
	/* SET YOUR TEST EMAIL HERE */
	protected String TEST_EMAIL = "enter-your-email-here@todaysplan.com.au";
	protected String TEST_PASSWORD = "enter-your-password-here";
	protected String TEST_BIKE_UUID = null; // andrew SBC Staging: "d584c5cb-e81f-4fbe-bc0d-667e9bcd2c4c"
	
	/* SET YOUR SERVER ENDPOINT HERE */
	protected String server = TP_STAGING;
	// This is your allocated clientId and secret - these can be set to null for S-Digital environments
    protected String clientID = TP_COGNITO_KEY;//;  // "<your OAuth clientId issued by Zone5>";
    protected String clientSecret = TP_COGNITO_SECRET; // "<your OAuth secret issued by Zone5>";
	
	public String getBaseEndpoint() {
		if (server.startsWith("127.0.0.1") || server.endsWith("8080"))
			return String.format("http://%s", server);
		return String.format("https://%s", server);
	}

}
