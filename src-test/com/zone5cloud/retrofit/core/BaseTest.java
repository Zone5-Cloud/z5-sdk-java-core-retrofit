package com.zone5cloud.retrofit.core;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.apache.commons.io.FileUtils;

import com.zone5cloud.core.ClientConfig;

import okhttp3.ResponseBody;
import okio.BufferedSink;
import okio.Okio;

public abstract class BaseTest {
	
	/* SET YOUR TEST EMAIL HERE */
	protected String TEST_EMAIL = "<enter-your-email-here@todaysplan.com.au>";
	protected String TEST_PASSWORD = "<enter-your-password-here>";
	protected String TEST_BIKE_UUID = null; // andrew SBC Staging: "d584c5cb-e81f-4fbe-bc0d-667e9bcd2c4c"
	protected boolean EMAIL_VERIFICATION = false;
	
	/* SET YOUR SERVER ENDPOINT HERE */
	private String server = "";
	// This is your allocated clientId and secret - these can be set to null for S-Digital environments
	protected final ClientConfig clientConfig = new ClientConfig();

    public BaseTest() {
    		// read config ~/tp.env or ~/z5.env
    		File f = new File(System.getProperty("user.home")+File.separatorChar+"tp.env");
    		if (!f.exists())
    			 f = new File(System.getProperty("user.home")+File.separatorChar+"z5.env");
    		
    		if (f.exists()) {
    			try {
    				for(String line : FileUtils.readLines(f, "UTF-8")) {
    					String[] arr = line.split("=");
    					if (arr.length == 2) {
    						String key = arr[0].trim();
    						String value = arr[1].trim();
    						switch(key) {
    						case "username":
    							TEST_EMAIL = value;
    							break;
    						case "password":
								TEST_PASSWORD = value;
    							break;
    						case "server":
    							server = value;
    							clientConfig.setZone5BaseUrl(new URL(getBaseEndpoint()));
    							break;
    						case "clientID":
    							clientConfig.setClientID(value);
    							break;
    						case "clientSecret":
    							clientConfig.setClientSecret(value);
    							break;
    						case "emailVerification":
    							EMAIL_VERIFICATION = Boolean.parseBoolean(value);
    							break;
    						}
    					}
    				}
    			} catch (Exception e) { }
    			
    			if (f.exists() && clientConfig.getUserName() != null || server != null)
    				System.out.println(String.format("[ Using credentials in file %s - server=%s, username=%s ]", f.getAbsolutePath(), server, clientConfig.getUserName()));
    		}
    }
    
	public String getBaseEndpoint() {
		if (server.startsWith("127.0.0.1") || server.endsWith("8080"))
			return String.format("http://%s", server);
		return String.format("https://%s", server);
	}

	protected File toFile(ResponseBody b) throws IOException {
		File f = File.createTempFile(getClass().getSimpleName(), "tmp");
		f.deleteOnExit();
		
		try (BufferedSink bufferedSink = Okio.buffer(Okio.sink(f))) {
			bufferedSink.writeAll(b.source());
		}
        
		assertTrue(f.exists() && f.length() > 0);
		return f;
	}
}
